package com.github.kwart.mvnquery;

import com.beust.jcommander.DefaultUsageFormatter;
import com.beust.jcommander.JCommander;

public class UsageFormatter extends DefaultUsageFormatter {

    private final String version;
    private final String description;
    private final String usage;

    public UsageFormatter(JCommander commander, String version, String description, String usage) {
        super(commander);
        this.version = version;
        this.description = description;
        this.usage = usage;
    }

    @Override
    public void appendMainLine(StringBuilder out, boolean hasOptions, boolean hasCommands, int indentCount, String indent) {
        StringBuilder mainLine = new StringBuilder();
        out.append(version).append("\n");
        out.append(description).append("\n");
        out.append("\n");
        mainLine.append("Usage:\n");
        mainLine.append(indent).append(usage).append("\n");

        wrapDescription(out, indentCount, mainLine.toString());
        out.append("\n");
    }

}
