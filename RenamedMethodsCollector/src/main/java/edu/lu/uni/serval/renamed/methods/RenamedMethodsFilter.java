package edu.lu.uni.serval.renamed.methods;

import edu.lu.uni.serval.utils.FileHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Parse method names: valid-method-ids-cluster.
 * Filter out correcting method naming typos.
 *
 * @author kui.liu
 */
public class RenamedMethodsFilter {

  public static void filterOutTyposByParsedMethodNames(String dataPath) throws IOException {
    List<Integer> renameIndexes = getRenameIndexes(dataPath);
    writeRenamedMethods(dataPath, renameIndexes);
    writeTypoMethods(dataPath, renameIndexes);
    writeMethodBodies(dataPath, renameIndexes);
  }

  private static List<Integer> getRenameIndexes(String dataPath) throws IOException {
    List<String> parsedOldMethodNames = Files.readAllLines(Path.of(dataPath, "OldMethodNames.txt"));
    List<String> parsedNewMethodNames = Files.readAllLines(Path.of(dataPath, "NewMethodNames.txt"));
    StringBuilder typos = new StringBuilder("index@ParsedOldName@ParsedNewName\n");
    StringBuilder renames = new StringBuilder("index@oldToken@newToken\n");
    StringBuilder oldNames = new StringBuilder();
    StringBuilder newNames = new StringBuilder();
    List<Integer> renameIndexes = new ArrayList<>();
    for (int i = 0, size = parsedOldMethodNames.size(); i < size; i++) {
      String oldMethodName = parsedOldMethodNames.get(i);
      oldMethodName = oldMethodName.substring(oldMethodName.indexOf("@") + 1);
      List<String> oldMethodNameTokens = Arrays.asList(oldMethodName.split(","));
      String newMethodName = parsedNewMethodNames.get(i);
      newMethodName = newMethodName.substring(newMethodName.indexOf("@") + 1);
      List<String> newMethodNameTokens = Arrays.asList(newMethodName.split(","));

      if (oldMethodNameTokens.getFirst().equals(newMethodNameTokens.getFirst())) {// typos: starts with the same token.
        typos.append(i).append("@").append(oldMethodName).append("@").append(newMethodName).append("\n");
      } else {
        if (oldMethodName.equals("main") || newMethodName.equals("main")) {
          typos.append(i).append("@").append(oldMethodName).append("@").append(newMethodName).append("\n");
          continue;
        }
        String firstOldChar = oldMethodName.substring(0, 1);
        String firstNewChar = newMethodName.substring(0, 1);
        if (!firstOldChar.toLowerCase().equals(firstOldChar) || !firstNewChar.toLowerCase().equals(firstNewChar)) {
          // Constructors. Maybe.
          typos.append(i).append("@").append(oldMethodName).append("@").append(newMethodName).append("\n");
          continue;
        }
        String oldToken = oldMethodNameTokens.getFirst();
        String newToken = newMethodNameTokens.getFirst();
        if (!oldToken.equalsIgnoreCase(newToken) && !newToken.startsWith(oldToken) && !oldToken.startsWith(newToken)) {
          renameIndexes.add(i);
          renames.append(i).append("@").append(oldToken).append("@").append(newToken).append("\n");
          oldNames.append(oldMethodName).append("\n");
          newNames.append(newMethodName).append("\n");
        } else {
          typos.append(i).append("@").append(oldMethodName).append("@").append(newMethodName).append("\n");
        }
      }
    }
    FileHelper.outputToFile(Path.of(dataPath, "Typo", "MethodNamePairs.txt"), typos, false);
    FileHelper.outputToFile(Path.of(dataPath, "ActualRenamed", "MethodNamesInfo.txt"), renames, false);
    FileHelper.outputToFile(Path.of(dataPath, "ActualRenamed", "ParsedOldNames.txt"), oldNames, false);
    FileHelper.outputToFile(Path.of(dataPath, "ActualRenamed", "ParsedNewNames.txt"), newNames, false);
    return renameIndexes;
  }

  private static void writeRenamedMethods(String dataPath, List<Integer> renameIndexes) throws IOException {
    StringBuilder methodsBuilder = new StringBuilder();
    StringBuilder methodTokensBuilder = new StringBuilder();
    File renamedMethodsFile = Path.of(dataPath, "RenamedMethods.txt").toFile();

    List<String> renamedMethodLines = Files.readAllLines(renamedMethodsFile.toPath());
    for (int i = 0; i < renamedMethodLines.size(); i++) {
      String line = renamedMethodLines.get(i);
      String[] elements = line.split(":");
      String[] tokens = elements[10].split(" ");

      if (elements.length != 11) {
        System.err.println(line);
      }

      if (renameIndexes.contains(i)) {
        methodsBuilder.append(line).append("\n");
        methodTokensBuilder.append(tokens.length).append("\n");
      }
    }

    FileHelper.outputToFile(Path.of(dataPath, "ActualRenamed", "MethodTokens.txt"), methodsBuilder, false);
    FileHelper.outputToFile(Path.of(dataPath, "ActualRenamed", "MethodTokensSizes.csv"), methodTokensBuilder, false);
  }

  private static void writeTypoMethods(String dataPath, List<Integer> renameIndexes) throws IOException {
    StringBuilder methodsBuilder = new StringBuilder();
    StringBuilder methodTokensBuilder = new StringBuilder();
    File renamedMethodsFile = Path.of(dataPath, "RenamedMethods.txt").toFile();

    List<String> renamedMethodLines = Files.readAllLines(renamedMethodsFile.toPath());
    for (int i = 0; i < renamedMethodLines.size(); i++) {
      String line = renamedMethodLines.get(i);
      String[] elements = line.split(":");
      String[] tokens = elements[10].split(" ");

      if (elements.length != 11) {
        System.err.println(line);
      }

      if (!renameIndexes.contains(i)) {
        methodsBuilder.append(line).append("\n");
        methodTokensBuilder.append(tokens.length).append("\n");
      }
    }

    FileHelper.outputToFile(Path.of(dataPath, "Typo", "MethodTokens.txt"), methodsBuilder, false);
    FileHelper.outputToFile(Path.of(dataPath, "Typo", "MethodTokensSizes.csv"), methodTokensBuilder, false);
  }

  private static void writeMethodBodies(String dataPath, List<Integer> renameIndexes) throws IOException {
    List<String> methodBodyLines = Files.readAllLines(Path.of(dataPath, "MethodBodies.txt"));
    StringBuilder renamedMethodBodies = new StringBuilder();
    StringBuilder typoMethodBodies = new StringBuilder();
    StringBuilder singleMethod = new StringBuilder();

    int index = -1;
    for (String line : methodBodyLines) {
      if ("#METHOD_BODY#========================".equals(line)) {
        if (!singleMethod.isEmpty()) {
          if (renameIndexes.contains(index)) {
            renamedMethodBodies.append(singleMethod);
          } else {
            typoMethodBodies.append(singleMethod);
          }
          singleMethod.setLength(0);
        }
        index++;
      }
      singleMethod.append(line).append("\n");
    }

    if (!singleMethod.isEmpty()) {
      if (renameIndexes.contains(index)) {
        renamedMethodBodies.append(singleMethod);
      } else {
        typoMethodBodies.append(singleMethod);
      }
      singleMethod.setLength(0);
    }

    FileHelper.outputToFile(Path.of(dataPath, "ActualRenamed", "MethodBodies.txt"), renamedMethodBodies, false);
    FileHelper.outputToFile(Path.of(dataPath, "Typo", "MethodBodies.txt"), typoMethodBodies, false);
  }
}
