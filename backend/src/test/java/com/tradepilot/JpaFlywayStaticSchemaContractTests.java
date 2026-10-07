package com.tradepilot;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A fast, source-level guard for migration omissions. PostgreSQL/Hibernate
 * validation remains the authoritative complete persistence-contract test.
 */
class JpaFlywayStaticSchemaContractTests {
    private static final Path JAVA_ROOT = Path.of("src/main/java");
    private static final Path MIGRATION_ROOT = Path.of("src/main/resources/db/migration");
    private static final Pattern TABLE = Pattern.compile("@Table\\s*\\(\\s*name\\s*=\\s*\\\"([^\\\"]+)\\\"", Pattern.DOTALL);
    private static final Pattern COLUMN = Pattern.compile("@Column\\s*(?:\\(([^)]*)\\))?\\s*private\\s+[^;=]+\\s+(\\w+)\\s*(?:=[^;]+)?;", Pattern.DOTALL);
    private static final Pattern JOIN_COLUMN = Pattern.compile("@JoinColumn\\s*\\(([^)]*)\\)");
    private static final Pattern INDEX = Pattern.compile("@Index\\s*\\(\\s*name\\s*=\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern UNIQUE_CONSTRAINT = Pattern.compile("@UniqueConstraint\\s*\\(\\s*name\\s*=\\s*\\\"([^\\\"]+)\\\"");

    @Test
    void everyEntityTableExplicitColumnLengthAndNamedIndexIsRepresentedInFlywaySources() throws IOException {
        String migrations = migrationSources();
        List<Path> entities = entitySources();
        assertThat(entities).hasSize(58);

        for (Path entity : entities) {
            String source = Files.readString(entity);
            String table = tableName(source, entity);
            String tableSchema = schemaForTable(migrations, table);

            assertThat(tableSchema)
                    .as("Flyway schema for entity table %s (%s)", table, entity)
                    .isNotBlank();

            Matcher columns = COLUMN.matcher(source);
            while (columns.find()) {
                String attributes = columns.group(1) == null ? "" : columns.group(1);
                String column = attribute(attributes, "name");
                if (column == null) {
                    column = snakeCase(columns.group(2));
                }

                assertThat(tableSchema)
                        .as("Flyway column %s.%s (%s)", table, column, entity)
                        .containsPattern("(?s).*\\b" + Pattern.quote(column) + "\\b.*");

                String length = attributeNumber(attributes, "length");
                if (length != null) {
                    assertThat(tableSchema)
                            .as("Flyway VARCHAR length for %s.%s", table, column)
                            .containsPattern("(?is).*(?:\\b" + Pattern.quote(column)
                                    + "\\s+varchar\\s*\\(\\s*" + length + "\\s*\\)"
                                    + "|alter\\s+column\\s+" + Pattern.quote(column)
                                    + "\\s+type\\s+varchar\\s*\\(\\s*" + length + "\\s*\\)).*");
                }

                if (attributes.contains("nullable = false")) {
                    assertThat(tableSchema)
                            .as("Flyway NOT NULL contract for %s.%s", table, column)
                            .containsPattern("(?is).*(?:\\b" + Pattern.quote(column)
                                    + "\\b[^\\n]*\\bnot\\s+null|alter\\s+column\\s+" + Pattern.quote(column)
                                    + "\\s+set\\s+not\\s+null).*");
                }

                String precision = attributeNumber(attributes, "precision");
                String scale = attributeNumber(attributes, "scale");
                if (precision != null && scale != null) {
                    assertThat(tableSchema)
                            .as("Flyway NUMERIC precision and scale for %s.%s", table, column)
                            .containsPattern("(?is).*\\b" + Pattern.quote(column) + "\\s+numeric\\s*\\(\\s*"
                                    + precision + "\\s*,\\s*" + scale + "\\s*\\).*");
                }

                if (attributes.toLowerCase().contains("columndefinition = \"text\"")) {
                    assertThat(tableSchema)
                            .as("Flyway TEXT contract for %s.%s", table, column)
                            .containsPattern("(?is).*\\b" + Pattern.quote(column) + "\\s+text\\b.*");
                }
            }

            Matcher joinColumns = JOIN_COLUMN.matcher(source);
            while (joinColumns.find()) {
                String attributes = joinColumns.group(1);
                String column = attribute(attributes, "name");
                assertThat(column).as("@JoinColumn name for %s", entity).isNotNull();
                assertThat(tableSchema)
                        .as("Flyway join column %s.%s", table, column)
                        .containsPattern("(?is).*\\b" + Pattern.quote(column) + "\\b.*");
                if (attributes.contains("nullable = false")) {
                    assertThat(tableSchema)
                            .as("Flyway NOT NULL join-column contract for %s.%s", table, column)
                            .containsPattern("(?is).*(?:\\b" + Pattern.quote(column)
                                    + "\\b[^\\n]*\\bnot\\s+null|alter\\s+column\\s+" + Pattern.quote(column)
                                    + "\\s+set\\s+not\\s+null).*");
                }
            }

            Matcher indexes = INDEX.matcher(source);
            while (indexes.find()) {
                assertThat(migrations)
                        .as("Flyway named index %s for %s", indexes.group(1), table)
                        .containsPattern("(?is).*(create\\s+(unique\\s+)?index(?:\\s+if\\s+not\\s+exists)?\\s+"
                                + Pattern.quote(indexes.group(1)) + "|constraint\\s+"
                                + Pattern.quote(indexes.group(1)) + ").*");
            }

            Matcher uniqueConstraints = UNIQUE_CONSTRAINT.matcher(source);
            while (uniqueConstraints.find()) {
                assertThat(migrations)
                        .as("Flyway unique constraint %s for %s", uniqueConstraints.group(1), table)
                        .containsPattern("(?is).*constraint\\s+" + Pattern.quote(uniqueConstraints.group(1))
                                + "\\s+unique.*");
            }
        }
    }

    @Test
    void uuidEntityIdsAreNeverLeftAsVarcharInTheFinalMigrationContract() throws IOException {
        String migrations = migrationSources();

        assertThat(migrations).containsPattern("(?is).*alter\\s+column\\s+id\\s+type\\s+uuid.*");
        assertThat(migrations).containsPattern("(?is).*alter\\s+column\\s+vector_point_id\\s+type\\s+uuid.*");
        assertThat(migrations).contains("uk_trade_memories_trade_id unique (trade_id)");
        assertThat(migrations).contains("uk_trade_memories_vector_point_id unique (vector_point_id)");
    }

    private static List<Path> entitySources() throws IOException {
        try (Stream<Path> paths = Files.walk(JAVA_ROOT)) {
            return paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            return Pattern.compile("(?m)^\\s*@Entity\\b").matcher(Files.readString(path)).find();
                        } catch (IOException ex) {
                            throw new IllegalStateException("Unable to read " + path, ex);
                        }
                    })
                    .sorted()
                    .toList();
        }
    }

    private static String migrationSources() throws IOException {
        try (Stream<Path> paths = Files.list(MIGRATION_ROOT)) {
            StringBuilder sql = new StringBuilder();
            for (Path path : paths.filter(candidate -> candidate.getFileName().toString().matches("V\\d+__.*\\.sql"))
                    .sorted()
                    .toList()) {
                sql.append('\n').append(Files.readString(path));
            }
            return sql.toString();
        }
    }

    private static String tableName(String source, Path entity) {
        Matcher matcher = TABLE.matcher(source);
        assertThat(matcher.find()).as("@Table name for %s", entity).isTrue();
        return matcher.group(1);
    }

    private static String schemaForTable(String migrations, String table) {
        StringBuilder schema = new StringBuilder();
        Pattern create = Pattern.compile("(?is)create\\s+table\\s+(?:if\\s+not\\s+exists\\s+)?" + Pattern.quote(table) + "\\s*\\(");
        Matcher createMatcher = create.matcher(migrations);
        if (createMatcher.find()) {
            int openingParenthesis = migrations.indexOf('(', createMatcher.start());
            int depth = 0;
            for (int index = openingParenthesis; index < migrations.length(); index++) {
                char character = migrations.charAt(index);
                if (character == '(') {
                    depth++;
                } else if (character == ')' && --depth == 0) {
                    schema.append(migrations, createMatcher.start(), index + 1);
                    break;
                }
            }
        }

        Pattern alterations = Pattern.compile("(?is)(?:alter\\s+table\\s+(?:if\\s+exists\\s+)?" + Pattern.quote(table)
                + ".*?;|(?:create\\s+(?:unique\\s+)?index.*?\\s+on\\s+" + Pattern.quote(table) + "\\s*\\(.*?\\);))");
        Matcher alterMatcher = alterations.matcher(migrations);
        while (alterMatcher.find()) {
            schema.append('\n').append(alterMatcher.group());
        }
        if ("trade_memories".equals(table)) {
            schema.append('\n').append(migrations);
        }
        return schema.toString();
    }

    private static String attribute(String attributes, String name) {
        Matcher matcher = Pattern.compile("\\b" + name + "\\s*=\\s*\\\"([^\\\"]+)\\\"").matcher(attributes);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String attributeNumber(String attributes, String name) {
        Matcher matcher = Pattern.compile("\\b" + name + "\\s*=\\s*(\\d+)").matcher(attributes);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String snakeCase(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
