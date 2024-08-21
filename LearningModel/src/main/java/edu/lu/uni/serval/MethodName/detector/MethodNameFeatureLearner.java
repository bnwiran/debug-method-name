package edu.lu.uni.serval.MethodName.detector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;

import edu.lu.uni.Configuration;
import edu.lu.uni.serval.deeplearner.SentenceEncoder;
import edu.lu.uni.serval.utils.FileHelper;
import edu.lu.uni.serval.utils.ReturnType;
import edu.lu.uni.serval.utils.ReturnType.ReturnTypeClassification;

/**
 * Learn features of method names with ParagraphVectors.
 * 
 * @author kui.liu
 *
 */
public class MethodNameFeatureLearner {
	
	public int SIZE = 0;

	public static void main(String[] args) throws IOException {
		MethodNameFeatureLearner learner = new MethodNameFeatureLearner();
		String inputPath = Configuration.DL_DATA_PATH;
		Path methodNameTokensFile = Paths.get(Configuration.RENAMED_METHODS_PATH, "ParsedMethodNames.txt");
		String outputPath = Configuration.EVALUATION_DATA_PATH;
		// Selecting data for method name feature learning.
		Path testingMethodNamesFile = Paths.get(outputPath, "TestingMethodNames.txt");
		learner.prepareData(methodNameTokensFile, testingMethodNamesFile, Paths.get(Configuration.EVALUATION_DATA_PATH, "TestingLabels.txt"));

		Path trainingData = Paths.get(inputPath, "SelectedData", "SelectedMethodInfo.txt");
		Path featureLearningData1 = Paths.get(outputPath, "FeatureLearningData1.txt"); // without return type.
		Path featureLearningData2 = Paths.get(outputPath, "FeatureLearningData2.txt"); // with return type.
		Path returnTypeOfTestingFile = Paths.get(inputPath, "RenamedMethods", "MethodInfo.txt");
		learner.prepareFeatureLearningData(trainingData, testingMethodNamesFile, featureLearningData1, featureLearningData2, returnTypeOfTestingFile);
		
		learner.learnFeatures(featureLearningData1, Paths.get(outputPath, "MethodNameFeatures_1_Size=" + learner.SIZE + ".txt"));
		learner.learnFeatures(featureLearningData2, Paths.get(outputPath, "MethodNameFeatures_2_Size=" + learner.SIZE + ".txt"));
//		learner.learnFeatures(featureLearningData1 + ".bak", outputPath + "MethodNameFeatures_1_Size=" + learner.SIZE + ".txt.bak");
//		learner.learnFeatures(featureLearningData2 + ".bak"), outputPath + "MethodNameFeatures_2_Size=" + learner.SIZE + ".txt.bak");
	} 

	public void prepareData(Path methodNameTokensFile, Path outputFile, Path outputLabelFile) throws IOException {
		// tokens of old method names @ tokens of new method names.
		List<String> methodNames = readFile(methodNameTokensFile);
		int numConsistent = methodNames.size() / 2;
		int numInconsistent = methodNames.size() - numConsistent;
		StringBuilder builder = new StringBuilder();
		StringBuilder labelBuilder = new StringBuilder();
		StringBuilder builder2 = new StringBuilder();
		StringBuilder labelBuilder2 = new StringBuilder();
		Random random = new Random();
		for (String methodNameStr : methodNames) {
			int index = methodNameStr.indexOf("@");
			int nextNumber = random.nextInt(100);
			String methodName;
			String methodName2;
			if (nextNumber < 50) {// inconsistent data: old name.
				if (numInconsistent == 0) {
					labelBuilder.append("1\n");
					methodName = methodNameStr.substring(index + 1);
					labelBuilder2.append("0\n");
					methodName2 = methodNameStr.substring(0, index);
				} else {
					labelBuilder.append("0\n");
					methodName = methodNameStr.substring(0, index);
					labelBuilder2.append("1\n");
					methodName2 = methodNameStr.substring(index + 1);
					numInconsistent --;
				}
			} else { // consistent data: new name.
				if (numConsistent == 0) {
					labelBuilder.append("0\n");
					methodName = methodNameStr.substring(0, index);
					labelBuilder2.append("1\n");
					methodName2 = methodNameStr.substring(index + 1);
				} else {
					numConsistent --;
					labelBuilder.append("1\n");
					methodName = methodNameStr.substring(index + 1);
					labelBuilder2.append("0\n");
					methodName2 = methodNameStr.substring(0, index);
				}
			}
			builder.append(methodName.replace(",", " ").toLowerCase(Locale.ROOT));
			builder.append("\n");
			builder2.append(methodName2.replace(",", " ").toLowerCase(Locale.ROOT));
			builder2.append("\n");
			SIZE ++;
		}

		FileHelper.outputToFile(outputFile, builder.toString(), false);
		FileHelper.outputToFile(outputLabelFile, labelBuilder.toString(), false);
		FileHelper.outputToFile(Paths.get(outputFile + ".bak"), builder2.toString(), false);
		FileHelper.outputToFile(Paths.get(outputLabelFile + ".bak"), labelBuilder2.toString(), false);
	}

