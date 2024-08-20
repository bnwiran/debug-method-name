package edu.lu.uni.serval.dlMethods;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import edu.lu.uni.Configuration;

/**
 * Prepare data for deep learning of methods.
 * @author kui.liu
 *
 */
public class DataPreparer {
	private static final int QUANTITY = 1; // the number of methods of which names start with the same token.
	private static final int MIN_SIZE = 0; // The minimum size of vectors.

	public static void main(String[] args) throws IOException {
		List<String> projects = readList(args[0]);
		for (String project : projects) {
			String inputPath = Configuration.TOKENIZED_METHODS_PATH + project + "/";
			String renamedMethodsPath = Configuration.RENAMED_METHODS_PATH + project + "/ActualRenamed/";
			String outputPath1 = Configuration.SELECTED_DATA_PATH + project + "/";
			String outputPath2 = Configuration.SELECTED_RENAMED_DATA_PATH + project + "/";

			DataInitializer dataInit = new DataInitializer();
			dataInit.QUANTITY = QUANTITY;
			dataInit.MIN_SIZE = MIN_SIZE;
			dataInit.inputPath = inputPath;
			dataInit.outputPath1 = outputPath1;
			dataInit.outputPath2 = outputPath2;
			dataInit.renamedMethodsPath = renamedMethodsPath;
			dataInit.initializeData(projects);
			dataInit.selectMethod();
		}
		
		DataMerger.merge(projects);
	}

	private static List<String> readList(String reposFileName) throws IOException {
		return Files.readAllLines(Paths.get(reposFileName)).stream()
				.map(line -> line.split("/"))
				.map(line -> {
					int index = line[line.length - 1].indexOf(".git");
					return line[line.length - 1].substring(0, index);
				}).collect(Collectors.toList());
	}

}
