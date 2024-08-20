package edu.lu.uni.serval.dlMethods;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import edu.lu.uni.Configuration;
import edu.lu.uni.serval.MethodName.detector.MethodNameFeatureLearner;

/**
 * Learn features of method names with ParagraphVectors.
 * 
 * @author kui.liu
 *
 */
public class MethodNameLearner {
	
	public static void main(String[] args) throws IOException {
		MethodNameFeatureLearner learner = new MethodNameFeatureLearner();
		Path testingData = Paths.get(Configuration.SELECTED_RENAMED_DATA_PATH, "ParsedMethodNames.txt");
		
		// Selecting data for method name feature learning.
		Path testingMethodNamesFile = Paths.get(Configuration.EVALUATION_DATA_PATH, "TestingMethodNames.txt");
		learner.prepareData(testingData, testingMethodNamesFile, Paths.get(Configuration.EVALUATION_DATA_PATH, "TestingLabels.txt"));

		Path trainingData = Paths.get(Configuration.SELECTED_DATA_PATH, "SelectedMethodInfo.txt");
		Path featureLearningData1 = Paths.get(Configuration.EVALUATION_DATA_PATH, "FeatureLearningData1.txt"); // without return type.
		Path featureLearningData2 = Paths.get(Configuration.EVALUATION_DATA_PATH, "FeatureLearningData2.txt"); // with return type.
		Path returnTypeOfTestingFile = Paths.get(Configuration.SELECTED_RENAMED_DATA_PATH, "MethodInfo.txt");
		
		learner.prepareFeatureLearningData(trainingData, testingMethodNamesFile, featureLearningData1, featureLearningData2, returnTypeOfTestingFile);
		
		learner.learnFeatures(featureLearningData1, Paths.get(Configuration.EVALUATION_DATA_PATH, "MethodNameFeatures_1_Size=" + learner.SIZE + ".txt"));
		learner.learnFeatures(featureLearningData2, Paths.get(Configuration.EVALUATION_DATA_PATH, "MethodNameFeatures_2_Size=" + learner.SIZE + ".txt"));
//		learner.learnFeatures(new File(featureLearningData1 + ".bak"), outputPath + "MethodNameFeatures_1_Size=" + learner.SIZE + ".txt.bak");
//		learner.learnFeatures(new File(featureLearningData2 + ".bak"), outputPath + "MethodNameFeatures_2_Size=" + learner.SIZE + ".txt.bak");
	}
	
}
