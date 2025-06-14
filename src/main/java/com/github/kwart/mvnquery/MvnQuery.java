package com.github.kwart.mvnquery;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.LogManager;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.util.Constants;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.Field;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.IteratorSearchRequest;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.context.ExistingLuceneIndexMismatchException;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdateResult;
import org.apache.maven.index.updater.IndexUpdater;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.sisu.launch.Main;
import org.eclipse.sisu.space.BeanScanning;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.internal.DefaultConsole;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Query Maven Index. See
 * https://stackoverflow.com/questions/5776519/how-to-parse-unzip-unpack-maven-repository-indexes-generated-by-nexus
 */
public class MvnQuery {

    private static final String PROP_LAST_UPDATE_TIMESTAMP = "last.update.timestamp";
    private static final String PROP_UPDATE_INTERVAL_HOURS = "update.interval.hours";
    private static final String PROP_REPOSITORY_URL = "repository.url";

    private static final String FILENAME_INDEX_PROPERTIES = "index.properties";

    public static final String VERSION;

    private final Indexer indexer;
    private final IndexUpdater indexUpdater;
    private final Config config;
    private final DateTimeFormatter timestampFormatter;
    private final PrintStream resultStream;
    private final PrintStream infoStream;

    public MvnQuery(Config config) throws Exception {
        this(config, System.out, System.err);
    }

    public MvnQuery(Config config, PrintStream resultStream, PrintStream infoStream) throws Exception {
        this.config = requireNonNull(config);
        this.infoStream = requireNonNull(infoStream);
        this.resultStream = requireNonNull(resultStream);
        String tf = config.getTimestampFormat();
        if (tf != null) {
            timestampFormatter = "ISO".equals(tf.toUpperCase(Locale.ROOT)) ? DateTimeFormatter.ISO_INSTANT
                    : DateTimeFormatter.ofPattern(tf).withZone(ZoneId.systemDefault());
        } else {
            timestampFormatter = null;
        }
        Injector injector = Guice.createInjector(Main.wire(BeanScanning.CACHE));
        MvnIndexerContext ctx = injector.getInstance(MvnIndexerContext.class);
        this.indexer = ctx.indexer;
        this.indexUpdater = ctx.indexUpdater;
    }

    public static void main(String args[]) throws Exception {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        Config config = new Config();
        JCommander jcmd = JCommander.newBuilder().programName("MvnQuery").console(new DefaultConsole(System.err))
                .acceptUnknownOptions(true).addObject(config).build();
        jcmd.parse(args);
        UsageFormatter usageFormatter = new UsageFormatter(jcmd, versionLine(),
                "MvnQuery retrieves Maven repository index and makes query on it.",
                "java --enable-native-access=ALL-UNNAMED -jar mvnquery.jar [options]");
        if (config.isPrintHelp() || !jcmd.getUnknownOptions().isEmpty()) {
            jcmd.setUsageFormatter(usageFormatter);
            jcmd.usage();
            System.exit(2);
        }

        if (config.isPrintVersion()) {
            System.err.println(versionLine());
            return;
        }
        MvnQuery app = new MvnQuery(config);
        app.perform();
    }

    public void perform() throws IOException, InvalidVersionSpecificationException {
        log("Use --quiet (-q) argument to supress the debug output. Use --help (-h) to print the help.\n");
        IndexingContext indexingContext = initIndexingContext();
        updateIndex(indexingContext);
        BooleanQuery query = buildQuery();
        runQuery(indexingContext, query);
        indexer.closeIndexingContext(indexingContext, false);
    }

    private void runQuery(IndexingContext indexingContext, BooleanQuery query) throws IOException {
        log("Querying index");
        log("------");
        Instant searchStart = Instant.now();
        final IteratorSearchRequest request = new IteratorSearchRequest(query, Collections.singletonList(indexingContext));
        long count = 0L;
        try (final IteratorSearchResponse response = indexer.searchIterator(request)) {
            for (ArtifactInfo ai : response) {
                resultStream.println(getCoordinates(ai));
                count++;
            }
            long secondsDiff = Duration.between(searchStart, Instant.now()).getSeconds();
            log("------");
            log("Total response size: " + response.getTotalHitsCount());
            log("Artifacts listed: " + count);
            log("Query took " + secondsDiff + " seconds");
            log();
        }
    }

    private String getCoordinates(ArtifactInfo ai) {
        StringBuilder sb = new StringBuilder();
        sb.append(ai.getGroupId()).append(":").append(ai.getArtifactId()).append(":").append(ai.getVersion()).append(":")
                .append(ai.getPackaging()).append(":").append(Objects.toString(ai.getClassifier(), ""));

        if (config.isUseTimestamp()) {
            sb.append(":").append(formatTimestamp(ai.getLastModified()));
        }
        return sb.toString();
    }

    private String formatTimestamp(long timestamp) {
        return timestampFormatter != null ? timestampFormatter.format(Instant.ofEpochMilli(timestamp))
                : Long.toString(timestamp);
    }

    private BooleanQuery buildQuery() {
        log("Building the query");

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        addToQuery(builder, MAVEN.GROUP_ID, config.getGroupId());
        addToQuery(builder, MAVEN.ARTIFACT_ID, config.getArtifactId());
        addToQuery(builder, MAVEN.PACKAGING, config.getPackaging());
        addToQuery(builder, MAVEN.CLASSIFIER, config.getClassifier());

        int lastDays = config.getLastDays();
        if (lastDays > 0) {
            long lastModifiedRangeStart = Instant.now().minus(lastDays, ChronoUnit.DAYS).toEpochMilli();
            builder.add(LongPoint.newRangeQuery(CustomArtifactInfoIndexCreator.FLD_LAST_MODIFIED.getKey(),
                    lastModifiedRangeStart, Long.MAX_VALUE), Occur.MUST);
        }
        BooleanQuery query = builder.build();
        log("\t" + query);
        return query;
    }

