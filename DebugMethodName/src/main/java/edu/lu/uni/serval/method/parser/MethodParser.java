package edu.lu.uni.serval.method.parser;

import edu.lu.uni.serval.jdt.method.Method;
import edu.lu.uni.serval.jdt.parser.JavaFileParser;
import edu.lu.uni.serval.utils.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parse methods of Java source code.
 * 
 * @author kui.liu
 *
 */
public class MethodParser {
	
	private final Logger logger = LoggerFactory.getLogger(MethodParser.class);
	
	public List<Method> parseMethods(String project) {
		List<File> javaFiles = getAllJavaFiles(project);
		
		List<Method> methods = new ArrayList<>();
		if (javaFiles.isEmpty()) {
			logger.error("*******************There is no .java file in the project:" + project);
		} else {
			for (File file : javaFiles) {
				methods.addAll(parseMethods(file, project));
			}
		}
		
		return methods;
	}

	private List<File> getAllJavaFiles(String project) {
		List<File> javaFiles = new ArrayList<>();
		
		if (project.endsWith(".git")) {
			javaFiles.addAll(FileHelper.getAllFiles(project.substring(0, project.lastIndexOf(".git")), ".java"));
		} else {
			javaFiles.addAll(FileHelper.getAllFiles(project, ".java"));
		}
		return javaFiles;
	}

	private List<Method> parseMethods(File file, String projectName) {
		if (isTestOrSampleJavaFile(file.getName().toLowerCase(Locale.ROOT))) return new ArrayList<>();
		
		String filePath = file.getPath();
		int index = filePath.indexOf(projectName);
		if (index < 0) {
			index = projectName.length();
		}
		else {
			index += projectName.length();
		}
		filePath = filePath.substring(index).toLowerCase(Locale.ROOT);
		if (isTestOrSampleJavaFile(filePath)) {
			return new ArrayList<>();
		}
		
		JavaFileParser jfp = new JavaFileParser();
		jfp.parseJavaFile(projectName, file);
		List<Method> methods= jfp.getMethods();
		if (methods != null && !methods.isEmpty()) {
			return methods;
		}
		return new ArrayList<>();
	}
	
	private boolean isTestOrSampleJavaFile(String filePath) {
    return filePath.contains("test") || filePath.contains("sample")
      || filePath.contains("example") || filePath.contains("template");
  }
}
