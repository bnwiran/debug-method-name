package edu.lu.uni.serval.method.parser.util;

import java.nio.file.Path;
import java.util.List;

import edu.lu.uni.serval.jdt.method.Method;
import edu.lu.uni.serval.method.parser.MethodNameParser;
import edu.lu.uni.serval.utils.FileHelper;

public class MethodExporter {

	private final StringBuilder originalTokens = new StringBuilder();
	private final StringBuilder sizes = new StringBuilder();
	private final StringBuilder methodBodies = new StringBuilder();
	private final StringBuilder parsedMethodNames = new StringBuilder();
	private final String outputFilePath;

	public MethodExporter(String outputFilePath) {
		this.outputFilePath = outputFilePath;
	}

	public int outputMethods(List<Method> methods, int id) {
		// four folders: tokens, sizes, bodies, parsed method names.
		Path featuresFile = Path.of(outputFilePath, "tokens", "tokens_" + id + ".txt");
		Path sizesFile = Path.of(outputFilePath, "sizes", "sizes_" + id + ".csv");
		Path methodBodiesFile = Path.of(outputFilePath, "method_bodies", "method_bodies_" + id + ".txt");
		Path methodNamesFile = Path.of(outputFilePath, "ParsedMethodNames", "ParsedMethodNames_" + id + ".txt");

    return outputMethods(methods, featuresFile, sizesFile, methodBodiesFile, methodNamesFile);
	}

	private int outputMethods(List<Method> methods, Path featuresFile, Path sizesFile, Path methodBodiesFile, Path methodNamesFile) {
		int counter = 0;
		for (Method method : methods) {
			boolean isSuccessful = readMethodInfo(method);
			if (isSuccessful) {
				counter ++;
				if (counter % 1000 == 0) {
					outputData(featuresFile, sizesFile, methodNamesFile, methodBodiesFile);
				}
			}
		}
		
		if (counter % 1000 != 0)  {
			outputData(featuresFile, sizesFile, methodNamesFile, methodBodiesFile);
		}
		return 0;
	}
	
	private boolean readMethodInfo(Method method) {
		if (!method.getBody().trim().isEmpty()) { // filter out the empty method bodies.
			String bodyCodeTokens = method.getBodyCodeTokens();
			String[] tokens = bodyCodeTokens.split(" ");
			int length = tokens.length;
			if (length > 0) {
//				tokens = filterOnlyMethodInvocationMethods(tokens); // FIXME
				String methodKey = method.getKey();
				String methodName = method.getName();
				// Parse method name into sub-tokens.
				String parsedMethodNameSubTokens = new MethodNameParser().parseMethodName(methodName);
				if (parsedMethodNameSubTokens == null) return false;
				
				originalTokens.append(methodKey).append("#").append(bodyCodeTokens).append("\n");
				sizes.append(length).append("\n");
				parsedMethodNames.append(methodKey).append("#").append(parsedMethodNameSubTokens).append("\n");
				methodBodies.append("#METHOD_BODY#========================\n").append(methodKey).append("\n").append(method.getBody()).append("\n");
				return true;
			}
		}
		return false;
	}
	
	private void outputData(Path featuresFile, Path sizesFile, Path methodNames, Path methodBodiesFile) {
		FileHelper.outputToFile(featuresFile, originalTokens.toString(), true);
		FileHelper.outputToFile(sizesFile, sizes.toString(), true);
		FileHelper.outputToFile(methodNames, parsedMethodNames.toString(), true);
		FileHelper.outputToFile(methodBodiesFile, methodBodies.toString(), true);

		parsedMethodNames.setLength(0);
		methodBodies.setLength(0);
		originalTokens.setLength(0);
		sizes.setLength(0);
	}

}
