package edu.lu.uni.serval.git.filter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineDiffFilter {
	private static final String REGULAR_EXPRESSION = "^@@\\s\\-\\d+,*\\d*\\s\\+\\d+,*\\d*\\s@@$"; //@@ -21,0 +22,2 @@
	private static final Pattern pattern = Pattern.compile(REGULAR_EXPRESSION);
	
	public static boolean filterSignal(String string) {
		return pattern.matcher(string).matches();
	}
}
