package edu.lu.uni.serval.renamed.methods;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serial;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import edu.lu.uni.serval.utils.FileHelper;

public class ParseWorker extends UntypedActor {

	private static final Logger log = LoggerFactory.getLogger(ParseWorker.class);

	private final String rootPath;
	private final String projectName;
	
	public ParseWorker(String rootPath, String projectName) {
		this.rootPath = rootPath;
		this.projectName = projectName;
	}

	public static Props props(final String rootPath, final String projectName) {
		return Props.create(new Creator<ParseWorker>() {

			@Serial
			private static final long serialVersionUID = -2972414308929536455L;

			@Override
			public ParseWorker create() {
				return new ParseWorker(rootPath, projectName);
			}
		});
	}

	@Override
	public void onReceive(Object message) {
		if (message instanceof MessageFiles messageFiles) {
      List<File> revFiles = messageFiles.getRevFiles();
			int id = messageFiles.getId();

			StringBuilder builder = new StringBuilder();
			StringBuilder methodBodiesBuilder = new StringBuilder();
			int counter = 0;

			for (File revFile : revFiles) {
				File prevFile = getPrevFile(revFile);
				File diffentryFile = getDiffentryFile(revFile);

				if (notValidFileName(prevFile) || notAllFileExist(revFile, prevFile, diffentryFile)) {
					deleteAllFiles(revFile, prevFile, diffentryFile);
					continue;
				}

				CodeChangeParser parser = new CodeChangeParser();
				final ExecutorService executor = Executors.newSingleThreadExecutor();
				final Future<?> future = executor.submit(new RunnableParser(prevFile, revFile, diffentryFile, parser));

				try {
					future.get(Configuration.Timeout, TimeUnit.SECONDS);
					// project_name : old_commit_id : new_commit_id : file_path : old_method_name : new_method_name : old_line : new_line : return_type : arguments : tokens.
					List<String> renamedMethods = parser.getRenamedMethods();
					if (renamedMethods.isEmpty()) {
						deleteAllFiles(revFile, prevFile, diffentryFile);
					} else {
						String oldCommitId = readPreviousCommitId(diffentryFile);
						methodBodiesBuilder.append(parser.getMethodBodies());
						for (String renamedMethod : renamedMethods) {
							builder.append(projectName).append(":");
							builder.append(oldCommitId).append(":");
							builder.append(renamedMethod).append("\n");
							counter ++;
							if (counter % 100 == 0) {
								writeMethods(id, builder, methodBodiesBuilder);
							}
						}
					}
				} catch (TimeoutException e) {
					future.cancel(true);
					System.err.println("#Timeout: " + revFile.getName());
				} catch (InterruptedException e) {
					System.err.println("#TimeInterrupted: " + revFile.getName());
				} catch (ExecutionException e) {
					System.err.println("#TimeAborted: " + revFile.getPath());
					log.error(e.getMessage());
				} finally {
					executor.shutdownNow();
				}
			}

			writeMethods(id, builder, methodBodiesBuilder);
      log.debug("Worker #{} Finish of parsing {} renamed methods...", id, counter);

			getSender().tell("END", getSelf());

		} else {
			unhandled(message);
		}
	}

	private void writeMethods(int id, StringBuilder builder, StringBuilder methodBodiesBuilder) {
		FileHelper.outputToFile(Path.of(rootPath, "RenamedMethods", "RenamedMethods_" + id + ".txt"), builder.toString(), true);
		builder.setLength(0);
		FileHelper.outputToFile(Path.of(rootPath, "MethodBodies", "MethodBodies_" + id + ".txt"), methodBodiesBuilder.toString(), true);
		methodBodiesBuilder.setLength(0);
	}

	private boolean notAllFileExist(File... files) {
		return !Arrays.stream(files).allMatch(File::exists);
	}

	private void deleteAllFiles(File... files) {
		Arrays.stream(files).forEach(File::delete);
	}

	private boolean notValidFileName(File file) {
		String filePath = file.getName().toLowerCase(Locale.ROOT);
		return !filePath.endsWith(".java") || filePath.contains("test") || filePath.contains("example")
			|| filePath.contains("template") || filePath.contains("sample");
	}

	private File getPrevFile(File revFile) {
		return Path.of(revFile.getParentFile().getParent(), "prevFiles", "prev_" + revFile.getName()).toFile();
	}

	private File getDiffentryFile(File revFile) {
		return Path.of(revFile.getParentFile().getParent(), "DiffEntries", revFile.getName().replace(".java", ".txt")).toFile();
	}

	private String readPreviousCommitId(File diffentryFile) {
		try (BufferedReader reader = new BufferedReader(new FileReader(diffentryFile))) {
			String commitID = reader.readLine();
			if (commitID.length() != 6) {
				System.err.println("WRONG COMMIT ID: " + diffentryFile);
				return "";
			}
			return commitID;
		} catch (IOException e) {
			log.error(e.getMessage());
			return "";
		}
	}
}