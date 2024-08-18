package edu.lu.uni.serval.renamed.methods;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
	/**
	 * Collect renamed methods for all project.
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		String reposFileName = args[0];
		List<String> projects = readList(reposFileName);

		long l = System.currentTimeMillis();
		projects.forEach(project -> {
			CommitDiffs.traverseGitRepos(project, Configuration.getReposRootPath() + "/" + project + "/.git");
			RenamedMethodsCollector.collect(project);
		});
		System.out.println((System.currentTimeMillis() - l) + "ms");
	}

	private static List<String> readList(String reposFileName) throws IOException {
		return Files.readAllLines(Path.of(Configuration.getReposRootPath() + "/" + reposFileName)).stream()
				.map(line -> line.split("/"))
				.map(line -> {
					int index = line[line.length-1].indexOf(".git");
					return line[line.length-1].substring(0, index);
				}).toList();
	}

}
