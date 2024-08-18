package edu.lu.uni.serval.renamed.methods;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import akka.routing.RoundRobinPool;
import edu.lu.uni.serval.utils.FileHelper;

public class ParseActor extends UntypedActor {
	private static Logger log = LoggerFactory.getLogger(ParseActor.class);
	
	private String inputProjectPath;
	private String outputPath;
	private String projectName;
	private ActorRef travelRouter;
	private int numberOfWorkers = 500;
	int number = 0;
	int startIndex = 0;
	int endIndex = 0;
	//lucene-solr
	public ParseActor(String inputProjectPath, String outputPath) {
		this.inputProjectPath = inputProjectPath;
		projectName = inputProjectPath.substring(inputProjectPath.lastIndexOf(File.separatorChar) + 1);
		this.outputPath = outputPath;
		
		travelRouter = this.getContext().actorOf(new RoundRobinPool(numberOfWorkers)
				.props(ParseWorker.props(this.outputPath, projectName)), "parse-router");
	}
	
	public ParseActor(String inputProjectPath, String outputPath, int startIndex, int endIndex) {
		this.inputProjectPath = inputProjectPath;
		projectName = inputProjectPath.substring(inputProjectPath.lastIndexOf(File.separatorChar) + 1);
		this.outputPath = outputPath;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		travelRouter = this.getContext().actorOf(new RoundRobinPool(numberOfWorkers)
				.props(ParseWorker.props(this.outputPath, projectName)), "parse-router");
	}

	public static Props props(final String rootPath, final String outputPath) {
		return Props.create(new Creator<ParseActor>() {

			private static final long serialVersionUID = -4156981079570315552L;

			@Override
			public ParseActor create() throws Exception {
				return new ParseActor(rootPath, outputPath);
			}
		});
	}

	public static Props props(final String rootPath, final String outputPath, final int startIndex, final int endIndex) {
		return Props.create(new Creator<ParseActor>() {

			private static final long serialVersionUID = -4156981079570315552L;

			@Override
			public ParseActor create() throws Exception {
				return new ParseActor(rootPath, outputPath, startIndex, endIndex);
			}
		});
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onReceive(Object msg) throws Throwable {
		if (msg instanceof String && "BEGIN".equals(msg.toString())) {
			FileHelper.deleteDirectory(outputPath);
			List<File> allRevFiles1 = Arrays.asList(Path.of(inputProjectPath, "revFiles").toFile().listFiles());
			if (allRevFiles1.isEmpty() || startIndex >= allRevFiles1.size()) {
				System.out.println(inputProjectPath);
				this.getContext().stop(travelRouter);
				this.getContext().stop(getSelf());
				this.getContext().system().shutdown();
			}
			if (endIndex == 0 || endIndex > allRevFiles1.size()) {
				endIndex = allRevFiles1.size();
			}
			System.out.println("Files: " + allRevFiles1.size());
			List<File> allRevFiles = allRevFiles1.subList(startIndex, endIndex);
			int size = allRevFiles.size();
			
			if (size < numberOfWorkers) {
				numberOfWorkers = size;
			}
			int average = size / numberOfWorkers;
			int remainder = size % numberOfWorkers;
			int index = 0;
			for (int i = 0; i < numberOfWorkers; i ++) {
				int beginIndex = i * average + index;
				if (index < remainder) index ++;
				int endIndex = (i + 1) * average + index;
				
				List<File> msgFilesOfWorker = new ArrayList<>();
				msgFilesOfWorker.addAll(allRevFiles.subList(beginIndex, endIndex));
				MessageFiles messageFiles = new MessageFiles(i + 1);
				messageFiles.setRevFiles(msgFilesOfWorker);
				travelRouter.tell(messageFiles, getSelf());
				log.debug("Assign a task to worker #" + (i + 1) + "...");
			}
		} else if (msg instanceof String && "END".equals(msg.toString())) {
			number ++;
			log.debug(number + " workers finished their work...");
			if (number == numberOfWorkers) {
				mergeData(); // Merge data.
				RenamedMethodsFilter.filteroutTyposByParsedMethodNames(outputPath);
				log.info("All workers finished their work...");
				this.getContext().stop(travelRouter);
				this.getContext().stop(getSelf());
				this.getContext().system().shutdown();
			}
		} else {
			unhandled(msg);
		}
	}

	private void mergeData() throws IOException {
		String methodsFile = outputPath + "/RenamedMethods.txt";
		FileHelper.deleteFile(methodsFile);

		String methodBodiesFile = outputPath + "/MethodBodies.txt";
		FileHelper.deleteFile(methodBodiesFile);

		StringBuilder sizes = new StringBuilder();
		StringBuilder methods = new StringBuilder();
		StringBuilder oldMethodNames = new StringBuilder();
		StringBuilder newMethodNames = new StringBuilder();
		int counter = 0;
		for (int i = 1; i <= numberOfWorkers; i ++) {
			File renamedMethodFile = Path.of(outputPath, "RenamedMethods", "RenamedMethods_" + i + ".txt").toFile();
			if (!renamedMethodFile.exists()) {
				continue;
			}
			FileInputStream fis = new FileInputStream(renamedMethodFile);
			Scanner scanner = new Scanner(fis);
			
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] elements = line.split(":");
				if (elements.length == 11) {
					String[] tokens = elements[10].split(" ");
					sizes.append(tokens.length).append("\n");
					methods.append(line).append("\n");
					oldMethodNames.append(elements[4]).append("\n");
					newMethodNames.append(elements[5]).append("\n");
					counter ++;
					if (counter % 2000 == 0) {
						FileHelper.outputToFile(methodsFile, methods, true);
						methods.setLength(0);
					}
				} else {
					System.err.println(line);
				}
			}
			
			scanner.close();
			fis.close();
			
			FileHelper.outputToFile(methodBodiesFile, Files.readString(Path.of(outputPath, "MethodBodies", "MethodBodies_" + i + ".txt")), true);
		}
		
		if (!methods.isEmpty()) {
			FileHelper.outputToFile(methodsFile, methods, true);
			methods.setLength(0);
		}

		String sizesFile = outputPath + "/Sizes.csv";
		FileHelper.outputToFile(sizesFile, sizes, false);
		sizes.setLength(0);

		String oldMethodNamesFile = outputPath + "/OldMethodNames.txt";
		FileHelper.outputToFile(oldMethodNamesFile, oldMethodNames, false);
		oldMethodNames.setLength(0);

		String newMethodNamesFile = outputPath + "/NewMethodNames.txt";
		FileHelper.outputToFile(newMethodNamesFile, newMethodNames, false);
		newMethodNames.setLength(0);

		String path = outputPath + "/RenamedMethods/";
		FileHelper.deleteDirectory(path);
		FileHelper.deleteDirectory(outputPath + "/MethodBodies/");
	}

}
