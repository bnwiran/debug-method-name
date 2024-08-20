package edu.lu.uni.serval;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import edu.lu.uni.serval.akka.method.parser.MultipleShreadParser;
import edu.lu.uni.serval.method.parser.MethodParser;
import edu.lu.uni.serval.utils.FileHelper;

/**
 * Examples of the tool: parse methods.
 * 
 * @author kui.liu
 *
 */
public class MainParser {
	
	public static void main(String[] args) {
		MainParser parser = new MainParser();
		
		try {
			/*
			 * Parse Java code methods with multiple threads.
			 * The input is all obtained Java code files of one Java project.
			 * It needs to merge all output data if using this choice.
			 * 
			 */
			String reposFileName = args[0];
			List<String> projects = readList(reposFileName);
			int i = Integer.parseInt(args[1]); // 0 - 429: 430 Java projects.
			if (i >= projects.size()) {
				System.out.println("Wrong parameter: " + args[0]);
				return;
			}
			String projectName = projects.get(i);
			String project = Configuration.JAVA_FILES_PATH + projectName + ".txt";
			if (! new File(project).exists()) {
				project = Configuration.JAVA_REPOS_PATH + projectName;
			}
			parser.parseMethodsWithMultipleThreads(project, projectName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Parse method bodies: tokenize and vectorize method bodies.
	 * 
	 * @throws IOException
	 */
	public void parseMethodsWithSingleThread() throws IOException {
		List<String> projects = Configuration.PROJECTS;

		String outputPath = Configuration.TOKENIZED_METHODS_PATH;
		// Clear existing output data generated at the last time.
		FileHelper.deleteFile(outputPath);

		MethodParser mp = new MethodParser();
		mp.parseProjects(projects, outputPath);
	}
	
	/**
	 * Parse method bodies with multiple threads.
	 * 
	 * One thread is used to parse one project.
	 * 
	 * @throws IOException
	 */
	public void parseMethodsWithMultipleThreads() throws IOException {
		List<String> projects = Configuration.PROJECTS;

		String outputPath = Configuration.TOKENIZED_METHODS_PATH;
		// Clear existing output data generated at the last time.
		FileHelper.deleteFile(outputPath);

		int numberOfWorkers = 430;
		MultipleShreadParser parser = new MultipleShreadParser(projects, outputPath, numberOfWorkers);
		parser.parseMethods();
	}
	
	/**
	 * Parse method bodies with multiple threads.
	 * @throws IOException
	 */
	public void parseMethodsWithMultipleThreads(String allJavaFilesFile) throws IOException {
		String outputPath = Configuration.TOKENIZED_METHODS_PATH;
		int numberOfWorkers = 1000;
		MultipleShreadParser parser = new MultipleShreadParser(allJavaFilesFile, outputPath, numberOfWorkers);
		parser.parseMethods();
	}
	
	/**
	 * Parse method bodies with multiple threads.
	 * 
	 * @throws IOException
	 */
	public void parseMethodsWithMultipleThreads(String project, String projectName) throws IOException {
		String outputPath = Configuration.TOKENIZED_METHODS_PATH + projectName + "/";
		int numberOfWorkers = 1000;
		MultipleShreadParser parser = new MultipleShreadParser(project, outputPath, numberOfWorkers);
		parser.parseMethods();
	}

	private static List<String> readList(String reposFileName) throws IOException {
		return Files.readAllLines(Path.of(reposFileName).toAbsolutePath()).stream()
			.map(line -> line.split("/"))
			.map(line -> {
				int index = line[line.length - 1].indexOf(".git");
				return line[line.length - 1].substring(0, index);
			}).toList();
	}
	
}
