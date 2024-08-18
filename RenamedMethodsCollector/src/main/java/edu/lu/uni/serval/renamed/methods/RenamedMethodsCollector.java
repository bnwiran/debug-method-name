package edu.lu.uni.serval.renamed.methods;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import edu.lu.uni.serval.git.travel.CommitDiffEntry;
import edu.lu.uni.serval.git.travel.GitRepository;
import edu.lu.uni.serval.utils.FileHelper;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collect renamed methods from the commit history of a Java repo.
 *
 * @author kui.liu
 */
public class RenamedMethodsCollector {
    private static Logger log = LoggerFactory.getLogger(RenamedMethodsCollector.class);

    public static void collect(final String projectName) {
        final File commitDiffDirectory = Path.of(Configuration.getCommitDiffPath(), projectName).toFile();
        if (!commitDiffDirectory.exists()) {
            File projectPath = Path.of(Configuration.getReposRootPath(), projectName).toFile();
            if (!projectPath.exists()) {
				return;
			}

            String projectGit = Path.of(projectPath.getAbsolutePath(), ".git").toString();
			traverseGitRepos(projectName, projectGit);
        }

        String outputPath = Path.of(Configuration.getRenamedMethodsPath(), projectName).toAbsolutePath().toString();
        parseRenamedMethods(commitDiffDirectory.getAbsolutePath(), outputPath);
    }

    private static void traverseGitRepos(String projectName, String projectGit) {
        String revisedFilesPath = Configuration.getCommitDiffPath() + "/" + projectName + "/revFiles/";
        String previousFilesPath = Configuration.getCommitDiffPath() + "/" + projectName + "/prevFiles/";
        FileHelper.createDirectory(revisedFilesPath);
        FileHelper.createDirectory(previousFilesPath);
        FileHelper.deleteDirectory(Configuration.getCommitDiffPath() + "/" + projectName + "/DiffEntries/");
        GitRepository gitRepo = new GitRepository(revisedFilesPath, previousFilesPath);

        try {
            gitRepo.open(projectGit);
            gitRepo.createFilesForGumTree(true);
        } catch (Exception e) {
            log.error("Error in reading {}: {}", projectName, e.getMessage());
        } finally {
            gitRepo.close();
        }
    }

    private static void parseRenamedMethods(String inputProject, String outputPath) {
        ActorSystem system;
        ActorRef gitTravelActor;
        try {
            system = ActorSystem.create("methodNames-system");
            gitTravelActor = system.actorOf(ParseActor.props(inputProject, outputPath), "parse-actor");
            gitTravelActor.tell("BEGIN", ActorRef.noSender());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

}
