package edu.lu.uni.serval.dlMethods;

import edu.lu.uni.Configuration;
import edu.lu.uni.serval.dlMethods.DataPrepare.CommonFirstTokens;
import edu.lu.uni.serval.dlMethods.DataPrepare.RenamedMethodSelector;
import edu.lu.uni.serval.utils.Distribution;
import edu.lu.uni.serval.utils.Distribution.MaxSizeType;
import edu.lu.uni.serval.utils.FileHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Figure out the common first tokens of all method names.
 * Select the methods of which method names start with these common first tokens.
 *
 * @author kui.liu
 */
public class DataInitializer {

  public int QUANTITY = 1;// or 500, the number of methods of which names start with the same token.
  public int MIN_SIZE = 0;    // The minimum size of vectors.

  private final String inputPath;
  public String selectedDataPath;
  public String selectedRenamedDataPath;
  public String renamedMethodsPath;

  // Tokens of all parsed method names.
  private List<String> tokenVectorOfAllParsedMethodNames = new ArrayList<>();

  // Threshold of method token vectors sizes for selecting methods.
  private int maxSize = 0;

  // Method info of renamed methods.
  private List<String> methodInfoOfRenamedMethods = new ArrayList<>();
  // Method body token vectors of renamed methods.
  private List<String> tokenVectorsOfRenamedMethods = new ArrayList<>();
  // parsed old method names @ parsed new method names: of selected renamed methods
  private List<String> parsedRenamedMethodNames = new ArrayList<>();

  // Selected method info of all methods.
  private final List<String> selectedMethodInfoOfAllMethods = new ArrayList<>();

  private final List<Integer> selectMethodIndexes = new ArrayList<>();
  private List<Integer> selectedRenamedMethodIndexes = new ArrayList<>();
  private final List<Integer> furtherSelectedRenamedMethodIndexes = new ArrayList<>();

  public DataInitializer(String inputPath) {
    this.inputPath = inputPath;
  }

  public void initializeData(List<String> projects) throws IOException {
    // Common first tokens of all methods.
    CommonFirstTokens cft = new CommonFirstTokens(inputPath, selectedDataPath);
    tokenVectorOfAllParsedMethodNames = cft.processMethodNameTokens();

    File sizesFile = mergeSizeFiles(projects);

    // Get the threshold of sizes of method token vectors
    List<Integer> sizesList = readSizes(sizesFile);
    maxSize = Distribution.computeMaxSize(MaxSizeType.UpperWhisker, sizesList);
    if (maxSize % 2 != 0) {
      maxSize += 1;
    }

    // Select the renamed methods of which names start with one of the common first token.
    RenamedMethodSelector rms = new RenamedMethodSelector(renamedMethodsPath);

    rms.selectRenamedMethods(MIN_SIZE, maxSize);
    methodInfoOfRenamedMethods = rms.getMethodInfoOfRenamedMethods();
    tokenVectorsOfRenamedMethods = rms.getTokenVectorsOfRenamedMethods();
    parsedRenamedMethodNames = rms.getParsedRenamedMethodNames(); // oldName@newName
    selectedRenamedMethodIndexes = rms.getIndexesOfSelectedMethods();
  }

  private static File mergeSizeFiles(List<String> projects) {
    File sizesFile = Paths.get(Configuration.TOKENIZED_METHODS_PATH, "sizes.csv").toFile();
    if (!sizesFile.exists()) {
      for (String project : projects) {
        File sizeFile = Paths.get(Configuration.TOKENIZED_METHODS_PATH, project, "sizes.csv").toFile();
        if (!sizeFile.exists()) {
          continue;
        }
        FileHelper.outputToFile(sizesFile.toPath(), FileHelper.readFile(sizeFile), true);
      }
    }
    return sizesFile;
  }