	private List<String> readFile(Path methodNameTokensFile) throws IOException {
		List<String> methodNames = new ArrayList<>();
		String content = FileHelper.readFile(methodNameTokensFile.toFile());
		BufferedReader reader = new BufferedReader(new StringReader(content));
		String line = null;
		while ((line = reader.readLine()) != null) {
			methodNames.add(line);
		}
		reader.close();
		return methodNames;
	}

	public void prepareFeatureLearningData(Path trainingData, Path testingMethodNamesFile,
																				 Path featureLearningData1, Path featureLearningData2, Path returnTypeOfTestingFile) throws IOException {
		FileInputStream fis = new FileInputStream(trainingData.toFile());
		Scanner scanner = new Scanner(fis);

		StringBuilder builder = new StringBuilder();
		StringBuilder returnTypeBuilder = new StringBuilder();
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			int index = line.lastIndexOf("@");
			String tokens = line.substring(index + 1).replace(",", " ").toLowerCase(Locale.ROOT);
			builder.append(tokens).append("\n");
			
			line = line.substring(0, index);
			index = line.lastIndexOf("@");
			String returnType = line.substring(index + 1);
			returnTypeBuilder.append(returnType).append(" ").append(tokens).append("\n");
		}
		scanner.close();
		fis.close();
		
		FileHelper.outputToFile(featureLearningData1, builder.toString(), false);
		FileHelper.outputToFile(Paths.get(featureLearningData1 + ".bak"), builder.toString(), false);
		String content = FileHelper.readFile(testingMethodNamesFile.toFile());
		String contentBak = FileHelper.readFile(Paths.get(testingMethodNamesFile + ".bak").toFile());
		FileHelper.outputToFile(featureLearningData1, content, true);
		FileHelper.outputToFile(Paths.get(featureLearningData1 + ".bak"), contentBak, true);
		
		if (featureLearningData2 != null) {
			FileHelper.outputToFile(featureLearningData2, returnTypeBuilder.toString(), false);
			FileHelper.outputToFile(Paths.get(featureLearningData2 + ".bak"), returnTypeBuilder.toString(), false);
		}
		returnTypeBuilder.setLength(0);
		StringBuilder returnTypeBuilderBak = new StringBuilder();

		List<String> tokensList = readTokensList(content);
		List<String> tokensListBak = readTokensList(contentBak);
		
		BufferedReader reader = new BufferedReader(new StringReader(FileHelper.readFile(returnTypeOfTestingFile.toFile())));
		int index = 0;
		String line = null;
		while ((line = reader.readLine()) != null) {
			String returnType = line.substring(line.lastIndexOf(":") + 1);
			returnType = new ReturnType().readReturnType(returnType, ReturnTypeClassification.ABSTRACT);
			returnTypeBuilder.append(returnType).append(" ").append(tokensList.get(index)).append("\n");
			returnTypeBuilderBak.append(returnType).append(" ").append(tokensListBak.get(index)).append("\n");
			index ++;
		}
		reader.close();
		
		if (featureLearningData2 != null) {
			FileHelper.outputToFile(featureLearningData2, returnTypeBuilder.toString(), true);
			FileHelper.outputToFile(Paths.get(featureLearningData2 + ".bak"), returnTypeBuilderBak.toString(), true);
		}
	}

	private List<String> readTokensList(String content) throws IOException {
		List<String> tokensList = new ArrayList<>();
		BufferedReader reader = new BufferedReader(new StringReader(content));
		String line = null;
		while ((line = reader.readLine()) != null) {
			tokensList.add(line);
		}
		reader.close();
		return tokensList;
	}
	
	public void learnFeatures(Path inputFile, Path outputFileName) throws FileNotFoundException {
		FileHelper.deleteFile(outputFileName);
		SentenceEncoder encoder = new SentenceEncoder();
		int minWordFrequency = 1;
		int layerSize = 300;
		int windowSize = 2;
		encoder.encodeSentences(inputFile, minWordFrequency, layerSize, windowSize, outputFileName);
	}
	
}
