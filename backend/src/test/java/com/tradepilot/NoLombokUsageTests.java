package com.tradepilot;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class NoLombokUsageTests {

    private static final List<String> FORBIDDEN_TOKENS = List.of(
            "import " + "lombok.",
            annotation("Data"),
            annotation("Getter"),
            annotation("Setter"),
            annotation("Builder"),
            annotation("NoArgsConstructor"),
            annotation("AllArgsConstructor"),
            annotation("RequiredArgsConstructor"),
            annotation("Slf4j"),
            annotation("SneakyThrows"),
            annotation("SuperBuilder")
    );

    @Test
    void backendSourcesDoNotUseLombok() throws IOException {
        List<String> violations = new ArrayList<>();

        for (Path root : List.of(Path.of("src/main/java"), Path.of("src/test/java"))) {
            if (!Files.exists(root)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(root)) {
                files.filter(path -> path.toString().endsWith(".java"))
                        .forEach(path -> scan(path, violations));
            }
        }

        assertThat(violations).isEmpty();
    }

    private void scan(Path path, List<String> violations) {
        try {
            List<String> lines = Files.readAllLines(path);
            for (int i = 0; i < lines.size(); i++) {
                String line = stripLineComment(lines.get(i));
                for (String token : FORBIDDEN_TOKENS) {
                    if (line.contains(token)) {
                        violations.add(path + ":" + (i + 1) + " contains " + token);
                    }
                }
            }
        } catch (IOException ex) {
            violations.add(path + " could not be scanned: " + ex.getMessage());
        }
    }

    private String stripLineComment(String line) {
        int commentStart = line.indexOf("//");
        return commentStart >= 0 ? line.substring(0, commentStart) : line;
    }

    private static String annotation(String name) {
        return "@" + name;
    }
}
