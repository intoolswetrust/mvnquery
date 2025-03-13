package com.github.kwart.mvnquery;

import java.io.File;
import java.util.Objects;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;

public class Config {

    @Parameter(names = { "--help", "-h" }, help = true, description = "Prints this help")
    private boolean printHelp;

    @Parameter(names = { "--quiet", "-q" }, description = "Don't print progress")
    private boolean quiet;

    @Parameter(names = { "--version", "-v" }, description = "Print version")
    private boolean printVersion;

    @Parameter(names = { "--groupId", "-g" }, description = "Filter by groupId")
    private String groupId;

    @Parameter(names = { "--artifactId", "-a" }, description = "Filter by artifactId")
    private String artifactId;

    @Parameter(names = { "--packaging", "-p" }, description = "Filter by packaging type")
    private String packaging = "jar";

    @Parameter(names = { "--classifier", "-c" }, description = "Filter by classifier")
    private String classifier = "-";

    @Parameter(names = { "--lastDays", "-d" }, description = "Filter artifacts modified in last X days")
    private int lastDays = 14;

    @Parameter(names = { "--config-data-dir" }, converter = FileConverter.class, description = "Set data directory for index")
    private File configDataDir = new File(System.getProperty("user.home"), ".mvnquery");

    @Parameter(names = { "--config-repo" }, description = "Set repository URL")
    private String configRepo = "https://repo1.maven.org/maven2";

    @Parameter(names = { "--use-timestamp", "-t" }, description = "Print also the lastModifiedTime")
    private boolean useTimestamp;

    @Parameter(names = { "--timestamp-format" }, description = "User defined format to print the lastModifiedTime ('iso', 'yyyyMMddHHmmssSSS', etc.) ")
    private String timestampFormat;

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getPackaging() {
        return packaging;
    }

    public String getClassifier() {
        return classifier;
    }

    public int getLastDays() {
        return lastDays;
    }

    public File getConfigDataDir() {
        return configDataDir;
    }

    public String getConfigRepo() {
        return configRepo;
    }

    public boolean isPrintHelp() {
        return printHelp;
    }

    public boolean isPrintVersion() {
        return printVersion;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public boolean isUseTimestamp() {
        return useTimestamp;
    }

    public String getTimestampFormat() {
        return timestampFormat;
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifactId, classifier, configDataDir, configRepo, groupId, lastDays, packaging, printHelp,
                printVersion, quiet, timestampFormat, useTimestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Config other = (Config) obj;
        return Objects.equals(artifactId, other.artifactId) && Objects.equals(classifier, other.classifier)
                && Objects.equals(configDataDir, other.configDataDir) && Objects.equals(configRepo, other.configRepo)
                && Objects.equals(groupId, other.groupId) && lastDays == other.lastDays
                && Objects.equals(packaging, other.packaging) && printHelp == other.printHelp
                && printVersion == other.printVersion && quiet == other.quiet
                && Objects.equals(timestampFormat, other.timestampFormat) && useTimestamp == other.useTimestamp;
    }

    @Override
    public String toString() {
        return "Config [printHelp=" + printHelp + ", quiet=" + quiet + ", printVersion=" + printVersion + ", groupId=" + groupId
                + ", artifactId=" + artifactId + ", packaging=" + packaging + ", classifier=" + classifier + ", lastDays="
                + lastDays + ", configDataDir=" + configDataDir + ", configRepo=" + configRepo + ", useTimestamp="
                + useTimestamp + ", timestampFormat=" + timestampFormat + "]";
    }

}
