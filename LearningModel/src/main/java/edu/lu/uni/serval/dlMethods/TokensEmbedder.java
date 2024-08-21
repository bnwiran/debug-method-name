package edu.lu.uni.serval.dlMethods;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import edu.lu.uni.Configuration;
import edu.lu.uni.serval.deeplearner.Word2VecEncoder;
import edu.lu.uni.serval.utils.FileHelper;

/**
 * Embed tokens with Word2Vec, and vectorized data with embedded tokens.
 * 
 * @author kui.liu
 *
 */
public class TokensEmbedder {
	private final String inputPath;
	private final String outputPath;

	public TokensEmbedder(String inputPath, String outputPath) {
		this.inputPath = inputPath;
		this.outputPath = outputPath;
	}
	/**
	 * Merge training data and testing data.
	 */
	public void mergeData() {
		Path methodTokens = Paths.get(inputPath, "SelectedData", "SelectedMethodTokens.txt");
		Path embeddingInputData = Paths.get(inputPath, "embedding", "inputData.txt");

		FileHelper.outputToFile(embeddingInputData, FileHelper.readFile(methodTokens), false);
	}
	
	/**
	 * Embed tokens with Word2Vec.
	 */
	public void embedTokens() {
		Path embeddingInputData = Paths.get(inputPath, "embedding", "inputData.txt");
		Path embeddedTokensFile = Paths.get(inputPath, "embedding", "embeddedTokens.txt");
		Word2VecEncoder encoder = new Word2VecEncoder();

		encoder.setWindowSize(4);
		try {
			int minWordFrequency = 1;
			int layerSize = 300;
			
			encoder.embedTokens(embeddingInputData, minWordFrequency, layerSize, embeddedTokensFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void vectorizedData(boolean benchMark) throws IOException {
		Path embeddedTokensFile = Paths.get(inputPath, "embedding", "embeddedTokens.txt");
		Map<String, String> embeddedTokens = readEmbeddedTokens(embeddedTokensFile);
		StringBuilder zeroVector = new StringBuilder();
		int size = Configuration.SIZE_OF_EMBEDDED_VECTOR - 1;
		for (int i = 0; i < size; i ++) {
			zeroVector.append("0,");
		}
		zeroVector.append("0");
		
		if (benchMark) {
			File[] files = Paths.get(inputPath, "SelectedData", "TrainingData").toFile().listFiles();
			int maxSize = 0;
			File trainingDataFile = null;
			for (File file : files) {
				String fileName = file.getName();
				if (fileName.startsWith("Tokens_MaxSize=")) {
					maxSize = Integer.parseInt(fileName.substring("Tokens_MaxSize=".length(), fileName.lastIndexOf(".txt")));
					trainingDataFile = file;
				}
			}
			vectorizeTokenVector(trainingDataFile, embeddedTokens, maxSize, zeroVector, outputPath + "TrainingData_");
			
			File renamedMethodsFile = Paths.get(inputPath, "RenamedMethods", "MethodTokens.txt").toFile();
			vectorizeTokenVector(renamedMethodsFile, embeddedTokens, maxSize, zeroVector, outputPath + "RenamedData_");
		} else {
			File[] files = Paths.get(inputPath, "TrainingData").toFile().listFiles();
			File trainingDataFile = null;
			int maxSize = 0;
			for (File file : files) {
				String fileName = file.getName();
				if (fileName.startsWith("Tokens_MaxSize=")) {
					maxSize = Integer.parseInt(fileName.substring("Tokens_MaxSize=".length(), fileName.lastIndexOf(".txt")));
					trainingDataFile = file;
				}
			}
			vectorizeTokenVector(trainingDataFile, embeddedTokens, maxSize, zeroVector, outputPath + "TrainingData_");
			
			File testingDataFile = Paths.get(inputPath, "TestingData", trainingDataFile.getName()).toFile();
			vectorizeTokenVector(testingDataFile, embeddedTokens, maxSize, zeroVector, outputPath + "TestingData_");
		}
	}

	public Map<String, String> readEmbeddedTokens(Path embeddedTokensFile) throws IOException {
		Map<String, String> embeddedTokens = new HashMap<>();
		File file = embeddedTokensFile.toFile();
		FileInputStream fis;
		Scanner scanner;
		fis = new FileInputStream(file);
		scanner = new Scanner(fis);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			int firstBlankIndex = line.indexOf(" ");
			String token = line.substring(0, firstBlankIndex);
			String value = line.substring(firstBlankIndex + 1).replaceAll(" ", ",");
			embeddedTokens.put(token, value);
		}
		scanner.close();
		fis.close();
		
		return embeddedTokens;
	}

	public void vectorizeTokenVector(File tokenVectorsFile, Map<String, String> embeddedTokens, int maxSize, StringBuilder zeroVector, String outputFileName) throws IOException {
		Path csvOutputPath = Paths.get(outputFileName + tokenVectorsFile.getName().replace(".txt", ".csv"));
		
		FileInputStream fis = new FileInputStream(tokenVectorsFile);
		Scanner scanner = new Scanner(fis);
		int vectorSize = 0;
		StringBuilder builder = new StringBuilder();
		int counter = 0;
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			List<String> methodBodyTokens = Arrays.asList(line.split(" "));
			SingleVectorizer vecBody = new SingleVectorizer();
			vecBody.vectorize(methodBodyTokens, embeddedTokens, maxSize, zeroVector);
			StringBuilder vectorizedTokenVector = vecBody.numericVector;
			int length = vectorizedTokenVector.toString().trim().split(",").length;
			if (length != vectorSize) {
				vectorSize = length;
			}
			builder.append(vectorizedTokenVector).append("\n");
			counter ++;
			if (counter % 500 == 0) {
				FileHelper.outputToFile(csvOutputPath, builder.toString(), true);
				builder.setLength(0);
			}
		}
		scanner.close();
		fis.close();
		
		FileHelper.outputToFile(csvOutputPath, builder.toString(), true);
		builder.setLength(0);
	}
	
	/**
	 * Single vectorizer of a single token vector.
	 * 
	 * @author kui.liu
	 *
	 */
	public static class SingleVectorizer {
		private StringBuilder numericVector = new StringBuilder();
		
		/**
		 * Append symbol "," in each iteration.
		 * 
		 * @param tokenVector
		 * @param embeddedTokens
		 * @param maxSize
		 * @param zeroVector
		 */
		public void vectorize(List<String> tokenVector, Map<String, String> embeddedTokens, int maxSize, StringBuilder zeroVector) {
			int i = 0;
			for (; i < tokenVector.size(); i ++) {
				String numericVectorOfSingleToken = embeddedTokens.get(tokenVector.get(i));
				if (numericVectorOfSingleToken == null) {
					numericVectorOfSingleToken = zeroVector.toString();
				}
				numericVector.append(numericVectorOfSingleToken);
				if (i < maxSize - 1) {
					numericVector.append(",");
				}
			}
			for (; i < maxSize; i ++) {
				numericVector.append(zeroVector);
				if (i < maxSize - 1) {
					numericVector.append(",");
				}
			}
		}
	}
}
