package edu.lu.uni.serval.dlMethods;

import edu.lu.uni.Configuration;
import edu.lu.uni.serval.utils.FileHelper;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DataMerger {
  /**
   * Merge data.
   */
  public static void merge(List<String> projects) {
    Path methodTokensPath = Paths.get(Configuration.SELECTED_DATA_PATH, "SelectedMethodTokens.txt");
    Path methodInfoPath = Paths.get(Configuration.SELECTED_DATA_PATH, "SelectedMethodInfo.txt");
    Path renamedMethodTokensPath = Paths.get(Configuration.SELECTED_RENAMED_DATA_PATH, "MethodTokens.txt");
    Path renamedMethodInfoPath = Paths.get(Configuration.SELECTED_RENAMED_DATA_PATH, "MethodInfo.txt");
    Path renamedMethodNamesPath = Paths.get(Configuration.SELECTED_RENAMED_DATA_PATH, "ParsedMethodNames.txt");

    methodTokensPath.toFile().delete();
    methodInfoPath.toFile().delete();
    renamedMethodTokensPath.toFile().delete();
    renamedMethodInfoPath.toFile().delete();
    renamedMethodNamesPath.toFile().delete();

    for (String project : projects) {
      String dataPath1 = Configuration.SELECTED_DATA_PATH + project;
      String dataPath2 = Configuration.SELECTED_RENAMED_DATA_PATH + project;
      FileHelper.outputToFile(methodTokensPath, FileHelper.readFile(Paths.get(dataPath1, "SelectedMethodTokens.txt").toFile()), true);
      FileHelper.outputToFile(methodInfoPath, FileHelper.readFile(Paths.get(dataPath1, "SelectedMethodInfo.txt").toFile()), true);
      FileHelper.outputToFile(renamedMethodTokensPath, FileHelper.readFile(Paths.get(dataPath2, "MethodTokens.txt").toFile()), true);
      FileHelper.outputToFile(renamedMethodInfoPath, FileHelper.readFile(Paths.get(dataPath2, "MethodInfo.txt").toFile()), true);
      FileHelper.outputToFile(renamedMethodNamesPath, FileHelper.readFile(Paths.get(dataPath2, "ParsedMethodNames.txt").toFile()), true);
    }

    File[] files = Paths.get(Configuration.SELECTED_DATA_PATH, "TrainingData").toFile().listFiles();
    if (Objects.nonNull(files)) {
      Optional<File> optionalFile = Arrays.stream(files).filter(f -> f.getName().startsWith("Tokens_MaxSize=")).findFirst();
      optionalFile.ifPresent(file -> {
        try {
          FileUtils.copyFile(methodTokensPath.toFile(), file);
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
    }

  }

}
