package edu.lu.uni.serval.dlMethods.DataPrepare;

import edu.lu.uni.serval.utils.FileHelper;
import edu.lu.uni.serval.utils.ReturnType;
import edu.lu.uni.serval.utils.ReturnType.ReturnTypeClassification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommonFirstTokens {
	
	private String inputPath;
	private String outputPath;
	public int QUANTITY = 1000;// or 500, the number of methods of which names start with the same token.
	
	/**
	 * <FirstToken, Number>: of all methods.
	 */
	Map<String, Integer> allFirstTokensDistribution = new HashMap<>();
	/**
	 * <ReturnType, <FirstToken, Number>>: of all methods.
	 */
	Map<String, Map<String, Integer>> returnTypes = new HashMap<>();
	// First token list of all methods.
	public List<String> allFirstTokensList = new ArrayList<>();
	// Tokens of all parsed method names.
	public List<String> tokenVectorOfAllParsedMethodNames = new ArrayList<>();
	// Common first tokens of all methods.
	public List<String> commonFirstTokens = new ArrayList<>();

	public CommonFirstTokens(String inputPath, String outputPath) {
		this.inputPath = inputPath;
		this.outputPath = outputPath;
	}

	/**
	 * Read the distribution of first tokens.
	 * @throws IOException
	 */
	public void readTokens() throws IOException {
		Path parsedMethodNamesFile = Paths.get(inputPath, "ParsedMethodNames.txt");
		String content = FileHelper.readFile(parsedMethodNamesFile.toFile());
		BufferedReader reader = new BufferedReader(new StringReader(content));
		String line;
		StringBuilder builder = new StringBuilder();
		while ((line = reader.readLine()) != null) {
			//hibernate-metamodelgen:org.hibernate.jpamodelgen.util:StringUtil:determineFullyQualifiedClassName:String+defaultPackage+String+name:String#determine:VB,Fully:NNP,Qualified:NNP,Class:NNP,Name:VB
			int sharpSymbleIndex = line.indexOf("#");
			String methodInfo = line.substring(0, sharpSymbleIndex);
			String returnType = methodInfo.substring(methodInfo.lastIndexOf(":") + 1);
			returnType = new ReturnType().readReturnType(returnType, ReturnTypeClassification.ABSTRACT);
			String methodNameTokens = line.substring(sharpSymbleIndex + 1);
			int indexOfComma = methodNameTokens.indexOf(",");
			String firstToken;
			if (indexOfComma > 0) {
				firstToken = methodNameTokens.substring(0, indexOfComma);
			} else firstToken = methodNameTokens;

      allFirstTokensDistribution.merge(firstToken, 1, Integer::sum);
			
			Map<String, Integer> returnTypeTokens = returnTypes.get(returnType);
			if (returnTypeTokens == null) {
				returnTypeTokens = new HashMap<>();
				returnTypeTokens.put(firstToken, 1);
				returnTypes.put(returnType, returnTypeTokens);
			} else {
        returnTypeTokens.merge(firstToken, 1, Integer::sum);
			}
//			methodNameTokens = methodNameTokens.replace(":", ",");
			
			this.allFirstTokensList.add(firstToken);
			methodNameTokens = returnType + "@" + methodNameTokens;
			this.tokenVectorOfAllParsedMethodNames.add(methodNameTokens);
			builder.append(methodNameTokens).append("\n");
		}
		reader.close();
		
		FileHelper.outputToFile(Paths.get(outputPath, "ParsedMethodNames.txt"), builder.toString(), false);
		builder.setLength(0);
	}

	/**
	 * Export the distribution of first tokens to a file, and select the common first tokens by the threshold of QUANTITY.
	 */
	public void outputTokens() {
		StringBuilder builder = new StringBuilder("Token,Number\n");
		for (Map.Entry<String, Integer> entry : this.allFirstTokensDistribution.entrySet()) {
			builder.append(entry.getKey()).append(",").append(entry.getValue()).append("\n");
			if (entry.getValue() >= QUANTITY) {
				this.commonFirstTokens.add(entry.getKey());
			}
		}
		FileHelper.outputToFile(Paths.get(outputPath, "FirstTokens.csv"), builder.toString(), false);
		builder.setLength(0);
		
		for (Map.Entry<String, Map<String, Integer>> entry : this.returnTypes.entrySet()) {
			String returnType = entry.getKey();
			Map<String, Integer> firstTokens = entry.getValue();
			builder.append("Token,Number\n");
			for (Map.Entry<String, Integer> subEntry : firstTokens.entrySet()) {
				builder.append(subEntry.getKey()).append(",").append(subEntry.getValue()).append("\n");
			}
			FileHelper.outputToFile(Paths.get(outputPath, "ReturnTypes", returnType + ".csv"), builder.toString(), false);
			builder.setLength(0);
		}
	}
}
