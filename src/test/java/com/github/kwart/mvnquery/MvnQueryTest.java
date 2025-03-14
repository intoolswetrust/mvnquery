package com.github.kwart.mvnquery;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MvnQueryTest {

    @TempDir
    Path tempDir;

    @Test
    void testPerform() throws Exception {
        Config config = Config.builder().withConfigDataDir(tempDir.toFile())
                .withConfigRepo("https://repository.jboss.org/nexus/content/repositories/thirdparty-releases").withLastDays(0)
                .withGroupId("apache-struts").build();
        try (ByteArrayOutputStream infoOS = new ByteArrayOutputStream();
                ByteArrayOutputStream resultOS = new ByteArrayOutputStream();
                PrintStream infoPS = new PrintStream(infoOS, true, UTF_8.name());
                PrintStream resultPS = new PrintStream(resultOS, true, UTF_8.name())) {
            MvnQuery mvnQuery = new MvnQuery(config, resultPS, infoPS);
            mvnQuery.perform();
            // base64(sha256(repo)).substring(0, 10)
            assertThat(tempDir.resolve("-kNTV7I8x0")).isDirectory();
            String info = infoOS.toString(UTF_8);
            assertThat(info).isNotNull().contains("Full update happened!", "+g:apache-struts +p:jar -l:*");
            String result = resultOS.toString(UTF_8);
            assertThat(result).contains("apache-struts:struts:1.2.6:jar:");
            assertThat(result).doesNotContain("apache-bsf:bsf:2.4.0:jar:");
        }
    }

}
