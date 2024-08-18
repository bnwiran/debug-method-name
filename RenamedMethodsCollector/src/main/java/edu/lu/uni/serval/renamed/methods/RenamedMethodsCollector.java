package edu.lu.uni.serval.renamed.methods;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

/**
 * Collect renamed methods from the commit history of a Java repo.
 *
 * @author kui.liu
 */
public class RenamedMethodsCollector {

    public static void collect(final String projectName) {
        final File commitDiffDirectory = Path.of(Configuration.getCommitDiffPath(), projectName).toFile();
        if (!commitDiffDirectory.exists()) {
            File projectPath = Path.of(Configuration.getReposRootPath(), projectName).toFile();
            if (!projectPath.exists()) {
				return;
			}

            String projectGit = Path.of(projectPath.getAbsolutePath(), ".git").toString();
			CommitDiffs.traverseGitRepos(projectName, projectGit);
        }

        String outputPath = Path.of(Configuration.getRenamedMethodsPath(), projectName).toAbsolutePath().toString();
        parseRenamedMethods(commitDiffDirectory.getAbsolutePath(), outputPath);
    }

    private static void parseRenamedMethods(String inputProject, String outputPath) {
        ActorSystem system;
        ActorRef gitTravelActor;
        try {
            system = ActorSystem.create("methodNames-system");
            gitTravelActor = system.actorOf(ParseActor.props(inputProject, outputPath), "parse-actor");
            gitTravelActor.tell("BEGIN", ActorRef.noSender());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
