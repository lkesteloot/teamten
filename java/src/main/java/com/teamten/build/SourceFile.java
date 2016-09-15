// Copyright 2011 Lawrence Kesteloot

package com.teamten.build;

import java.io.File;

/**
 * Represents a source file in the build tree.
 */
public class SourceFile {
    private final File mFile;
    private final File mTopDir;

    public SourceFile(File file, File topDir) {
        mFile = file;
        mTopDir = topDir;
    }

    public long lastModified() {
        return mFile.lastModified();
    }

    public File getClassFile(String classesDirectory) {
        // Get the top path with separator (like "src/"). This is
        // the part we strip.
        String topPath = mTopDir.getPath() + File.separator;

        // Get the source path.
        String path = mFile.getPath();
        if (!path.startsWith(topPath)) {
            throw new IllegalArgumentException("\"" + path
                    + "\" must start with \"" + topPath + "\"");
        }

        // Strip top path.
        path = path.substring(topPath.length());

        // Change extension.
        if (!path.endsWith(".java")) {
            throw new IllegalArgumentException("\"" + mFile.getPath()
                    + "\" must end with \".java\"");
        }
        path = path.substring(0, path.length() - 5) + ".class";

        // Prepend classes directory.
        return new File(classesDirectory, path);
    }
}

