package edu.lu.uni.serval.renamed.methods;

import java.io.IOException;
import java.util.List;

public class Main {
	/**
	 * Collect renamed methods for all project.
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		List<String> projects = CommitDiffs.readList(Configuration.getReposRootPath() + "/repos.txt");
		for (String project : projects) {
			CommitDiffs.traverseGitRepos(project, Configuration.getReposRootPath() + "/" + project + "/.git");
		}
		for (String project : projects) {
			RenamedMethodsCollector.collect(project);
		}
	}

}
