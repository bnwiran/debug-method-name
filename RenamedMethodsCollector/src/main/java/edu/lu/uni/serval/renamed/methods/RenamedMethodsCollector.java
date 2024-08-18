package edu.lu.uni.serval.renamed.methods;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import edu.lu.uni.serval.git.travel.GitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

/**
 * Collect renamed methods from the commit history of a Java repo.
 *
 * @author kui.liu
 */
public class RenamedMethodsCollector {
  private final static Logger log = LoggerFactory.getLogger(RenamedMethodsCollector.class);

  public static void collect(final String projectName) {
    final File commitDiffDirectory = Path.of(Configuration.getCommitDiffPath(), projectName).toFile();
    if (!commitDiffDirectory.exists()) {
      File projectPath = Path.of(Configuration.getReposRootPath(), projectName).toFile();
      if (!projectPath.exists()) {
        return;
      }

      Path projectGit = Path.of(projectPath.getAbsolutePath(), ".git");
      traverseGitRepos(projectName, projectGit);
    }

    String outputPath = Path.of(Configuration.getRenamedMethodsPath(), projectName).toAbsolutePath().toString();
    parseRenamedMethods(commitDiffDirectory.getAbsolutePath(), outputPath);
  }

  private static void traverseGitRepos(String projectName, Path projectGit) {
    GitRepository gitRepo = new GitRepository(projectName);

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
