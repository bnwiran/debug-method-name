package edu.lu.uni.serval.dlMethods.DataPrepare;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RenamedMethodSelector {
  private final String renamedMethodsPath;

  // Method info of renamed methods.
  private final List<String> methodInfoOfRenamedMethods = new ArrayList<>();
  // Method body token vectors of renamed methods.
  private final List<String> tokenVectorsOfRenamedMethods = new ArrayList<>();
  // parsed old method names @ parsed new method names: of selected renamed methods
  private final List<String> parsedRenamedMethodNames = new ArrayList<>();
  private final List<Integer> indexesOfSelectedMethods = new ArrayList<>();

  public RenamedMethodSelector(String renamedMethodsPath) {
    this.renamedMethodsPath = renamedMethodsPath;
  }

  public List<String> getMethodInfoOfRenamedMethods() {
    return methodInfoOfRenamedMethods;
  }

  public List<String> getParsedRenamedMethodNames() {
    return parsedRenamedMethodNames;
  }

  public List<String> getTokenVectorsOfRenamedMethods() {
    return tokenVectorsOfRenamedMethods;
  }

  public List<Integer> getIndexesOfSelectedMethods() {
    return indexesOfSelectedMethods;
  }

  /**
   * Select renamed methods by their first tokens and sizes.
   *
   * @throws IOException
   */
  public void selectRenamedMethods(int minSize, int maxSize) throws IOException {
    List<Integer> sizesOfRenamesMethodTokenVectors = readSizes(Paths.get(renamedMethodsPath, "MethodTokensSizes.csv"));
    List<String> parsedOldMethodNames = Files.readAllLines(Paths.get(renamedMethodsPath, "ParsedOldNames.txt"));
    List<String> parsedNewMethodNames = Files.readAllLines(Paths.get(renamedMethodsPath, "ParsedNewNames.txt"));

    List<String> methodTokens = Files.readAllLines(Paths.get(renamedMethodsPath, "MethodTokens.txt"));
    for(int i = 0; i < methodTokens.size(); i++) {
      String line = methodTokens.get(i);

      int sizeOfTokenVector = sizesOfRenamesMethodTokenVectors.get(i);
      if (minSize < sizeOfTokenVector && sizeOfTokenVector <= maxSize) {
        String[] elements = line.split(":");
        String tokens = elements[10];

        if ("Block Block".equals(tokens)) {
          continue;
        }

        indexesOfSelectedMethods.add(i);
        methodInfoOfRenamedMethods.add(getMethodInfo(elements));
        tokenVectorsOfRenamedMethods.add(tokens);
        parsedRenamedMethodNames.add(parsedOldMethodNames.get(i) + "@" + parsedNewMethodNames.get(i));
      }
    }
  }

  private static String getMethodInfo(String[] elements) {
    String filePath = elements[3];
    filePath = filePath.substring(0, filePath.lastIndexOf(".java"));
    int indexOfLastDot = filePath.lastIndexOf(".");
    String packageName = filePath.substring(0, indexOfLastDot);
    String className = filePath.substring(indexOfLastDot + 1);
    // ProjectName : packageName : className : methodName : arguments : returnType.
    String methodName = elements[5];
    methodName = methodName.substring(0, methodName.indexOf("@"));
    return elements[0] + ":" + packageName + ":" + className + ":" + methodName + ":" + elements[9] + ":" + elements[8];
  }

  private List<Integer> readSizes(Path sizesFilePath) throws IOException {
    return Files.readAllLines(sizesFilePath).stream().map(Integer::new).collect(Collectors.toList());
  }
}
