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

// Copyright 2011 Lawrence Kesteloot

package com.teamten.jawa;

import java.io.IOException;

/**
 * Pre-processor for Jawa files.
 */
public class Jawa {
    public static void main(String[] args) throws IOException {
        new Jawa().run(args);
    }

    private void run(String[] args) throws IOException {
        String inputFilename = args[0];
        String outputFilename = args[1];

        new JawaProcessor().processFile(inputFilename, outputFilename);
    }
}
