package com.github.kwart.mvnquery;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.kwart.mvnquery.Config.Builder;

class MvnQueryTest {

    @TempDir
    Path tempDir;

    @Test
    void testPerform() throws Exception {
        Builder configBuilder = Config.builder().withConfigDataDir(tempDir.toFile())
                .withConfigRepo("https://repository.jboss.org/nexus/content/repositories/thirdparty-releases").withLastDays(0)
                .withSkipUpdate(true).withGroupId("apache-struts");
        Config config = configBuilder.build();
        try (ByteArrayOutputStream infoOS = new ByteArrayOutputStream();
                ByteArrayOutputStream resultOS = new ByteArrayOutputStream();
                PrintStream infoPS = new PrintStream(infoOS, true, UTF_8.name());
                PrintStream resultPS = new PrintStream(resultOS, true, UTF_8.name())) {
            MvnQuery mvnQuery = new MvnQuery(config, resultPS, infoPS);
            mvnQuery.perform();
            // base64(sha256(repo)).substring(0, 10)
            Path repoDir = tempDir.resolve("-kNTV7I8x0");
            assertThat(repoDir).isDirectory();
            Path propertyFile = repoDir.resolve("index.properties");
            assertThat(propertyFile).isNotEmptyFile();
            String info = infoOS.toString(UTF_8);
            assertThat(info).isNotNull().contains("Full update happened!", "+g:apache-struts +p:jar -l:*");
            String result = resultOS.toString(UTF_8);
            assertThat(result).contains("apache-struts:struts:1.2.6:jar:");
            assertThat(result).doesNotContain("apache-bsf:bsf:2.4.0:jar:");
            assertThat(info).doesNotContain("Skipping index update");
            mvnQuery.perform();
            info = infoOS.toString(UTF_8);
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
