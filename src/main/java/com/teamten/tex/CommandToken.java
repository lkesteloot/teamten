package com.teamten.tex;

/**
 * Represents a TeX command (those that start with a backslash, like \hbox).
 */
public class CommandToken extends Token {
    private final String mCommand;

    public CommandToken(String command) {
        mCommand = command;
    }

    /**
     * The command itself, not including the backslash.
     */
    public String getCommand() {
        return mCommand;
    }

    @Override
    public String toString() {
        return "CommandToken: \\" + mCommand;
    }
}
