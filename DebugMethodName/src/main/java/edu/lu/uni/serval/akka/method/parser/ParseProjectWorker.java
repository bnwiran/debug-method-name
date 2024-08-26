package edu.lu.uni.serval.akka.method.parser;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import edu.lu.uni.serval.jdt.method.Method;
import edu.lu.uni.serval.jdt.parser.JavaFileParser;
import edu.lu.uni.serval.method.parser.MethodParser;
import edu.lu.uni.serval.method.parser.util.MethodExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ParseProjectWorker extends UntypedActor {
	
	private static final Logger log = LoggerFactory.getLogger(ParseProjectWorker.class);
	
	public ParseProjectWorker() {
	}

	public static Props props() {
		return Props.create(new Creator<ParseProjectWorker>() {

			@Serial
			private static final long serialVersionUID = -7615153844097275009L;

			@Override
			public ParseProjectWorker create() {
				return new ParseProjectWorker();
			}
			
		});
	}

	@Override
	public void onReceive(Object message) {
		if (message instanceof ProjectsMessage pro) {
      List<String> projects = pro.getProjects();
			String outputPath = pro.getOutputPath();
			int workerId = pro.getWorkerID();
			
			int numberOfMethods = 0;
			int numberOfNonNullMethods = 0;
			MethodParser parser = new MethodParser();
			
			if (Objects.nonNull(projects)) {
				for (String project : projects) {
					List<Method> methods = parser.parseMethods(project);
					if (Objects.nonNull(methods)) {
						int size = methods.size(); 
						if (size > 0) {
							numberOfMethods += size;
							int nonNullMethods = exportParsedMethods(methods, outputPath, workerId);
							numberOfNonNullMethods += nonNullMethods;
							log.debug("Worker #" + workerId +" Finish of parsing " + size + " methods " + nonNullMethods + " in project " + project + "...");
							methods.clear();
						}
					}
				}
			} else {
				List<File> javaFiles = pro.getJavaFiles();

				String projectPath = pro.getProject();
				List<Method> allMethods = new ArrayList<>();
				String projectName = projectPath.substring(projectPath.lastIndexOf("/") + 1);
				
				if (Objects.nonNull(javaFiles)) {
					for (File javaFile : javaFiles) {
						JavaFileParser jfp = new JavaFileParser();
						jfp.parseJavaFile(projectName, javaFile);
						List<Method> methods= jfp.getMethods();
						if (methods.isEmpty()) {
							continue;
						}
						allMethods.addAll(methods);
						int size = allMethods.size();
						if (size >= 500) {
							numberOfMethods += size;
							int nonNullMethods = exportParsedMethods(allMethods, outputPath, workerId);
							numberOfNonNullMethods += nonNullMethods;
							allMethods.clear();
						}
					}
				} else {
					List<String> javaFilePaths = pro.getJavaFilePathes();
					for (String javaFilePath : javaFilePaths) {
						JavaFileParser jfp = new JavaFileParser();
						jfp.parseJavaFile(projectName, new File(javaFilePath));
						List<Method> methods= jfp.getMethods();
						if (methods.isEmpty()) {
							continue;
						}
						allMethods.addAll(methods);
						int size = allMethods.size();
						if (size >= 500) {
							numberOfMethods += size;
							int nonNullMethods = exportParsedMethods(allMethods, outputPath, workerId);
							numberOfNonNullMethods += nonNullMethods;
							allMethods.clear();
						}
					}
				}

				int size = allMethods.size();
				numberOfMethods += size;
				int nonNullMethods = exportParsedMethods(allMethods, outputPath, workerId);
				numberOfNonNullMethods += nonNullMethods;
				allMethods.clear();
			}
			
			log.debug("Worker #" + workerId +" Finish of parsing " + numberOfMethods + " methods");
			
			this.getSender().tell("SHUT_DOWN:" + numberOfMethods + ":" + numberOfNonNullMethods, getSelf());
		} else {
			unhandled(message);
		}
	}
	
	/**
	 * Export parsed methods.
	 * 
	 * @param parsedMethods
	 * @param outputPath
	 * @return
	 */
	private int exportParsedMethods(List<Method> parsedMethods, String outputPath, int workerId) {
		MethodExporter exporter = new MethodExporter(outputPath);
    return exporter.outputMethods(parsedMethods, workerId);
	}
}
