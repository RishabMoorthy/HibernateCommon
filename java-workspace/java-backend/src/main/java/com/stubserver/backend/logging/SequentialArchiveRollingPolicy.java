package com.stubserver.backend.logging;

import ch.qos.logback.core.rolling.RollingPolicyBase;
import ch.qos.logback.core.rolling.RolloverFailure;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class SequentialArchiveRollingPolicy extends RollingPolicyBase {

    private String fileNameBase;
    private int maxFiles = 50;
    private int deleteOlderAtIndex = 25;
    private String olderFolderName = "older_logs";

    @Override
    public void start() {
        if (fileNameBase == null || fileNameBase.isEmpty()) {
            addError("fileNameBase must be set for SequentialArchiveRollingPolicy");
            return;
        }
        super.start();
    }

    @Override
    public void rollover() throws RolloverFailure {
        String activeFile = getParentsRawFileProperty();
        File logDir = new File(activeFile).getAbsoluteFile().getParentFile();

        int nextIndex = findCurrentMaxIndex(logDir) + 1;

        if (nextIndex > maxFiles) {
            moveToOlderFolder(logDir);
            nextIndex = 1;
        }

        if (nextIndex == deleteOlderAtIndex) {
            deleteOlderFolder(logDir);
        }

        String target = fileNameBase + "_" + String.format("%02d", nextIndex) + ".log";
        try {
            Files.move(Path.of(activeFile), Path.of(target), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RolloverFailure("Failed to rollover log file: " + e.getMessage(), e);
        }
    }

    private int findCurrentMaxIndex(File logDir) {
        String baseName = new File(fileNameBase).getName();
        if (!logDir.exists()) return 0;
        File[] files = logDir.listFiles();
        if (files == null) return 0;
        int max = 0;
        for (File f : files) {
            String name = f.getName();
            if (name.startsWith(baseName + "_") && name.endsWith(".log")) {
                String idx = name.substring(baseName.length() + 1, name.length() - 4);
                try {
                    int i = Integer.parseInt(idx);
                    if (i > max) max = i;
                } catch (NumberFormatException ignored) {}
            }
        }
        return max;
    }

    private void moveToOlderFolder(File logDir) throws RolloverFailure {
        String baseName = new File(fileNameBase).getName();
        File olderDir = new File(logDir, olderFolderName);
        try {
            Files.createDirectories(olderDir.toPath());
            File[] files = logDir.listFiles();
            if (files == null) return;
            for (File f : files) {
                String name = f.getName();
                if (name.startsWith(baseName + "_") && name.endsWith(".log")) {
                    Files.move(f.toPath(), new File(olderDir, name).toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new RolloverFailure("Failed to archive logs: " + e.getMessage(), e);
        }
    }

    private void deleteOlderFolder(File logDir) {
        File olderDir = new File(logDir, olderFolderName);
        if (olderDir.exists()) {
            deleteRecursively(olderDir);
        }
    }

    private void deleteRecursively(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursively(child);
            }
        }
        f.delete();
    }

    @Override
    public String getActiveFileName() {
        return getParentsRawFileProperty();
    }

    public void setFileNameBase(String fileNameBase) { this.fileNameBase = fileNameBase; }
    public void setMaxFiles(int maxFiles) { this.maxFiles = maxFiles; }
    public void setDeleteOlderAtIndex(int deleteOlderAtIndex) { this.deleteOlderAtIndex = deleteOlderAtIndex; }
    public void setOlderFolderName(String olderFolderName) { this.olderFolderName = olderFolderName; }
}