  /**
   * Select methods to get the indexes for selecting methods.
   *
   * @throws IOException
   */
  public void selectMethod() throws IOException {
    // Sizes of all method token vectors.
    List<Integer> sizesListOfAllMethodBodyTokenVectors = readSizes(Paths.get(inputPath, "sizes.csv").toFile());

    StringBuilder tokensBuilder = new StringBuilder();
    StringBuilder methodInfoBuilder = new StringBuilder();
    StringBuilder sizesBuilder = new StringBuilder();
    StringBuilder tokensBuilderOfSelectedRenamedMethods = new StringBuilder();
    StringBuilder methodInfoBuilderOfSelectedRenamedMethods = new StringBuilder();
    StringBuilder selectedParseRenamedMethodNames = new StringBuilder();

    File tokensFile = Paths.get(inputPath, "tokens.txt").toFile();
    FileInputStream fis = new FileInputStream(tokensFile);
    Scanner scanner = new Scanner(fis);
    int index = -1;
    int counter = 0;
    int a = 0;
    int test = 0;

    while (scanner.hasNextLine()) {
      // projectName : packageName : ClassName : methodName : arguments: ReturnType#tokens.
      String lineStr = scanner.nextLine();
      index++;

      int sizeOfTokenVector = sizesListOfAllMethodBodyTokenVectors.get(index);

      int sharpPosition = lineStr.indexOf("#");
      String methodInfo = lineStr.substring(0, sharpPosition);
      String packageName = methodInfo.split(":")[1].toLowerCase(Locale.ROOT);
      if (packageName.contains("test") || packageName.contains("sample") || packageName.contains("example") || packageName.contains("template")) {
        test++;
        continue;
      }
      String tokens = lineStr.substring(sharpPosition + 1, lineStr.length() - 1).replace(", ", " ");
      int renamedIndex = this.methodInfoOfRenamedMethods.indexOf(methodInfo);
      if (renamedIndex >= 0) {
        a++;
      }

      if (MIN_SIZE < sizeOfTokenVector && sizeOfTokenVector <= maxSize) {
        if (renamedIndex >= 0) {
          // selected renamed methods.
          String renamedTokens = this.tokenVectorsOfRenamedMethods.get(renamedIndex);
          if (renamedTokens.equals(tokens)) {
            int index2 = this.selectedRenamedMethodIndexes.get(renamedIndex);
            if (!this.furtherSelectedRenamedMethodIndexes.contains(index2)) {
              this.furtherSelectedRenamedMethodIndexes.add(index2);
              selectedParseRenamedMethodNames.append(this.parsedRenamedMethodNames.get(renamedIndex)).append("\n");
              methodInfoBuilderOfSelectedRenamedMethods.append(methodInfo).append("\n");
              tokensBuilderOfSelectedRenamedMethods.append(renamedTokens).append("\n");
            }
            continue;
          }
        }

        if ("Block Block".equals(tokens)) {
          continue;
        }

        String parsedMethodName = this.tokenVectorOfAllParsedMethodNames.get(index);
        tokensBuilder.append(tokens).append("\n");
        String methodInfo1 = index + "@" + methodInfo + "@" + parsedMethodName;
        methodInfoBuilder.append(methodInfo1).append("\n");
        selectMethodIndexes.add(index);

        this.selectedMethodInfoOfAllMethods.add(methodInfo1);
        sizesBuilder.append(sizeOfTokenVector).append("\n");
        counter++;

        if (counter % 10000 == 0) {
          FileHelper.outputToFile(Paths.get(selectedDataPath, "SelectedMethodTokens.txt"), tokensBuilder.toString(), true);
          tokensBuilder.setLength(0);
          FileHelper.outputToFile(Paths.get(selectedDataPath, "SelectedMethodInfo.txt"), methodInfoBuilder.toString(), true);
          methodInfoBuilder.setLength(0);
        }
      }
    }
    scanner.close();
    fis.close();

    FileHelper.outputToFile(Paths.get(selectedDataPath, "SelectedSizes.csv"), sizesBuilder.toString(), false);
    FileHelper.outputToFile(Paths.get(selectedDataPath, "SelectedMethodTokens.txt"), tokensBuilder.toString(), true);
    tokensBuilder.setLength(0);
    FileHelper.outputToFile(Paths.get(selectedDataPath, "SelectedMethodInfo.txt"), methodInfoBuilder.toString(), true);
    methodInfoBuilder.setLength(0);

    File tokensFile_ = Paths.get(Configuration.SELECTED_DATA_PATH, "TrainingData", "Tokens_MaxSize=" + this.maxSize + ".txt").toFile();
    if (!tokensFile_.exists()) {
      FileHelper.outputToFile(tokensFile_.toPath(), "", false);
    }

    FileHelper.outputToFile(Paths.get(selectedRenamedDataPath, "MethodTokens.txt"), tokensBuilderOfSelectedRenamedMethods.toString(), false);
    FileHelper.outputToFile(Paths.get(selectedRenamedDataPath, "MethodInfo.txt"), methodInfoBuilderOfSelectedRenamedMethods.toString(), false);
    FileHelper.outputToFile(Paths.get(selectedRenamedDataPath, "ParsedMethodNames.txt"), selectedParseRenamedMethodNames.toString(), false);

    System.out.println("Number of further selected renamed methods:" + furtherSelectedRenamedMethodIndexes.size());
    System.out.println("Number of selected training methods:" + this.selectedMethodInfoOfAllMethods.size());
    System.out.println("Renamed methods: " + a);
    System.out.println("Test methods: " + test);

    exportMethodBodies();
  }

