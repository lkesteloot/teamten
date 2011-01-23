// Copyright 2011 Lawrence Kesteloot

package com.teamten.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.io.File;
import java.io.IOException;

/**
 * Builds a Java project.
 */
public class Builder {
    private final List<String> mSourceDirectoryList = new ArrayList<String>();
    private String mClassesDirectory;

    public Builder addSourceDirectory(String sourceDirectory) {
        mSourceDirectoryList.add(sourceDirectory);

        return this;
    }

    public Builder setClassesDirectory(String classesDirectory) {
        mClassesDirectory = classesDirectory;

        return this;
    }

    public void build() throws IOException {
        // Find all Java files in all source trees.
        List<SourceFile> sourceFileList = new ArrayList<SourceFile>();

        for (String sourceDirectory : mSourceDirectoryList) {
            File dir = new File(sourceDirectory);
            addSourceFiles(sourceFileList, dir, dir);
        }

        // Remove up-to-date files.
        Iterator<SourceFile> itr = sourceFileList.iterator();
        while (itr.hasNext()) {
            SourceFile sourceFile = itr.next();

            File classFile = sourceFile.getClassFile(mClassesDirectory);

            if (classFile.lastModified() >= sourceFile.lastModified()) {
                itr.remove();
            }
        }

        // Build javac command line.
        // XXX
    }

    /**
     * Add all Java files in the "dir" tree to the list.
     *
     * @param topDir the top-level directory at the top of the source tree.
     */
    private void addSourceFiles(List<SourceFile> sourceFileList, File dir,
            File topDir) throws IOException {

        for (File file : dir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".java")) {
                sourceFileList.add(new SourceFile(file, topDir));
            } else if (file.isDirectory()) {
                addSourceFiles(sourceFileList, file, topDir);
            }
        }
    }
}
