package edu.lu.uni.serval.utils;

import java.util.List;

public class Distribution {
	
	public enum MaxSizeType {  
		UpperWhisker, ThirdQuartile
	}

	public static int computeMaxSize(MaxSizeType maxSizeType, List<Integer> sizesDistribution) {
		return switch (maxSizeType) {
      case UpperWhisker -> upperWhisker(sizesDistribution);
      case ThirdQuartile -> thirdQuarter(sizesDistribution);
    };
	}

	private static int upperWhisker(final List<Integer> sizesDistribution) {
		List<Integer> sortedSizesDistribution = sizesDistribution.stream().sorted().toList();
		int firstQuarterIndex = (int) (0.25 * sortedSizesDistribution.size());
		int firstQuarter = sortedSizesDistribution.get(firstQuarterIndex);
		int thirdQuarterIndex = (int) (0.75 * sortedSizesDistribution.size());
		int thirdQuarter = sortedSizesDistribution.get(thirdQuarterIndex);
		int upperWhisker = thirdQuarter + (int) (1.5 * (thirdQuarter - firstQuarter));
		int maxSize = sortedSizesDistribution.getLast();
		return Math.min(upperWhisker, maxSize);
	}
	
	private static int thirdQuarter(final List<Integer> sizesDistribution) {
		List<Integer> sortedSizesDistribution = sizesDistribution.stream().sorted().toList();
		int thirdQuarterIndex = (int)(0.75 * sortedSizesDistribution.size());
    return sortedSizesDistribution.get(thirdQuarterIndex);
	}
}
