package zzk.townshipscheduler.backend.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ReportZipUtil {


    public static void zipReportDirectory(ZipOutputStream zos, File sourceDir, String zipRootName) throws IOException {
        if (sourceDir == null || !sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new IllegalArgumentException("invalid dir: " + (sourceDir == null ? "null" : sourceDir.getAbsolutePath()));
        }

        String baseEntryPath = (zipRootName == null || zipRootName.isBlank()) ? "" : zipRootName + "/";

        zipRecursive(zos, sourceDir, baseEntryPath);
    }

    private static void zipRecursive(ZipOutputStream zos, File currentFile, String currentEntryPath) throws IOException {
        File[] files = currentFile.listFiles();
        if (files == null) return;

        for (File file : files) {
            String entryName = currentEntryPath + file.getName();
            if (file.isDirectory()) {
                entryName += "/";
            }

            ZipEntry entry = new ZipEntry(entryName);
            entry.setTime(file.lastModified());
            zos.putNextEntry(entry);
            if (file.isFile()) {
                try (var fis = new FileInputStream(file)) {
                    fis.transferTo(zos);
                }
            }
            zos.closeEntry();

            if (file.isDirectory()) {
                zipRecursive(zos, file, entryName);
            }
        }
    }

}
