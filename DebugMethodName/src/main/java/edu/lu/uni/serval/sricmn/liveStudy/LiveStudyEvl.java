package edu.lu.uni.serval.sricmn.liveStudy;

import edu.lu.uni.serval.sricmn.TokenSuggestor;
import edu.lu.uni.serval.sricmn.info.MethodInfo;
import edu.lu.uni.serval.sricmn.info.PredictToken;
import edu.lu.uni.serval.utils.FileHelper;
import edu.lu.uni.serval.utils.MapSorter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LiveStudyEvl {

	public void evaluate(List<MethodInfo> trainingMethodInfo, List<MethodInfo> testingMethodInfo, Map<Integer, Map<Integer, Double>> topSimilarBodyMap,
			Map<Integer, Map<Integer, Double>> topSimilarNameMap, int topNum, String outputPath, int numOfPredictTokens, boolean identifyWithTopOne) {
		int size = testingMethodInfo.size();
		
		StringBuilder resultsBuilder = new StringBuilder();
		int a = 0;
		for (int index = 0; index < size; index ++) {
			Map<Integer, Double> bodySimilarities = topSimilarBodyMap.get(index);
			if (bodySimilarities.isEmpty()) {
				continue;
			}
			MapSorter<Integer, Double> sorter = new MapSorter<Integer, Double>();
			bodySimilarities = sorter.sortByValueDescending(bodySimilarities);
			
			List<PredictToken> ptList = new ArrayList<>(); // used to recommend method names.
			List<String> tokensPredictedByBodyList = new ArrayList<>();// used to identify inconsistent method names
			List<String> tokensPredictedByNameList = new ArrayList<>();// used to identify inconsistent method names
			int counter = 0;
			for (Map.Entry<Integer, Double> entry : bodySimilarities.entrySet()) {
				int trainingMethodIndex = entry.getKey();
				Double similarity = entry.getValue();
				MethodInfo methodInfo = trainingMethodInfo.get(trainingMethodIndex);
				List<String> similarRawTokens = methodInfo.getMethodNameTokens();// 1 to numOfPredictTokens raw tokens
				StringBuilder token = new StringBuilder();
				for (int i = 0, length = similarRawTokens.size(); i < numOfPredictTokens && i < length; i ++) {// numOfPredictTokens: 1 or 3.
					token.append(similarRawTokens.get(i)).append("_");
				}
				
				PredictToken pt = new PredictToken(token.toString().toLowerCase());
				int index1 = ptList.indexOf(pt);
				if (index1 > -1) {
					pt = ptList.get(index1);
					pt.setSimilarity(pt.getSimilarity() + similarity);
					pt.setTimes(pt.getTimes() + 1);
				} else {
					pt.setSimilarity(similarity);
					pt.setTimes(1);
					ptList.add(pt);
				}
				
				String firstSubToken = similarRawTokens.getFirst();
				if (!tokensPredictedByBodyList.contains(firstSubToken)) {
					tokensPredictedByBodyList.add(firstSubToken);
				}
				
				counter ++;
				if (counter >= topNum) break;
			}
			
			Map<Integer, Double> nameSimilarities = topSimilarNameMap.get(index);
			nameSimilarities = sorter.sortByValueDescending(nameSimilarities);
			for (Map.Entry<Integer, Double> entry : nameSimilarities.entrySet()) {
				int methodIndex = entry.getKey();
				MethodInfo methodInfo = trainingMethodInfo.get(methodIndex);
				String token = methodInfo.getMethodNameTokens().getFirst();
				if (!tokensPredictedByNameList.contains(token)) {
					tokensPredictedByNameList.add(token);
				}
				counter --;
				if (counter <= 0) break;
			}
			
			boolean isInconsistent = false;
			if (identifyWithTopOne) {
				if (!tokensPredictedByBodyList.getFirst().equals(tokensPredictedByNameList.getFirst())) {
					isInconsistent = true;
				}
			} else {
				tokensPredictedByBodyList.retainAll(tokensPredictedByNameList);
				if (tokensPredictedByBodyList.isEmpty()) {
					isInconsistent = true;
				}
			}
			
			if (isInconsistent) {
				a ++;
				TokenSuggestor ts = new TokenSuggestor();
				List<String> predictTokens1 = ts.selectTop5PredictTokens1(ptList);
				List<String> predictTokens2 = ts.selectTop5PredictTokens2(ptList);
				List<String> predictTokens3 = ts.selectTop5PredictTokens3(ptList);

				resultsBuilder.append("T1: ").append(predictTokens1.toString()).append("\n");
				resultsBuilder.append("T2: ").append(predictTokens2.toString()).append("\n");
				resultsBuilder.append("T3: ").append(predictTokens3.toString()).append("\n");
				resultsBuilder.append(testingMethodInfo.get(index).getMethodBody()).append("\n");
			}
		}
		System.out.println("Identify with top-" + topNum + ", prediction with " + (identifyWithTopOne ? 1 : topNum) + ", sub-tokens: " + numOfPredictTokens + ", inconsistent names: " + a);
		FileHelper.outputToFile(Path.of(outputPath, "results.txt"), resultsBuilder.toString(), false);
	}

}
