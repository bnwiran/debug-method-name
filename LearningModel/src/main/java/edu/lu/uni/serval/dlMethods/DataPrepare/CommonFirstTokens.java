package edu.lu.uni.serval.dlMethods.DataPrepare;

import edu.lu.uni.serval.utils.FileHelper;
import edu.lu.uni.serval.utils.ReturnType;
import edu.lu.uni.serval.utils.ReturnType.ReturnTypeClassification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CommonFirstTokens {
	
	private final String inputPath;
	private final String outputPath;
	
	/**
	 * <FirstToken, Number>: of all methods.
	 */
	private final Map<String, Integer> allFirstTokensDistribution = new HashMap<>();
	/**
	 * <ReturnType, <FirstToken, Number>>: of all methods.
	 */
	private final Map<String, Map<String, Integer>> returnTypes = new HashMap<>();

	public CommonFirstTokens(String inputPath, String outputPath) {
		this.inputPath = inputPath;
		this.outputPath = outputPath;
	}

	public List<String> processMethodNameTokens() throws IOException {
		List<String> tokenVectorOfAllParsedMethodNames = readTokens();
		writeFirstTokens();
		writeReturnTypes();

		return tokenVectorOfAllParsedMethodNames;
	}

	private List<String> readTokens() throws IOException {
		Path parsedMethodNamesPath = Paths.get(inputPath, "ParsedMethodNames.txt");
		List<String> parsedMethodNames = Files.readAllLines(parsedMethodNamesPath);
		List<String> result = new ArrayList<>();

		StringBuilder builder = new StringBuilder();
		for(final String line : parsedMethodNames) {
			final int sharpSymbolIndex = line.indexOf("#");
			final String methodNameTokens = line.substring(sharpSymbolIndex + 1);
			final int commaIndex = methodNameTokens.indexOf(",");

			final String firstToken;
			if (commaIndex > 0) {
				firstToken = methodNameTokens.substring(0, commaIndex);
			} else {
				firstToken = methodNameTokens;
			}

      allFirstTokensDistribution.merge(firstToken, 1, Integer::sum);

			final String returnType = getReturnType(line, firstToken);
			final String returnAtMethodNameTokens = returnType + "@" + methodNameTokens;
			result.add(returnAtMethodNameTokens);
			builder.append(returnAtMethodNameTokens).append("\n");
		}
		
		FileHelper.outputToFile(Paths.get(outputPath, "ParsedMethodNames.txt"), builder.toString(), false);
		builder.setLength(0);

		return result;
	}

	private String getReturnType(String line, String firstToken) {
		final int sharpSymbolIndex = line.indexOf("#");
		final String methodInfo = line.substring(0, sharpSymbolIndex);
		final String returnTypeStm = methodInfo.substring(methodInfo.lastIndexOf(":") + 1);
		final String returnType = new ReturnType().readReturnType(returnTypeStm, ReturnTypeClassification.ABSTRACT);
		final Map<String, Integer> returnTypeTokens = returnTypes.get(returnType);

		if (Objects.isNull(returnTypeTokens)) {
			Map<String, Integer> tokensCount = new HashMap<>();
			tokensCount.put(firstToken, 1);
			returnTypes.put(returnType, tokensCount);
		} else {
			returnTypeTokens.merge(firstToken, 1, Integer::sum);
		}
		return returnType;
	}

	/**
	 * Export the distribution of first tokens to a file, and select the common first tokens by the threshold of QUANTITY.
	 */
	private void writeFirstTokens() {
		StringBuilder builder = new StringBuilder("Token,Number\n");
		for (Map.Entry<String, Integer> entry : allFirstTokensDistribution.entrySet()) {
			builder
				.append(entry.getKey())
				.append(",")
				.append(entry.getValue())
				.append("\n");
		}

		FileHelper.outputToFile(Paths.get(outputPath, "FirstTokens.csv"), builder.toString(), false);
	}

	private void writeReturnTypes() {
		for (Map.Entry<String, Map<String, Integer>> entry : returnTypes.entrySet()) {
			StringBuilder builder = new StringBuilder("Token,Number\n");
			Map<String, Integer> firstTokens = entry.getValue();

			for (Map.Entry<String, Integer> subEntry : firstTokens.entrySet()) {
				builder
					.append(subEntry.getKey())
					.append(",")
					.append(subEntry.getValue())
					.append("\n");
			}

			String returnType = entry.getKey();
			FileHelper.outputToFile(Paths.get(outputPath, "ReturnTypes", returnType + ".csv"), builder.toString(), false);
		}
	}
}