    private boolean addToQuery(Builder builder, Field field, String val) {
        if (null == val || "-".equals(val)) {
            return false;
        }
        if (val.isEmpty()) {
            builder.add(indexer.constructQuery(field, new SourcedSearchExpression(Field.NOT_PRESENT)), Occur.MUST_NOT);
        } else {
            builder.add(indexer.constructQuery(field, new SourcedSearchExpression(val)), Occur.MUST);
        }

        return true;
    }

    private void updateIndex(IndexingContext indexingContext) throws IOException {
        File repoDir = new File(config.getConfigDataDir(), hashRepo(config.getConfigRepo()));
        Path propsPath = repoDir.toPath().resolve(FILENAME_INDEX_PROPERTIES);

        Properties props = new Properties();
        if (Files.exists(propsPath)) {
            try (InputStream in = Files.newInputStream(propsPath)) {
                props.load(in);
            }
        }

        // Maven Central indexes are updated once a week, but
        // other index sources might have different index publishing frequency.
        // Let's default to a reasonable value.
        long updateIntervalHours = 48;
        if (props.containsKey(PROP_UPDATE_INTERVAL_HOURS)) {
            try {
                updateIntervalHours = Long.parseLong(props.getProperty(PROP_UPDATE_INTERVAL_HOURS));
            } catch (NumberFormatException ignored) {
            }
        }
        props.setProperty(PROP_UPDATE_INTERVAL_HOURS, Long.toString(updateIntervalHours));

        Instant now = Instant.now();
        Instant lastUpdate = null;
        if (props.containsKey(PROP_LAST_UPDATE_TIMESTAMP)) {
            try {
                lastUpdate = Instant.ofEpochSecond(Long.parseLong(props.getProperty(PROP_LAST_UPDATE_TIMESTAMP)));
            } catch (NumberFormatException ignored) {
            }
        }

        boolean needsUpdate = true;

        // When it's the first update or force flag is set, then always perform the update
        if (!(config.isForceUpdate() || indexingContext.getTimestamp() == null)) {
            if (config.isSkipUpdate()) {
                needsUpdate = false;
            } else if (lastUpdate != null) {
                Instant nextAllowedUpdate = lastUpdate.plus(updateIntervalHours, ChronoUnit.HOURS);
                needsUpdate = now.isAfter(nextAllowedUpdate);
            }
        }

        if (!needsUpdate) {
            log("Skipping index update (not needed or explicitly suppressed)");
            return;
        }

        log("Updating Index ...");
        log("\tThis might take a while on first run, so please be patient!");

        Date contextCurrentTimestamp = indexingContext.getTimestamp();
        IndexUpdateRequest updateRequest = new IndexUpdateRequest(indexingContext, new Java11HttpClient());
        IndexUpdateResult updateResult = indexUpdater.fetchAndUpdateIndex(updateRequest);

        if (updateResult.isFullUpdate()) {
            log("\tFull update happened!");
        } else {
            Date timestamp = updateResult.getTimestamp();
            if (timestamp == null || timestamp.equals(contextCurrentTimestamp)) {
                log("\tNo update needed, index is up to date!");
            } else {
                log("\tIncremental update happened, change covered " + contextCurrentTimestamp + " - " + timestamp
                        + " period.");
            }
        }

        props.setProperty(PROP_LAST_UPDATE_TIMESTAMP, Long.toString(now.getEpochSecond()));
        props.setProperty(PROP_REPOSITORY_URL, config.getConfigRepo());

        try (OutputStream out = Files.newOutputStream(propsPath)) {
            props.store(out, "MvnQuery repository index properties");
        }

        log("\tFinished in " + Duration.between(now, Instant.now()).getSeconds() + " sec");
        log();
    }

    private IndexingContext initIndexingContext() throws IOException, ExistingLuceneIndexMismatchException {
        String configRepo = config.getConfigRepo();
        log("Initiating indexing context for " + configRepo);
        String repoHash = hashRepo(configRepo);
        File repoDir = new File(config.getConfigDataDir(), repoHash);
        log("\t- repository index data location: " + repoDir);
        if (!repoDir.exists()) {
            log("\t- creating index data directory");
            repoDir.mkdirs();
        }

        File cacheDir = new File(repoDir, "cache");
        File indexDir = new File(repoDir, "index");

        List<IndexCreator> indexers = new ArrayList<>();
        indexers.add(new CustomArtifactInfoIndexCreator());

        return indexer.createIndexingContext(repoHash, repoHash, cacheDir, indexDir, configRepo, null, true, true, indexers);
    }

    private static String hashRepo(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 10);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    private void log(String line) {
        if (!config.isQuiet()) {
            infoStream.println(line);
        }
    }

    private void log() {
        log("");
    }

    private static String versionLine() {
        return "MvnQuery version " + VERSION;
    }

    static {
        String version = "[UNKNOWN]";
        try (InputStream is = Constants.class
                .getResourceAsStream("/META-INF/maven/com.github.kwart.mvnquery/mvnquery/pom.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                if (props.containsKey("version")) {
                    version = props.getProperty("version");
                }
            }
        } catch (IOException e) {
            // ignore
        }
        VERSION = version;
    }
}