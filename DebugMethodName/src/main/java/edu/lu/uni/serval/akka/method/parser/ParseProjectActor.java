package edu.lu.uni.serval.akka.method.parser;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import akka.routing.RoundRobinPool;
import edu.lu.uni.serval.utils.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ParseProjectActor extends UntypedActor {
	
	private static final Logger logger = LoggerFactory.getLogger(ParseProjectActor.class);

	private final ActorRef travelRouter;
	private int numberOfWorkers;
	private int counter = 0;

  private int totalityOfMethods = 0;
	private int totalityOfNonNullMethods = 0;
	private String outputPath;
	
	public ParseProjectActor(int numberOfWorkers) {
		this.numberOfWorkers = numberOfWorkers;
		this.travelRouter = this.getContext().actorOf(new RoundRobinPool(numberOfWorkers)
				.props(ParseProjectWorker.props()), "parse-method-router");
	}

	public static Props props(int numberOfWorkers) {
		
		return Props.create(new Creator<ParseProjectActor>() {

			@Serial
			private static final long serialVersionUID = 9207427376110704705L;

			@Override
			public ParseProjectActor create() {
				return new ParseProjectActor(numberOfWorkers);
			}
			
		});
	}

	@Override
	public void onReceive(Object message) {
		if (message instanceof ProjectsMessage) {
			logger.info("****************Start to parse methods****************\n");
			ProjectsMessage projMsg = (ProjectsMessage) message;
			List<String> projectList = projMsg.getProjects();
			String project = projMsg.getProject();
			outputPath = projMsg.getOutputPath();
			
			if (project == null) {
				// Parse methods project by project.
				int size = projectList.size();
				int average = size / numberOfWorkers;
				
				for (int i = 0; i < numberOfWorkers; i ++) {
					int fromIndex = i * average;
					int toIndex = (i + 1) * average;
					if (i == numberOfWorkers - 1) {
						toIndex = size;
					}
					
					List<String> projectsOfWorkers = new ArrayList<>();
					projectsOfWorkers.addAll(projectList.subList(fromIndex, toIndex));
					final ProjectsMessage pro = new ProjectsMessage(projectsOfWorkers, i + 1, outputPath);
					travelRouter.tell(pro, getSelf());
					logger.debug("Assign a task to worker #" + (i + 1) + "...");
				}
			} else {
				if (project.endsWith(".txt")) {
					// Parse methods Java-code-file by Java-code-file.
					List<String> javaFiles;
					String content = FileHelper.readFile(new File(project));
					javaFiles = Arrays.asList(content.trim().split("\n"));
					int size = javaFiles.size();
					if (size < numberOfWorkers) {
						numberOfWorkers = size;
					}
					System.out.println("Java Files: " + size);
					int average = size / numberOfWorkers;
					int remainder = size % numberOfWorkers;
					int index = 0;
					
					for (int i = 0; i < numberOfWorkers; i ++) {
						int fromIndex = i * average + index;
						if (index < remainder) index ++;
						int toIndex = (i + 1) * average + index;
						if (i == numberOfWorkers - 1) {
							toIndex = size;
						}
						
						List<String> javaFilesOfWorkers = new ArrayList<>();
						javaFilesOfWorkers.addAll(javaFiles.subList(fromIndex, toIndex));
						ProjectsMessage pro = new ProjectsMessage(project, i + 1, outputPath);
						pro.setJavaFilePathes(javaFilesOfWorkers);
						travelRouter.tell(pro, getSelf());
						logger.debug("Assign a task to worker #" + (i + 1) + "...");
					}
				} else {
					// Parse methods in a project.
					List<File> javaFiles = getAllFiles(new File(project), ".java", project.length() + 1);
					int size = javaFiles.size();
					if (size < numberOfWorkers) {
						numberOfWorkers = size;
					}
					
					int average = size / numberOfWorkers;
					int remainder = size % numberOfWorkers;
					int index = 0;
					
					for (int i = 0; i < numberOfWorkers; i ++) {
						int fromIndex = i * average + index;
						if (index < remainder) index ++;
						int toIndex = (i + 1) * average + index;
						if (i == numberOfWorkers - 1) {
							toIndex = size;
						}

            List<File> javaFilesOfWorkers = new ArrayList<>(javaFiles.subList(fromIndex, toIndex));
						ProjectsMessage pro = new ProjectsMessage(project, i + 1, outputPath);
						pro.setJavaFiles(javaFilesOfWorkers);
						travelRouter.tell(pro, getSelf());
						logger.debug("Assign a task to worker #" + (i + 1) + "...");
					}
				}
			}
		} else if (message instanceof String && message.toString().startsWith("SHUT_DOWN")) {
			String str = message.toString();
			String[] elements = str.split(":");
			totalityOfMethods += Integer.parseInt(elements[1]);
			totalityOfNonNullMethods += Integer.parseInt(elements[2]);
			
			counter ++;
      logger.debug("{} workers finished their work...", counter);
			if (counter >= numberOfWorkers) {

				mergeData();
				logger.debug("All workers finished their work...");
				
				this.getContext().stop(travelRouter);
				this.getContext().stop(getSelf());
				this.getContext().system().terminate();

        logger.info("****************Totality of all methods: {}", totalityOfMethods);
        logger.info("****************Totality of all non-empty methods: {}", totalityOfNonNullMethods);
				logger.info("****************Finish off parsing methods****************\n");
			}
		} else {
			unhandled(message);
		}
	}
	
	private List<File> getAllFiles(File file, String type, int length) {
		if (!file.exists()) {
			return List.of();
		}
		
		File[] files = file.listFiles();
		List<File> fileList = new ArrayList<>();

		if (Objects.nonNull(files)) {
			for (File f : files) {
				if (f.isFile()) { // Filter out test, sample, example, and template Java code files.
					if (f.getName().endsWith(type)) {
						String filePath = f.getPath();
						filePath = filePath.substring(length).toLowerCase(Locale.ROOT);
						if (filePath.contains("test") || filePath.contains("sample") || filePath.contains("example") || filePath.contains("template")) {
							continue;
						}
						fileList.add(f);
					}
				} else {
					List<File> fl = getAllFiles(f, type, length);
					if (Objects.nonNull(fl) && !fl.isEmpty()) {
						fileList.addAll(fl);
					}
				}
			}
		}
		
		return fileList;
	}

	public void mergeData() {
		mergeData("tokens", ".txt");
		mergeData("sizes", ".csv");
		mergeData("method_bodies", ".txt");
		mergeData("ParsedMethodNames", ".txt");
	}

	private void mergeData(String type, String fileType) {
		Path outputFileName = Path.of(outputPath, type + fileType);
		FileHelper.deleteFile(outputFileName);
		
		for (int i = 1; i <= numberOfWorkers; i ++) {
			File file = Path.of(outputPath + type,type + "_" + i + fileType).toFile();
			if (file.exists()) {
				FileHelper.outputToFile(outputFileName, FileHelper.readFile(Path.of(outputPath + type, type + "_" + i + fileType).toFile()), true);
				file.delete();
			}
		}
		FileHelper.deleteFile(outputPath + type);
	}

}
