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

package com.teamten.util;

/**
 * Utility class for dealing with dates and times.
 */
public class Dates {
    public static final long MS_PER_S = 1000;

    /**
     * Returns a minutes:seconds string for this duration if less than an hour,
     * or hour:minutes:seconds if an hour or more.
     */
    public static String durationToString(long ms) {
        long seconds = ms / MS_PER_S;
        long minutes = seconds / 60;
        seconds %= 60;
        long hours = minutes / 60;
        minutes %= 60;

        if (hours == 0) {
            return String.format("%02d:%02d", minutes, seconds);
        } else {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
    }

    /**
     * Return the number of milliseconds left in this process.
     *
     * @param startTime the time when the process started.
     * @param now the current timestamp, or 0 to mean the current time.
     * @param progress how far we are into the process, must be positive and
     * less than or equals to "total".
     * @param total the total number of steps in the process.
     *
     * @return the estimated number of milliseconds left, or 0 if there's been
     * no progress.
     */
    public static long estimateTimeLeft(long startTime, long now,
            long progress, long total) {

        if (progress == 0) {
            return 0;
        } else {
            if (now == 0) {
                now = System.currentTimeMillis();
            }

            return (now - startTime)*(total - progress)/progress;
        }
    }
}
