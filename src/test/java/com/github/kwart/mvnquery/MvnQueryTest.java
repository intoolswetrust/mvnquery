package com.github.kwart.mvnquery;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import com.github.kwart.mvnquery.Config.Builder;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.SimpleFileServer;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MvnQueryTest {

    private static final int REPO_PORT = 5757;

    @TempDir
    Path tempDir;

    private HttpServer staticServer;

    @BeforeAll
    void startServers() throws IOException {
        Path root = Path.of("src/test/resources/index").toAbsolutePath();
        staticServer = HttpServer.create(new InetSocketAddress(REPO_PORT), 0);
        staticServer.createContext("/.index/", SimpleFileServer.createFileHandler(root));
        staticServer.start();
        System.out.println("Dummy repository index server started on port " + REPO_PORT);

    }

    @AfterAll
    void stopServers() {
        staticServer.stop(0);
        System.out.println("Static index server stopped.");
    }

    @Test
    void testPerform() throws Exception {
        Builder configBuilder = Config.builder().withConfigDataDir(tempDir.toFile())
                .withConfigRepo("http://localhost:" + REPO_PORT).withLastDays(0).withSkipUpdate(true)
                .withGroupId("com.hazelcast").withArtifactId("hazelcast");
        Config config = configBuilder.build();
        try (ByteArrayOutputStream infoOS = new ByteArrayOutputStream();
                ByteArrayOutputStream resultOS = new ByteArrayOutputStream();
                PrintStream infoPS = new PrintStream(infoOS, true, UTF_8.name());
                PrintStream resultPS = new PrintStream(resultOS, true, UTF_8.name())) {
            MvnQuery mvnQuery = new MvnQuery(config, resultPS, infoPS);
            mvnQuery.perform();
            // base64(sha256(repo)).substring(0, 10)
            Path repoDir = tempDir.resolve("crcaNi7JcZ");
            assertThat(repoDir).isDirectory();
            Path propertyFile = repoDir.resolve("index.properties");
            assertThat(propertyFile).isNotEmptyFile();
            String info = infoOS.toString(UTF_8);
            assertThat(info).isNotNull().contains("Full update happened!", "+v:* +g:com.hazelcast +a:hazelcast +e:jar");
            String result = resultOS.toString(UTF_8);
            assertThat(result).contains("com.hazelcast:hazelcast:3.12.13:jar:");
            assertThat(result).doesNotContain("com.hazelcast:hazelcast-client:3.12.13:jar:");
            assertThat(info).doesNotContain("Skipping index update");
        }
        config = configBuilder.withClassifier("").build();
        try (ByteArrayOutputStream infoOS = new ByteArrayOutputStream();
                ByteArrayOutputStream resultOS = new ByteArrayOutputStream();
                PrintStream infoPS = new PrintStream(infoOS, true, UTF_8.name());
                PrintStream resultPS = new PrintStream(resultOS, true, UTF_8.name())) {
            MvnQuery mvnQuery = new MvnQuery(config, resultPS, infoPS);
            mvnQuery.perform();
            String result = resultOS.toString(UTF_8);
            assertThat(result).contains("com.hazelcast:hazelcast:3.12.13:jar:");
            assertThat(result).doesNotContain("com.hazelcast:hazelcast-client:3.12.13:jar:");
            String info = infoOS.toString(UTF_8);
            assertThat(info).contains("Skipping index update");
        }

        config = configBuilder.withForceUpdate(true).build();
        try (ByteArrayOutputStream infoOS = new ByteArrayOutputStream();
                ByteArrayOutputStream resultOS = new ByteArrayOutputStream();
                PrintStream infoPS = new PrintStream(infoOS, true, UTF_8.name());
                PrintStream resultPS = new PrintStream(resultOS, true, UTF_8.name())) {
            MvnQuery mvnQuery = new MvnQuery(config, resultPS, infoPS);
            mvnQuery.perform();
            String info = infoOS.toString(UTF_8);
            assertThat(info).doesNotContain("Skipping index update");
        }
    }

}
