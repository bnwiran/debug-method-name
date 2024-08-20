package edu.lu.uni.serval.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class FileHelper {

  /**
   * @param filePath
   */
  public static void createDirectory(Path filePath) {
    File file = filePath.toFile();
    if (file.exists()) {
      deleteFile(file.getAbsolutePath());
    }
    file.mkdirs();
  }

  public static void createFile(File file, String content) throws IOException {
    Files.writeString(file.toPath(), content, StandardOpenOption.CREATE);
  }

  public static void deleteFile(Path path) {
    File directory = path.toFile();

    if (directory.exists()) {
      try (Stream<Path> pathStream = Files.walk(directory.toPath())) {
        pathStream
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static void deleteFile(String file) {
    File directory = new File(file);

    if (directory.exists()) {
      try (Stream<Path> pathStream = Files.walk(directory.toPath())) {
        pathStream
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * List all files in the directory.
   *
   * @param filePath
   * @param type
   * @return
   */
  public static List<File> getAllFiles(String filePath, String type) {
    return listAllFiles(new File(filePath), type);
  }

  /**
   * Recursively list all files in file.
   *
   * @param directory
   * @return
   */
  private static List<File> listAllFiles(File directory, String type) {
    if (directory.exists()) {
      try (Stream<Path> pathStream = Files.walk(directory.toPath())) {
        return pathStream
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .filter(file -> file.getName().endsWith(type))
          .toList();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return List.of();
  }

  /**
   * Read the content of a file.
   *
   * @param path
   * @return String, the content of a file.
   */
  public static String readFile(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "";
  }

  /**
   * Read the content of a file.
   *
   * @param file
   * @return String, the content of a file.
   */
  public static String readFile(File file) {
    try {
      return Files.readString(file.toPath());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "";
  }

  /**
   * Output output into a file.
   *
   * @param fileName, output file name.
   * @param data,     output data.
   * @param append,   the output data will be appended previous data in the file or not.
   */
  public static void outputToFile(String fileName, StringBuilder data, boolean append) {
    outputToFile(fileName, data.toString(), append);
  }

  public static void outputToFile(Path path, StringBuilder data, boolean append) {
    outputToFile(path.toFile(), data.toString(), append);
  }

  public static void outputToFile(Path path, String data, boolean append) {
    outputToFile(path.toFile(), data, append);
  }

  public static void outputToFile(String fileName, String data, boolean append) {
    outputToFile(new File(fileName), data, append);
  }

  public static void outputToFile(File file, String data, boolean append) {
    file.getParentFile().mkdirs();
    try {
      if (append) {
        Files.writeString(file.toPath(), data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } else {
        Files.writeString(file.toPath(), data, StandardOpenOption.CREATE);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static String getFileNameWithoutExtension(File file) {
    if (file.exists()) {
      String fileName = file.getName();
      fileName = fileName.substring(0, fileName.lastIndexOf("."));
      return fileName;
    } else {
      return null;
    }
  }

  public static void makeDirectory(String fileName) {
    makeDirectory(Path.of(fileName));
  }

  public static void makeDirectory(Path path) {
    deleteFile(path);
    File file = path.toFile().getParentFile();
    if (!file.exists()) {
      file.mkdirs();
    }
  }
}
