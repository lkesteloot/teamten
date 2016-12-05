/*
 *
 *    Copyright 2016 Lawrence Kesteloot
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.teamten.font;

import java.io.File;

/**
 * Represents the individual fonts that we know about and where to find them.
 */
public enum FontName {
    TIMES_NEW_ROMAN(FontDirs.FONT_DIR, "Times New Roman.ttf"),
    TIMES_NEW_ROMAN_BOLD(FontDirs.FONT_DIR, "Times New Roman Bold.ttf"),
    TIMES_NEW_ROMAN_ITALIC(FontDirs.FONT_DIR, "Times New Roman Italic.ttf"),
    TIMES_NEW_ROMAN_BOLD_ITALIC(FontDirs.FONT_DIR, "Times New Roman Italic.ttf"),
    COURIER_NEW(FontDirs.FONT_DIR, "Courier New.ttf"),
    COURIER_NEW_BOLD(FontDirs.FONT_DIR, "Courier New Bold.ttf"),
    COURIER_NEW_ITALIC(FontDirs.FONT_DIR, "Courier New Italic.ttf"),
    COURIER_NEW_BOLD_ITALIC(FontDirs.FONT_DIR, "Courier New Bold Italic.ttf"),
    ROBOTO_MONO_THIN(FontDirs.MY_FONT_DIR, "RobotoMono/RobotoMono-Thin.ttf"),
    ROBOTO_MONO_THIN_ITALIC(FontDirs.MY_FONT_DIR, "RobotoMono/RobotoMono-ThinItalic.ttf"),
    ROBOTO_MONO_LIGHT(FontDirs.MY_FONT_DIR, "RobotoMono/RobotoMono-Light.ttf"),
    ROBOTO_MONO_LIGHT_ITALIC(FontDirs.MY_FONT_DIR, "RobotoMono/RobotoMono-LightItalic.ttf"),
    ROBOTO_MONO(FontDirs.MY_FONT_DIR, "RobotoMono/RobotoMono-Regular.ttf"),
    ROBOTO_MONO_ITALIC(FontDirs.MY_FONT_DIR, "RobotoMono/RobotoMono-Italic.ttf"),
    ROBOTO_MONO_MEDIUM(FontDirs.MY_FONT_DIR, "RobotoMono/RobotoMono-Medium.ttf"),
    ROBOTO_MONO_MEDIUM_ITALIC(FontDirs.MY_FONT_DIR, "RobotoMono/RobotoMono-MediumItalic.ttf"),
    ROBOTO_MONO_BOLD(FontDirs.MY_FONT_DIR, "RobotoMono/RobotoMono-Bold.ttf"),
    ROBOTO_MONO_BOLD_ITALIC(FontDirs.MY_FONT_DIR, "RobotoMono/RobotoMono-BoldItalic.ttf"),
    MINION(FontDirs.MY_FONT_DIR, "Minion/MinionPro-Regular.ttf"),
    MINION_BOLD(FontDirs.MY_FONT_DIR, "Minion/MinionPro-Bold.ttf"),
    MINION_ITALIC(FontDirs.MY_FONT_DIR, "Minion/MinionPro-It.ttf"),
    MINION_BOLD_ITALIC(FontDirs.MY_FONT_DIR, "Minion/MinionPro-BoldIt.ttf"),
    MINION_SMALL_CAPS(FontDirs.MY_FONT_DIR, "Minion/Minion Small Caps  Oldstyle Fi Regular.ttf"),
    HELVETICA_NEUE_ULTRA_LIGHT(FontDirs.MY_FONT_DIR, "Helvetica Neue/HelveticaNeue-UltraLight.ttf"),
    ALEGREYA_REGULAR(FontDirs.MY_FONT_DIR, "Alegreya/Alegreya-Regular.ttf"),
    ALEGREYA_ITALIC(FontDirs.MY_FONT_DIR, "Alegreya/Alegreya-Italic.ttf"),
    ALEGREYA_SMALL_CAPS(FontDirs.MY_FONT_DIR, "Alegreya/AlegreyaSC-Regular.ttf"),
    IM_FELL_ENGLISH_REGULAR(FontDirs.MY_FONT_DIR, "IM Fell English/IMFeENrm28P.ttf"),
    IM_FELL_ENGLISH_ITALIC(FontDirs.MY_FONT_DIR, "IM Fell English/IMFeENit28P.ttf"),
    IM_FELL_ENGLISH_SMALL_CAPS(FontDirs.MY_FONT_DIR, "IM Fell English/IMFeENsc28P.ttf"),
    SORTS_MILL_GOUDY_REGULAR(FontDirs.MY_FONT_DIR, "Sorts Mill Goudy/SortsMillGoudy-Regular.ttf"),
    SORTS_MILL_GOUDY_ITALIC(FontDirs.MY_FONT_DIR, "Sorts Mill Goudy/SortsMillGoudy-Italic.ttf");

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
