package com.teamten.typeset;

import java.io.File;

/**
 * Represents the individual fonts that we know about and where to find them.
 */
public enum FontName {
    TIMES_NEW_ROMAN(FontDirs.FONT_DIR, "Times New Roman.ttf"),
    TIMES_NEW_ROMAN_BOLD(FontDirs.FONT_DIR, "Times New Roman Bold.ttf"),
    TIMES_NEW_ROMAN_ITALIC(FontDirs.FONT_DIR, "Times New Roman Italic.ttf"),
    TIMES_NEW_ROMAN_BOLD_ITALIC(FontDirs.FONT_DIR, "Times New Roman Italic.ttf"),
    MINION(FontDirs.MY_FONT_DIR, "Minion/MinionPro-Regular.ttf"),
    MINION_BOLD(FontDirs.FONT_DIR, "MinionPro-Bold.otf"),
    MINION_ITALIC(FontDirs.MY_FONT_DIR, "Minion/MinionPro-It.ttf"),
    MINION_BOLD_ITALIC(FontDirs.FONT_DIR, "MinionPro-BoldIt.otf"),
    MINION_SMALL_CAPS(FontDirs.MY_FONT_DIR, "Minion/Minion Small Caps  Oldstyle Fi Regular.ttf"),
    HELVETICA_NEUE_ULTRA_LIGHT(FontDirs.MY_FONT_DIR, "Helvetica Neue/HelveticaNeue-UltraLight.ttf");

    // Enums aren't allows to have forward references to static members, so we wrap the static members
    // in a nested class.
    private static class FontDirs {
        private static final File MY_FONT_DIR = new File("/Users/lk/Dropbox/Personal/Fonts");
        private static final File SYSTEM_FONT_DIR = new File("/System/Library/Fonts");
        private static final File FONT_DIR = new File("/Library/Fonts");
    }

    private final File mDirectory;
    private final String mFilename;

    FontName(File directory, String filename) {
        mDirectory = directory;
        mFilename = filename;
    }

    public File getFile() {
        return new File(mDirectory, mFilename);
    }
}
