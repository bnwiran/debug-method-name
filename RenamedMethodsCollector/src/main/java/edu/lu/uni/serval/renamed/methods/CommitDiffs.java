package edu.lu.uni.serval.renamed.methods;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.lu.uni.serval.git.exception.GitRepositoryNotFoundException;
import edu.lu.uni.serval.git.exception.NotValidGitRepositoryException;
import edu.lu.uni.serval.git.travel.CommitDiffEntry;
import edu.lu.uni.serval.git.travel.GitRepository;
import edu.lu.uni.serval.utils.FileHelper;

/**
 * Collect all commits and generate previous java files and revised java files.
 * 
 * @author kui.liu
 *
 */
public class CommitDiffs {

	private static Logger log = LoggerFactory.getLogger(CommitDiffs.class);

	public static List<String> readList(String fileName) throws IOException {
		return Files.readAllLines(Path.of(fileName));
	}

	public static void traverseGitRepos(String projectName, String projectGit) {
		String revisedFilesPath = Configuration.getCommitDiffPath() + "/" + projectName + "/revFiles/";
		String previousFilesPath = Configuration.getCommitDiffPath() + "/" + projectName + "/prevFiles/";
		FileHelper.createDirectory(revisedFilesPath);
		FileHelper.createDirectory(previousFilesPath);
		FileHelper.deleteDirectory(Configuration.getCommitDiffPath() + "/" + projectName + "/DiffEntries/");
		GitRepository gitRepo = new GitRepository(projectGit, revisedFilesPath, previousFilesPath);
		try {
			gitRepo.open();
			List<RevCommit> commits = gitRepo.getAllCommits(false);
            log.debug("{} Commits: {}", projectName, commits.size());
			List<CommitDiffEntry> commitDiffentries = gitRepo.getCommitDiffEntries(commits);
			// previous java file vs. modified java file
			gitRepo.createFilesForGumTree(commitDiffentries, true);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			gitRepo.close();
		}
	}
}
