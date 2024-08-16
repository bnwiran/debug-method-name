package edu.lu.uni.serval.renamed.methods;

import java.io.File;
import java.util.List;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

/**
 * Collect renamed methods from the commit history of a Java repo.
 * 
 * @author kui.liu
 *
 */
public class RenamedMethodsCollector {

	private static final String INPUT_DATA_PATH = Configuration.getCommitDiffPath() + "/";
	private static final String OUTPUT_DATA_PATH = Configuration.getRenamedMethodsPath() + "/";

	public static void collect(String projectName) {
		String filePath = INPUT_DATA_PATH + projectName;
		if (!new File(filePath).exists()) {
			File projectPath = new File(Configuration.getReposRootPath() + "/" + projectName);
			if (!projectPath.exists()) return;
			File[] projectFiles = projectPath.listFiles();
			if (projectFiles.length == 0) {
				projectPath.delete();
				return;
			}

			File projectFile = projectFiles[0];
			String projectGit = projectFile.getPath() + "/.git";
			filePath = INPUT_DATA_PATH + projectName;

			if (!new File(filePath).exists()) {
				CommitDiffs.traverseGitRepos(projectName, projectGit);
			}

			String outputPath = OUTPUT_DATA_PATH + projectName + "/";
			parseRenamedMethods(filePath, outputPath);
		} else {
			String outputPath = OUTPUT_DATA_PATH + projectName + "/";
			parseRenamedMethods(filePath, outputPath);
		}
	}

	private static void parseRenamedMethods(String inputProject, String outputPath) {
		ActorSystem system = null;
		ActorRef gitTravelActor = null;
		try {
			system = ActorSystem.create("methodNames-system");
			gitTravelActor = system.actorOf(ParseActor.props(inputProject, outputPath), "parse-actor");
			gitTravelActor.tell("BEGIN", ActorRef.noSender());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
