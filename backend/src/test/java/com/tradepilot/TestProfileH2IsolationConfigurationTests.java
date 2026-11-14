package com.tradepilot;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TestProfileH2IsolationConfigurationTests {

    @Test
    void testProfileUsesAUniqueInMemoryDatabaseForEachSpringContext() throws IOException {
        String configuration = Files.readString(Path.of("src/test/resources/application-test.yml"));

        assertThat(configuration)
                .contains("jdbc:h2:mem:tradepilot-${random.uuid};MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
                .doesNotContain("jdbc:h2:mem:tradepilot;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    }
}
