package edu.lu.uni.serval.renamed.methods;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.ITree;
import edu.lu.uni.serval.diffentry.DiffEntryHunk;
import edu.lu.uni.serval.diffentry.DiffEntryReader;
import edu.lu.uni.serval.gumtree.GumTreeComparer;
import edu.lu.uni.serval.gumtree.regroup.HierarchicalActionSet;
import edu.lu.uni.serval.gumtree.regroup.HierarchicalRegrouper;
import edu.lu.uni.serval.gumtree.regroup.SimpleTree;
import edu.lu.uni.serval.gumtree.regroup.SimplifyTree;
import edu.lu.uni.serval.utils.CUCreator;
import edu.lu.uni.serval.utils.Checker;
import edu.lu.uni.serval.utils.FileHelper;
import edu.lu.uni.serval.utils.MapSorter;
import edu.lu.uni.serval.utils.Tokenizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.jdt.core.dom.CompilationUnit;
import scala.concurrent.java8.FuturesConvertersImpl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class CodeChangeParser {
	
	private final List<String> renamedMethods = new ArrayList<>();
	private final StringBuilder methodBodies = new StringBuilder();

	public void parse(File revFile, File prevFile, File diffentryFile) {
		List<Action> gumTreeResults = new GumTreeComparer().compareTwoFilesWithGumTree(prevFile, revFile);
		if (Objects.nonNull(gumTreeResults) && !gumTreeResults.isEmpty()) {
			List<HierarchicalActionSet> allActionSets = new HierarchicalRegrouper().regroupGumTreeResults(gumTreeResults);
			
			Map<HierarchicalActionSet, HierarchicalActionSet> renamedMethodsActionSets = new HashMap<>();
			Map<Integer, HierarchicalActionSet> delActionSets = new HashMap<>();
			Map<Integer, HierarchicalActionSet> insActionSets = new HashMap<>();
			for (HierarchicalActionSet actionSet : allActionSets) {
				String action = actionSet.getActionString();
				if (action.startsWith("UPD MethodDeclaration")) {
					if (action.startsWith("@@=CONSTRUCTOR=, MethodName:")) {
						continue;
					}
					List<HierarchicalActionSet> children = actionSet.getSubActions();
					for (HierarchicalActionSet child : children) {
						if (child.getActionString().startsWith("UPD SimpleName@@MethodName:")) {
							renamedMethodsActionSets.put(actionSet, child);
							break;
						}
					}
				} else if (action.contains("Statement@@")) {
					if (action.startsWith("UPD ") || action.startsWith("DEL ")) {
						delActionSets.put(actionSet.getStartPosition(), actionSet);
					} else if (action.startsWith("INS ")) {
						insActionSets.put(actionSet.getStartPosition(), actionSet);
					}
				}
			}
			
			if (!renamedMethodsActionSets.isEmpty()) {
				MapSorter<Integer, HierarchicalActionSet> mapSorter = new MapSorter<>();
				delActionSets = mapSorter.sortByKeyAscending(delActionSets);
				insActionSets = mapSorter.sortByKeyAscending(insActionSets);
				
				List<DiffEntryHunk> diffentryHunks = new DiffEntryReader().readHunks2(diffentryFile);
				CUCreator cuCreator = new CUCreator();
				CompilationUnit prevUnit = cuCreator.createCompilationUnit(prevFile);
				CompilationUnit revUnit = cuCreator.createCompilationUnit(revFile);
				String filePath;
				try {
					filePath = revUnit.getPackage().getName().toString();
					String fileName = revFile.getName();
					filePath += "." + fileName.substring(fileName.lastIndexOf("#") + 1);
				} catch (Exception e) {
					filePath = revFile.getName().substring(14).replaceAll("#", ".");
				}
				
				for (Map.Entry<HierarchicalActionSet, HierarchicalActionSet> entry : renamedMethodsActionSets.entrySet()) {
					HierarchicalActionSet actionSet = entry.getKey();
					if (!methodBodyChanged(actionSet, delActionSets, insActionSets)) {
						HierarchicalActionSet renamedMethod = entry.getValue();
						int bugStartPosition = renamedMethod.getStartPosition();
						int bugEndPosition = bugStartPosition + renamedMethod.getLength();
						Update update = (Update) renamedMethod.getAction();
						ITree newNode = update.getNewNode();
						int fixStartPosition = newNode.getPos();
						int fixEndPosition = fixStartPosition + newNode.getLength();
						
						int bugStartLine = prevUnit.getLineNumber(bugStartPosition);
						int bugEndLine = prevUnit.getLineNumber(bugEndPosition);
						int fixStartLine = revUnit.getLineNumber(fixStartPosition);
						int fixEndLine = revUnit.getLineNumber(fixEndPosition);
						
						if (hasDiffEntry(bugStartLine, bugEndLine, fixStartLine, fixEndLine, diffentryHunks)) {
							String oldMethodName = renamedMethod.getNode().getLabel();
							oldMethodName = oldMethodName.substring(oldMethodName.indexOf(":") + 1);
							String newMethodName = newNode.getLabel();
							newMethodName = newMethodName.substring(newMethodName.indexOf(":") + 1);
							// new_commit_id, file_path, old_method_name, new_method_name, old_line, new_line.
							String fileName = revFile.getName();
							String newCommitId = fileName.substring(0, 6);
							
							String parsedOldMethodName = parseMethodName(oldMethodName);
							String parsedNewMethodName = parseMethodName(newMethodName);
							if (parsedNewMethodName == null) continue;
							// Get the return type, arguments and  tokens of method bodies.
							String tokens = readTokensOfMethodBody(actionSet, revUnit, revFile);
							
							if (Objects.nonNull(tokens)) {
								renamedMethods.add(newCommitId + ":" +filePath + ":" + oldMethodName + "@" + parsedOldMethodName + ":" + newMethodName + "@" + parsedNewMethodName + ":" + bugStartLine + ":" + fixStartLine + ":" + tokens);
							}
						}
					}
				}
			} else {
				revFile.delete();
				prevFile.delete();
				diffentryFile.delete();
			}
		} else {
			revFile.delete();
			prevFile.delete();
			diffentryFile.delete();
		}
	}
	
	private String parseMethodName(String methodName) {
		String[] subTokensArray = StringUtils.splitByCharacterTypeCamelCase(methodName);
		String methodTokens = Arrays.stream(subTokensArray)
			.filter(token -> !"_".equals(token) && !NumberUtils.isDigits(token))
			.collect(Collectors.joining(","));

		return methodTokens.isEmpty() ? null : methodTokens;
	}

	private String readTokensOfMethodBody(HierarchicalActionSet actionSet, CompilationUnit cu, File sourceCodeFile) {
		ITree methodTree = actionSet.getAction().getNode();
		String tokensOfOldMethod = readTokensOfMethodBody(methodTree);
		ITree newMethodTree = ((Update)actionSet.getAction()).getNewNode();
		String tokensOfNewMethod = readTokensOfMethodBody(newMethodTree);
		
		if (Objects.isNull(tokensOfOldMethod) || Objects.isNull(tokensOfNewMethod)) {
			return null;
		}

		if (tokensOfOldMethod.equals(tokensOfNewMethod)) {
			methodBodies.append(readMethodBody(sourceCodeFile, cu.getLineNumber(newMethodTree.getPos()), cu.getLineNumber(newMethodTree.getPos() + newMethodTree.getLength())));
			return tokensOfOldMethod;
		}
		return null;
	}
	
	private StringBuilder readMethodBody(File sourceCodeFile, int startLine, int endLine) {
		StringBuilder method = new StringBuilder("#METHOD_BODY#========================\n\n");

		try {
			List<String> codeLines = Files.readAllLines(sourceCodeFile.toPath()).subList(startLine-1, endLine);
			for (String line : codeLines) {
				method.append(line).append("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		method.append("\n\n");
		return method;
	}

	private String readTokensOfMethodBody(ITree methodTree) {
		String methodNameInfo = methodTree.getLabel();
		int indexOfMethodName = methodNameInfo.indexOf("MethodName:");
		
		String returnType = methodNameInfo.substring(0, indexOfMethodName - 2);
		int indexOfReturnType = returnType.lastIndexOf("@@");
		returnType = returnType.substring(indexOfReturnType + 2);
		
		String arguments;
		if (methodNameInfo.endsWith("@@Argus:null")) {
			arguments = "null";
		} else {
			arguments = methodNameInfo.substring(methodNameInfo.indexOf("@@Argus:") + 8, methodNameInfo.length() - 1);
		}
		
		List<ITree> children = methodTree.getChildren();
		SimpleTree simpleTree = new SimpleTree();
		simpleTree.setNodeType("Block");
		simpleTree.setLabel("Block");
		List<SimpleTree> childrenOfSimpleTree = new ArrayList<>();
		
		boolean isStmt = false;
    for (ITree child : children) {
			if (Checker.isStatement(child.getType())) {
				isStmt = true;
			}

			if (isStmt) {
				SimpleTree stmt = new SimplifyTree().canonicalizeSourceCodeTree(child, simpleTree);
				childrenOfSimpleTree.add(stmt);
			}
    }
		
		if (childrenOfSimpleTree.isEmpty()) {
			return null;
		}
		simpleTree.setChildren(childrenOfSimpleTree);
    return returnType + ":" + arguments + ":" + Tokenizer.getTokensDeepFirst(simpleTree).trim();
	}

	/**
	 * Delete statements or insert new statements.
	 * @param actionSet
	 * @param delActionSets
	 * @param insActionSets
	 * @return
	 */
	private boolean methodBodyChanged(HierarchicalActionSet actionSet,
			Map<Integer, HierarchicalActionSet> delActionSets, Map<Integer, HierarchicalActionSet> insActionSets) {
		int startPosition = actionSet.getStartPosition();
		int endPosition = startPosition + actionSet.getLength();
		if (hasActionSet(startPosition, endPosition, delActionSets)) {
			return true;
		}
		
		Update update = (Update) actionSet.getAction();
		ITree newNode = update.getNewNode();
		int fixStartPosition = newNode.getPos();
		int fixEndPosition = fixStartPosition + newNode.getLength();

    return hasActionSet(fixStartPosition, fixEndPosition, insActionSets);
  }

	private boolean hasActionSet(int startPosition, int endPosition,
			Map<Integer, HierarchicalActionSet> actionSets) {
		for (Map.Entry<Integer, HierarchicalActionSet> entry : actionSets.entrySet()) {
			int start = entry.getKey();
			if (start > endPosition) {
				break;
			}

			int end = start + entry.getValue().getLength();
			if (startPosition <= start && end <= endPosition) {
				return true;
			}
		}
		return false;
	}

	private boolean hasDiffEntry(int bugSL, int bugEL, int fixSL, int fixEL, List<DiffEntryHunk> diffentryHunks) {
		/*
		 * 1. To reduce the limitation of GumTree, We need to identify whether the old method name and the new method name are in the same DiffEntry or not.
		 * 2. The method body has no any changes.
		 */
		if (bugSL > 0) {
      for (DiffEntryHunk hunk : diffentryHunks) {
        int bugStartLine = hunk.getBugLineStartNum();
        int bugEndLine = bugStartLine + hunk.getBugRange() - 1;
        if (bugSL > bugEndLine) continue;
        if (bugEL < bugStartLine) break;

        int fixStartLine = hunk.getFixLineStartNum();
        int fixEndLine = fixStartLine + hunk.getFixRange() - 1;
        if (bugStartLine <= bugSL && bugEL <= bugEndLine
          && fixStartLine <= fixSL && fixEL <= fixEndLine) {
          return true;
        }
      }
		}
		
		return false;
	}
	
	public List<String> getRenamedMethods() {
		return renamedMethods;
	}

	public StringBuilder getMethodBodies() {
		return methodBodies;
	}
	
}
