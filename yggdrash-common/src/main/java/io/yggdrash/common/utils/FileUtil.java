package io.yggdrash.common.utils;

import com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 * File Utility
 * extends org.apache.commons.io.FileUtils.
 */
public class FileUtil extends org.apache.commons.io.FileUtils {
    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * write file as byte[].
     *
     * @param filePath file path
     * @param fileName file name
     * @param data     data
     * @throws IOException IOException
     */
    public static void writeFile(String filePath, String fileName, byte[] data)
            throws IOException {

        if (Strings.isNullOrEmpty(filePath) || Strings.isNullOrEmpty(fileName)) {
            throw new IOException("Invalid filepath or filename");
        }

        File file = new File(filePath, fileName);

        FileUtils.writeByteArrayToFile(file, data);

    }

    /**
     * Write file as byte[].
     *
     * @param file file
     * @param data data
     * @throws IOException IOException
     */
    public static void writeFile(File file, byte[] data) throws IOException {
        FileUtils.writeByteArrayToFile(file, data);
    }

    /**
     * Read file as byte[].
     *
     * @param filePath file path
     * @param fileName file name
     * @return data
     * @throws IOException IOException
     */
    public static byte[] readFile(String filePath, String fileName) throws IOException {

        File file = FileUtils.getFile(filePath + "/" + fileName);

        return FileUtils.readFileToByteArray(file);
    }

    /**
     * Get file name.
     *
     * @param filePathName file path + name
     * @return file name
     */
    public static String getFileName(String filePathName) {

        String[] splitName = filePathName.split(File.separator);

        if (splitName.length > 0) {
            return splitName[splitName.length - 1];
        } else {
            return null;
        }
    }

    /**
     * Get file path.
     *
     * @param filePathName file path + name
     * @return file path
     */
    public static String getFilePath(String filePathName) {

        String[] splitName = filePathName.split(File.separator);

        StringBuilder result = new StringBuilder();
        if (splitName.length > 0) {

            for (int i = 0; i < splitName.length - 1; i++) {
                result.append(splitName[i]).append(File.separator);
            }

            return result.toString();
        } else {
            return null;
        }
    }

    /**
     * Is exists boolean.
     *
     * @param path the file or dir path
     * @return the boolean
     */
    public static boolean isExists(Path path) {
        File file = path.toFile();
        return file.exists();
    }

    public static void recursiveDelete(Path path) {
        File file = path.toFile();
        if (file.exists()) {
            if (file.isDirectory()) {
                Arrays.stream(Objects.requireNonNull(file.list()))
                        .map(path::resolve)
                        .forEachOrdered(FileUtil::recursiveDelete);
            }

            if (!file.setWritable(true)) {
                log.debug("{} writable fail", file);
            }

            try {
                Files.delete(path);
            } catch (IOException e) {
                log.debug(e.getMessage());
            }
        }
    }
}
