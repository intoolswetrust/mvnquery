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
    private String packaging;

    @Parameter(names = { "--classifier", "-c" }, description = "Filter by classifier")
    private String classifier;

    @Parameter(names = { "--lastDays", "-d" }, description = "Filter artifacts modified in last X days")
    private int lastDays;

    @Parameter(names = { "--config-data-dir" }, converter = FileConverter.class, description = "Set data directory for index")
    private File configDataDir;

    @Parameter(names = { "--config-repo" }, description = "Set repository URL")
    private String configRepo;

    @Parameter(names = { "--use-timestamp", "-t" }, description = "Include the lastModified field in query results")
    private boolean useTimestamp;

    @Parameter(names = { "--timestamp-format" }, description = "User defined format to print the lastModifiedTime ('iso', 'yyyyMMddHHmmssSSS', etc.) ")
    private String timestampFormat;

    public Config() {
        this(builder());
    }

    private Config(Builder builder) {
        this.printHelp = builder.printHelp;
        this.quiet = builder.quiet;
        this.printVersion = builder.printVersion;
        this.groupId = builder.groupId;
        this.artifactId = builder.artifactId;
        this.packaging = builder.packaging;
        this.classifier = builder.classifier;
        this.lastDays = builder.lastDays;
        this.configDataDir = builder.configDataDir;
        this.configRepo = builder.configRepo;
        this.useTimestamp = builder.useTimestamp;
        this.timestampFormat = builder.timestampFormat;
    }

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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean printHelp;
        private boolean quiet;
        private boolean printVersion;
        private String groupId;
        private String artifactId;
        private String packaging = "jar";
        private String classifier = "-";
        private int lastDays = 14;
        private File configDataDir = new File(System.getProperty("user.home"), ".mvnquery");
        private String configRepo = "https://repo1.maven.org/maven2";
        private boolean useTimestamp;
        private String timestampFormat;

        private Builder() {
        }

        public Builder withPrintHelp(boolean printHelp) {
            this.printHelp = printHelp;
            return this;
        }

        public Builder withQuiet(boolean quiet) {
            this.quiet = quiet;
            return this;
        }

        public Builder withPrintVersion(boolean printVersion) {
            this.printVersion = printVersion;
            return this;
        }

        public Builder withGroupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder withArtifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public Builder withPackaging(String packaging) {
            this.packaging = packaging;
            return this;
        }

        public Builder withClassifier(String classifier) {
            this.classifier = classifier;
            return this;
        }

        public Builder withLastDays(int lastDays) {
            this.lastDays = lastDays;
            return this;
        }

        public Builder withConfigDataDir(File configDataDir) {
            this.configDataDir = configDataDir;
            return this;
        }

        public Builder withConfigRepo(String configRepo) {
            this.configRepo = configRepo;
            return this;
        }

        public Builder withUseTimestamp(boolean useTimestamp) {
            this.useTimestamp = useTimestamp;
            return this;
        }

        public Builder withTimestampFormat(String timestampFormat) {
            this.timestampFormat = timestampFormat;
            return this;
        }

        public Config build() {
            return new Config(this);
        }
    }

}
