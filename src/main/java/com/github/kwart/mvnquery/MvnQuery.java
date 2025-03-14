package com.github.kwart.mvnquery;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
        return timestampFormatter != null ? timestampFormatter.format(Instant.ofEpochMilli(timestamp)) : Long.toString(timestamp);
    }

    private BooleanQuery buildQuery() {
        log("Building the query");

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        addToQuery(builder, MAVEN.GROUP_ID, config.getGroupId());
        addToQuery(builder, MAVEN.ARTIFACT_ID, config.getArtifactId());
        addToQuery(builder, MAVEN.PACKAGING, config.getPackaging());
        if (!addToQuery(builder, MAVEN.CLASSIFIER, config.getClassifier()) && config.getClassifier() != null) {
            builder.add(indexer.constructQuery(MAVEN.CLASSIFIER, new SourcedSearchExpression(Field.NOT_PRESENT)),
                    Occur.MUST_NOT);
        }

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
        builder.add(indexer.constructQuery(field, new SourcedSearchExpression(val)), Occur.MUST);
        return true;
    }

    private void updateIndex(IndexingContext indexingContext) throws IOException {
        // Update the index (incremental update will happen if this is not 1st run and files are not deleted)
        // This whole block below should not be executed on every app start, but rather controlled by some configuration
        // since this block will always emit at least one HTTP GET. Maven Central indexes are updated once a week, but
        // other index sources might have different index publishing frequency.
        // Preferred frequency is once a week.
        Instant updateStart = Instant.now();
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
        log("\tFinished in " + Duration.between(updateStart, Instant.now()).getSeconds() + " sec");
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

        Path repoUrlPath = repoDir.toPath().resolve("repo-url");
        if (!Files.exists(repoUrlPath)) {
            Files.writeString(repoUrlPath, configRepo, StandardOpenOption.CREATE);
        }
        File cacheDir = new File(repoDir, "cache");
        File indexDir = new File(repoDir, "index");

        // Use a custom creator which indexes lastModified ArtifactInfo value
        List<IndexCreator> indexers = new ArrayList<>();
        indexers.add(new CustomArtifactInfoIndexCreator());

        // Create context for central repository index
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