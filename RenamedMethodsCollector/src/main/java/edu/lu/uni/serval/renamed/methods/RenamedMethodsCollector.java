package edu.lu.uni.serval.renamed.methods;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import edu.lu.uni.serval.git.travel.GitRepository;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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
      traverseGitRepos(projectName);
    }

    String outputPath = Path.of(Configuration.getRenamedMethodsPath(), projectName).toAbsolutePath().toString();
    parseRenamedMethods(commitDiffDirectory.getAbsolutePath(), outputPath);
  }

  private static void traverseGitRepos(String projectName) {
    GitRepository gitRepo = new GitRepository(projectName);

    try {
      gitRepo.open();
      gitRepo.createFilesForGumTree(true);
    } catch (GitAPIException | IOException e) {
      log.error("Error in reading {}: {}", projectName, e.getMessage());
    } finally {
      gitRepo.close();
    }
  }

  private static void parseRenamedMethods(String inputProject, String outputPath) {
    try {
      ActorSystem system = ActorSystem.create("methodNames-system");
      ActorRef gitTravelActor = system.actorOf(ParseActor.props(inputProject, outputPath), "parse-actor");
      gitTravelActor.tell("BEGIN", ActorRef.noSender());
    } catch (Exception e) {
      log.error(e.getMessage());
    }
  }

}
