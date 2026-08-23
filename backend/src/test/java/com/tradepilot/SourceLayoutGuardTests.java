package com.tradepilot;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SourceLayoutGuardTests {
    @Test
    void mainJavaSourcesDoNotContainAccidentalDuplicateCopyFiles() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        List<String> duplicateSources;
        try (var paths = Files.walk(sourceRoot)) {
            duplicateSources = paths
                    .filter(Files::isRegularFile)
                    .map(sourceRoot::relativize)
                    .map(Path::toString)
                    .filter(path -> path.endsWith(".java"))
                    .filter(path -> path.matches(".*( 2| copy| duplicate)\\.java$"))
                    .sorted()
                    .toList();
        }

        assertThat(duplicateSources)
                .as("Accidental duplicate Java source files under src/main/java")
                .isEmpty();
    }

    @Test
    void frontendSourcesDoNotContainAccidentalDuplicateCopyFiles() throws IOException {
        Path sourceRoot = Path.of("../frontend/src");
        List<String> duplicateSources;
        try (var paths = Files.walk(sourceRoot)) {
            duplicateSources = paths
                    .filter(Files::isRegularFile)
                    .map(sourceRoot::relativize)
                    .map(Path::toString)
                    .filter(path -> path.endsWith(".ts") || path.endsWith(".tsx"))
                    .filter(path -> path.matches(".*( 2| copy| duplicate)\\.(ts|tsx)$"))
                    .sorted()
                    .toList();
        }

        assertThat(duplicateSources)
                .as("Accidental duplicate TypeScript source files under frontend/src")
                .isEmpty();
    }

    @Test
    void importSessionClassificationDoesNotUseHardcodedFallbacksOrJvmTimezone() throws IOException {
        Path sourceRoot = Path.of("src/main/java/com/tradepilot/importing");
        Set<String> allowedFiles = Set.of("TradingSessionClassifier.java", "TradingSessionPolicy.java");
        List<String> unsafeMatches;
        try (var paths = Files.walk(sourceRoot)) {
            unsafeMatches = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !allowedFiles.contains(path.getFileName().toString()))
                    .flatMap(path -> {
                        try {
                            return Files.readAllLines(path).stream()
                                    .filter(line -> line.contains("SessionType.NY_AM") || line.contains("ZoneId.systemDefault()"))
                                    .map(line -> sourceRoot.relativize(path) + ": " + line.trim());
                        } catch (IOException ex) {
                            throw new java.io.UncheckedIOException(ex);
                        }
                    })
                    .sorted()
                    .toList();
        }

        assertThat(unsafeMatches)
                .as("Importers must use TradingSessionClassifier instead of hardcoded NY_AM or JVM timezone")
                .isEmpty();
    }
}
