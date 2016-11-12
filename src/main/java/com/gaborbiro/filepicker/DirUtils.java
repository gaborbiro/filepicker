package com.gaborbiro.filepicker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class DirUtils {

    public static List<FileInfo> getDirInfo(String dirPath, String[] formatFilters)
            throws DirReadException {
        File directory = new File(dirPath);
        File[] files = directory.listFiles();

        if (files == null) {
            throw new DirReadException("Error listing files for " + dirPath);
        }

        List<FileInfo> result = new ArrayList<>();

        TreeMap<String, String> dirsMap = new TreeMap<>();
        TreeMap<String, String> filesMap = new TreeMap<>();

        for (File file : files) {
            if (file.isDirectory()) {
                dirsMap.put(file.getName(), file.getName());
            } else {
                final String fileName = file.getName();
                final String fileNameLwr = fileName.toLowerCase();

                if (formatFilters != null) {
                    boolean contains = false;

                    for (String aFormatFilter : formatFilters) {
                        final String formatLwr = aFormatFilter.toLowerCase();
                        if (fileNameLwr.endsWith(formatLwr)) {
                            contains = true;
                            break;
                        }
                    }
                    if (contains) {
                        filesMap.put(fileName, fileName);
                    }
                } else {
                    filesMap.put(fileName, fileName);
                }
            }
        }
        for (String dir : dirsMap.tailMap("")
                .values()) {
            result.add(new FileInfo(dir, true));
        }

        for (String file : filesMap.tailMap("")
                .values()) {
            result.add(new FileInfo(file, false));
        }
        return result;
    }

    public static class FileInfo {
        public String path;
        public boolean isFolder;

        public FileInfo(String path, boolean isFolder) {
            this.path = path;
            this.isFolder = isFolder;
        }
    }
}
