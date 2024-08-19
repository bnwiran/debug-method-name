package edu.lu.uni.serval.git.travel;

import edu.lu.uni.serval.git.filter.LineDiffFilter;
import edu.lu.uni.serval.utils.FileHelper;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class GitRepository {

  private Git git;

  private final Path revisedFilesPath;
  private final Path previousFilesPath;
  private final Path difffEntriesPath;
  private final String projectName;

  private final Map<String, String> tooLongFileNames = new HashMap<>();

  /**
   * Creates a new GitRepository with a correct local path of a git repository.
   */
  public GitRepository(String projectName) {
    this.projectName = projectName;
    this.revisedFilesPath = Path.of(Configuration.getCommitDiffPath(), projectName, "revFiles");
    this.previousFilesPath = Path.of(Configuration.getCommitDiffPath(), projectName, "prevFiles");
    this.difffEntriesPath = Path.of(Configuration.getCommitDiffPath(), projectName, "DiffEntries");
  }

  /**
   * Open the git repository.
   *
   * @throws IOException
   */
  public void open() throws IOException {
    File projectPath = Path.of(Configuration.getReposRootPath(), projectName).toFile();
    Path repositoryPath = Path.of(projectPath.getAbsolutePath(), ".git");

    FileHelper.createDirectory(revisedFilesPath);
    FileHelper.createDirectory(previousFilesPath);
    FileHelper.deleteFile(difffEntriesPath);
    File repoDirectory = repositoryPath.toFile();

    FileRepositoryBuilder fileRepositoryBuilder = new FileRepositoryBuilder()
      .setGitDir(repoDirectory)
      .setMustExist(true)
      .readEnvironment()
      .findGitDir();

    Repository repository = fileRepositoryBuilder.build();
    git = new Git(repository);
  }

  /**
   * Close the git repository.
   */
  public void close() {
    if (Objects.nonNull(git)) {
      git.close();
      git.getRepository().close();
    }
  }

  private List<RevCommit> getAllCommits() throws GitAPIException, IOException {
    List<RevCommit> commits = new ArrayList<>();
    git.log().all().call().forEach(commits::add);

    return commits;
  }

  /**
   * Return the DiffEntry List of a RevCommit compared with its parent-RevCommits.
   *
   * @param revCommit A RevCommit.
   * @throws GitAPIException
   * @throws IOException
   * @throws RevisionSyntaxException
   */
  private List<CommitDiffEntry> getDiffEntriesForEachCommit(RevCommit revCommit) throws RevisionSyntaxException, IOException, GitAPIException {
    RevCommit[] parentRevCommits = revCommit.getParents();

    if (Objects.nonNull(parentRevCommits)) {
      List<CommitDiffEntry> diffEntries = new ArrayList<>();
      for (RevCommit parentCommit : parentRevCommits) {
        for (DiffEntry diffentry : getDiffEntries(revCommit, parentCommit)) {
          diffEntries.add(new CommitDiffEntry(revCommit, parentCommit, diffentry));
        }
      }

      return diffEntries;
    }

    return Collections.emptyList();
  }

  private List<DiffEntry> getDiffEntries(RevCommit revCommit, RevCommit parentCommit) throws IOException, GitAPIException {
    AbstractTreeIterator oldTreeParser = prepareTreeParser(parentCommit);
    AbstractTreeIterator newTreeParser = prepareTreeParser(revCommit);

    return git
      .diff()
      .setOldTree(oldTreeParser)
      .setNewTree(newTreeParser)
      .call();
  }

  private AbstractTreeIterator prepareTreeParser(RevCommit commit) throws IOException {
    // from the commit we can build the tree which allows us to construct the TreeParser
    RevTree tree = commit.getTree();
    CanonicalTreeParser treeParser = new CanonicalTreeParser();

    ObjectReader objReader = git.getRepository().newObjectReader();
    treeParser.reset(objReader, tree.getId());

    return treeParser;
  }

  private List<CommitDiffEntry> getCommitDiffEntries() throws RevisionSyntaxException,
    IOException, GitAPIException {

    List<CommitDiffEntry> commitDiffEntries = new ArrayList<>();

    for (RevCommit commit : getAllCommits()) {
      commitDiffEntries.addAll(getDiffEntriesForEachCommit(commit));
    }

    return commitDiffEntries;
  }

  /**
   * Return the changed details of the DiffEntry in a Commit.
   *
   * @param entry A DiffEnty.
   * @return changedDetails
   * The changed details of the DiffEntry.
   * @throws IOException
   */
  private List<String> getFormattedDiff(DiffEntry entry) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    DiffFormatter formatter = new DiffFormatter(baos);
    formatter.setRepository(git.getRepository());
    formatter.setContext(1); // 0: without context, 1: with context
    formatter.format(entry); // org.eclipse.jgit.errors.MissingObjectException TODO

    return new BufferedReader(new StringReader(baos.toString())).lines().toList();
  }

  /**
   * Return the changed file content of a commit.
   *
   * @param commit
   * @param path   the new path in the DiffEntry of the commit.
   * @return changed file content.
   * @throws IOException
   * @throws MissingObjectException
   */
  public String getFileContent(RevCommit commit, String path) throws MissingObjectException, IOException {
    String content = null;

    @SuppressWarnings("resource")
    TreeWalk treeWalk = new TreeWalk(git.getRepository());
    RevTree tree = commit.getTree();

    treeWalk.addTree(tree);  // org.eclipse.jgit.errors.MissingObjectException TODO
    treeWalk.setRecursive(true);
    treeWalk.setFilter(PathFilter.create(path));

    if (!treeWalk.next()) {
      // logger.error("Can't find file: $path in " + commit.getName)
    } else {
      String resultingPath = treeWalk.getPathString();
      if (!path.equals(resultingPath)) {
        // logger.info("Resulting path is different from requested one: " +
        // resultingPath)
      } else {
        ObjectId objectID = treeWalk.getObjectId(0);
        ObjectLoader loader = git.getRepository().open(objectID);

        content = new String(loader.getBytes());
      }
    }

    return content;
  }

  public void createFilesForGumTree(boolean ignoreTestCases) throws IOException, GitAPIException {

    for (CommitDiffEntry commitDiffEntry : getCommitDiffEntries()) {
      if (commitDiffEntry.isJavaFile() && commitDiffEntry.isModifyType()) {
        RevCommit commit = commitDiffEntry.getCommit();
        if (commit.getParentCount() > 1) {
          continue;
        }

        DiffEntry diffentry = commitDiffEntry.getDiffentry();
        String fileName = diffentry.getNewPath().replaceAll("/", "#");
        if (fileName.toLowerCase(Locale.ENGLISH).contains("test") && ignoreTestCases) {
          continue;
        }

        RevCommit parentCommit = commitDiffEntry.getParentCommit();
        String previousFileContent = getFileContent(parentCommit, diffentry.getOldPath());

        String revisedFileContent = getFileContent(commit, diffentry.getNewPath());
        if (StringUtils.isNotEmpty(revisedFileContent) && StringUtils.isNotEmpty(previousFileContent)) {
          String createFileName = createFileName(fileName, commitDiffEntry);
          Path revisedPath = Path.of(this.revisedFilesPath.toString(), createFileName);
          FileHelper.createFile(revisedPath.toFile(), revisedFileContent);

          Path previousPath = Path.of(this.previousFilesPath.toString(), "prev_" + createFileName);
          FileHelper.createFile(previousPath.toFile(), previousFileContent);

          String diffEntryChangedDetails = getDiffEntryChangedDetails(commitDiffEntry);
          Path diffEntryFilePath = Path.of(this.difffEntriesPath.toString(), createFileName.replace(".java", ".txt"));
          FileHelper.outputToFile(diffEntryFilePath.toString(), diffEntryChangedDetails, false);
        }
      }
    }
  }

  private String getDiffEntryChangedDetails(CommitDiffEntry commitDiffEntry) throws IOException {
    StringBuilder result = new StringBuilder();

    RevCommit parentCommit = commitDiffEntry.getParentCommit();
    result.append(parentCommit.getId().name().substring(0, 6)).append("\n");

    boolean isDiff = false;
    DiffEntry diffentry = commitDiffEntry.getDiffentry();
    for(String line : getFormattedDiff(diffentry)) {
      if (!isDiff) {
        if (LineDiffFilter.filterSignal(line)) {
          isDiff = true;
          result.append(line).append("\n");
        }
      } else {
        result.append(line).append("\n");
      }
    }
    return result.toString();
  }

  private String createFileName(String fileName, CommitDiffEntry commitDiffEntry) {
    RevCommit parentCommit = commitDiffEntry.getParentCommit();
    String parentCommitId = parentCommit.getId().name().substring(0, 6);
    String commitId = commitDiffEntry.getCommit().getId().name().substring(0, 6);
    String result = commitId + "_" + parentCommitId + "_" + fileName;

    if (result.length() > 200) {
      if (!tooLongFileNames.containsKey(result)) {
        int size = tooLongFileNames.size() + 1;
        String newFileName = commitId + "_" + parentCommitId + "_" + String.format("%05d", size) + ".java";
        tooLongFileNames.put(result, newFileName);
      }

      return tooLongFileNames.get(result);
    }
    return result;
  }
}
