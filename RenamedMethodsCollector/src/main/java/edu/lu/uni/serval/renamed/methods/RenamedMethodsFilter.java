package edu.lu.uni.serval.renamed.methods;

import edu.lu.uni.serval.utils.FileHelper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Parse method names: valid-method-ids-cluster.
 * Filter out correcting method naming typos.
 * @author kui.liu
 */
public class RenamedMethodsFilter {

	public static void filteroutTyposByParsedMethodNames(String dataPath) throws IOException {
		String parsedOldMethodNamesFile = dataPath + "/OldMethodNames.txt";
		List<String> parsedOldMethodNames = Files.readAllLines(Path.of(parsedOldMethodNamesFile));
		String parsedNewMethodNamesFile = dataPath + "/NewMethodNames.txt";
		List<String> parsedNewMethodNames = Files.readAllLines(Path.of(parsedNewMethodNamesFile));
		StringBuilder typos = new StringBuilder("index@ParsedOldName@ParsedNewName\n");
		StringBuilder renames = new StringBuilder("index@oldToken@newToken\n");
		StringBuilder oldNames = new StringBuilder();
		StringBuilder newNames = new StringBuilder();
		List<Integer> renameIndexes = new ArrayList<>();
		for (int i = 0, size = parsedOldMethodNames.size(); i < size; i ++) {
			String oldMethodName = parsedOldMethodNames.get(i);
			oldMethodName = oldMethodName.substring(oldMethodName.indexOf("@") + 1);
			List<String> oldMethodNameTokens = Arrays.asList(oldMethodName.split(","));
			String newMethodName = parsedNewMethodNames.get(i);
			newMethodName = newMethodName.substring(newMethodName.indexOf("@") + 1);
			List<String> newMethodNameTokens = Arrays.asList(newMethodName.split(","));
			
			if (oldMethodNameTokens.getFirst().equals(newMethodNameTokens.getFirst())) {// typos: starts with the same token.
				typos.append(i).append("@").append(oldMethodName).append("@").append(newMethodName).append("\n");
			} else {
				if (oldMethodName.equals("main") || newMethodName.equals("main")) { 
//						|| oldMethodName.startsWith("main,") || newMethodName.startsWith("main,")) {
					typos.append(i).append("@").append(oldMethodName).append("@").append(newMethodName).append("\n");
					continue;
				}
				String firstOldChar = oldMethodName.substring(0, 1);
				String firstNewChar = newMethodName.substring(0, 1);
				if (!firstOldChar.toLowerCase().equals(firstOldChar) || !firstNewChar.toLowerCase().equals(firstNewChar)) {
					// Constructors. Maybe.
					typos.append(i).append("@").append(oldMethodName).append("@").append(newMethodName).append("\n");
					continue;
				}
				String oldToken = oldMethodNameTokens.getFirst();
				String newToken = newMethodNameTokens.getFirst();
				if (!oldToken.equalsIgnoreCase(newToken) && !newToken.startsWith(oldToken) && !oldToken.startsWith(newToken)) {
					renameIndexes.add(i);
					renames.append(i).append("@").append(oldToken).append("@").append(newToken).append("\n");
					oldNames.append(oldMethodName).append("\n");
					newNames.append(newMethodName).append("\n");
				} else {
					typos.append(i).append("@").append(oldMethodName).append("@").append(newMethodName).append("\n");
				}
			}
		}
		FileHelper.outputToFile(dataPath + "/Typo/MethodNamePairs.txt", typos, false);
		FileHelper.outputToFile(dataPath + "/ActualRenamed/MethodNamesInfo.txt", renames, false);
		FileHelper.outputToFile(dataPath + "/ActualRenamed/ParsedOldNames.txt", oldNames, false);
		FileHelper.outputToFile(dataPath + "/ActualRenamed/ParsedNewNames.txt", newNames, false);

		// Output tokens of method bodies.
		StringBuilder renamedMethodsBuilder = new StringBuilder();
		StringBuilder renameMethodTokensSizeBuilder = new StringBuilder();
		StringBuilder typoMethodsBuilder = new StringBuilder();
		StringBuilder typoMethodTokensSizeBuilder = new StringBuilder();
		String methodsFile = dataPath + "/RenamedMethods.txt";
		FileInputStream fis = null;
		Scanner scanner = null;
		try {
			fis = new FileInputStream(methodsFile);
			scanner = new Scanner(fis);
			int index = -1;
			
			while (scanner.hasNextLine()) {
				index ++;
				String line = scanner.nextLine();
				String[] elements = line.split(":");
				String[] tokens = elements[10].split(" ");
				if (elements.length != 11) {
					System.err.println(line);
				}
				
				if (renameIndexes.contains(index)) {
					renamedMethodsBuilder.append(line).append("\n");
					renameMethodTokensSizeBuilder.append(tokens.length).append("\n");
				} else {
					typoMethodsBuilder.append(line).append("\n");
					typoMethodTokensSizeBuilder.append(tokens.length).append("\n");
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (scanner != null) {
				scanner.close();
      }
			if (fis != null) {
				try {
					fis.close();
        } catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		FileHelper.outputToFile(dataPath + "/ActualRenamed/MethodTokens.txt", renamedMethodsBuilder, false);
		renamedMethodsBuilder.setLength(0);
		FileHelper.outputToFile(dataPath + "/ActualRenamed/MethodTokensSizes.csv", renameMethodTokensSizeBuilder, false);
		FileHelper.outputToFile(dataPath + "/Typo/MethodTokens.txt", typoMethodsBuilder, false);
		renamedMethodsBuilder.setLength(0);
		FileHelper.outputToFile(dataPath + "/Typo/MethodTokensSizes.csv", typoMethodTokensSizeBuilder, false);
		
		
		// Output method bodies.
		StringBuilder renamedMethodBodies = new StringBuilder();
		StringBuilder typoMethodBodies = new StringBuilder();
		try {
			fis = new FileInputStream(dataPath + "/MethodBodies.txt");
			scanner = new Scanner(fis);
			int index = -1;
			StringBuilder singleMethod = new StringBuilder();
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if ("#METHOD_BODY#========================".equals(line)) {
					if (singleMethod.length() > 0) {
						if (renameIndexes.contains(index)) {
							renamedMethodBodies.append(singleMethod);
						} else {
							typoMethodBodies.append(singleMethod);
						}
						singleMethod.setLength(0);
					}
					index ++;
				}
				singleMethod.append(line).append("\n");
			}
			scanner.close();
			fis.close();
			
			if (singleMethod.length() > 0) {
				if (renameIndexes.contains(index)) {
					renamedMethodBodies.append(singleMethod);
				} else {
					typoMethodBodies.append(singleMethod);
				}
				singleMethod.setLength(0);
			}

			FileHelper.outputToFile(dataPath + "/ActualRenamed/MethodBodies.txt", renamedMethodBodies, false);
			FileHelper.outputToFile(dataPath + "/Typo/MethodBodies.txt", typoMethodBodies, false);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