  private void exportMethodBodies() throws IOException {
    Path selectedMethodsPath = Paths.get(selectedDataPath, "method_bodies.txt");
    selectedMethodsPath.toFile().delete();

    File methodBodyFile = Paths.get(inputPath, "method_bodies.txt").toFile();
    FileInputStream fis = new FileInputStream(methodBodyFile);
    Scanner scanner = new Scanner(fis);
    StringBuilder singleMethod = new StringBuilder();
    StringBuilder selectedMethods = new StringBuilder();
    int index = -1;
    int counter = 0;
    boolean isMethodBody = false;
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      if ("#METHOD_BODY#========================".equals(line)) {
        if (isMethodBody) {
          if (this.selectMethodIndexes.contains(index)) {
            selectedMethods.append(singleMethod.toString().replace("@Override", "")).append("\n");
            if (counter % 2000 == 0) {
              FileHelper.outputToFile(selectedMethodsPath, selectedMethods.toString(), true);
              selectedMethods.setLength(0);
            }
            counter++;
          }
        }
        singleMethod.setLength(0);
        isMethodBody = false;
        index++;
      } else {
        if (isMethodBody) {
          singleMethod.append(line).append("\n");
        } else isMethodBody = true;
      }
      singleMethod.append(line).append("\n");
    }
    if (this.selectMethodIndexes.contains(index)) {
      selectedMethods.append(singleMethod.toString().replace("@Override", "")).append("\n");
    }
    scanner.close();
    fis.close();

    FileHelper.outputToFile(selectedMethodsPath, selectedMethods.toString(), true);
    selectedMethods.setLength(0);

    // Export the method bodies of further selected renamed methods.
    String renamedMethodBodyFile = this.renamedMethodsPath + "/MethodBodies.txt";
    fis = new FileInputStream(renamedMethodBodyFile);
    scanner = new Scanner(fis);
    index = -1;
    counter = 0;
    isMethodBody = false;
    singleMethod = new StringBuilder();
    StringBuilder testMethods = new StringBuilder();
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      if ("#METHOD_BODY#========================".equals(line)) {
        if (isMethodBody) {
          if (this.furtherSelectedRenamedMethodIndexes.contains(index)) {
            testMethods.append(singleMethod.toString().replace("@Override", "")).append("\n");
            counter++;
          }
        }
        singleMethod.setLength(0);
        isMethodBody = false;
        index++;
      } else {
        if (isMethodBody) {
          singleMethod.append(line).append("\n");
        } else isMethodBody = true;
      }
    }
    scanner.close();
    fis.close();
    if (this.furtherSelectedRenamedMethodIndexes.contains(index)) {
      counter++;
      testMethods.append(singleMethod.toString().replace("@Override", "")).append("\n");
    }
    FileHelper.outputToFile(Paths.get(selectedRenamedDataPath, "method_bodies", "TestingMethods.java"), "public class TestingMethods {\n" + testMethods + "}", false);
    System.out.println("Testing methods: " + counter);
  }

  private List<Integer> readSizes(File sizesFile) throws IOException {
    return Files.readAllLines(sizesFile.toPath()).stream().map(Integer::new).collect(Collectors.toList());
  }
}
