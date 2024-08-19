package edu.lu.uni.serval.renamed.methods;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {
	public static final Long Timeout = 300L;

	private static class SingletonHelper {
		private static final Configuration INSTANCE = new Configuration();
	}

	private final Properties properties;

	private Configuration() {
		properties = new Properties();
		try(InputStream resourceStream = getClass().getClassLoader().getResourceAsStream("config.properties")) {
			properties.load(resourceStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getOutputPath() {
		return SingletonHelper.INSTANCE.properties.getProperty("data.output.path");
	}

	public static String getCommitDiffPath() {
		return getOutputPath() + "/Commit_Diffs";
	}

	public static String getRenamedMethodsPath() {
		return getOutputPath() + "/RenamedMethods";
	}
}
