package com.tradepilot;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyCheckSuppressionScopeTests {
    private static final Instant EXPIRY = Instant.parse("2026-11-30T23:59:59Z");
    private static final Set<ApprovedFinding> APPROVED_FINDINGS = Set.of(
            finding("pkg:maven/org.jetbrains.kotlin/kotlin-stdlib@1.9.25", "CVE-2026-53914"),
            finding("pkg:maven/org.springframework/spring-core@6.2.19", "CVE-2026-47884"),
            finding("pkg:maven/org.springframework/spring-core@6.2.19", "CVE-2026-47890"),
            finding("pkg:maven/org.springframework/spring-core@6.2.19", "CVE-2026-47891"),
            finding("pkg:maven/org.springframework/spring-core@6.2.19", "CVE-2026-47892"),
            finding("pkg:maven/org.springframework/spring-core@6.2.19", "CVE-2026-59313"),
            finding("pkg:maven/org.springframework/spring-core@6.2.19", "CVE-2026-59283"),
            finding("pkg:maven/org.springframework.security/spring-security-core@6.5.11", "CVE-2026-59270")
    );
    private static final Map<ApprovedFinding, String> APPROVED_PACKAGE_URL_REGEXES = Map.of(
            finding("pkg:maven/org.jetbrains.kotlin/kotlin-stdlib@1.9.25", "CVE-2026-53914"), "^pkg:maven/org\\.jetbrains\\.kotlin/kotlin-stdlib@1\\.9\\.25$",
            finding("pkg:maven/org.springframework/spring-core@6.2.19", "CVE-2026-47884"), "^pkg:maven/org\\.springframework/spring-core@6\\.2\\.19$",
            finding("pkg:maven/org.springframework/spring-core@6.2.19", "CVE-2026-47890"), "^pkg:maven/org\\.springframework/spring-core@6\\.2\\.19$",
            finding("pkg:maven/org.springframework/spring-core@6.2.19", "CVE-2026-47891"), "^pkg:maven/org\\.springframework/spring-core@6\\.2\\.19$",
            finding("pkg:maven/org.springframework/spring-core@6.2.19", "CVE-2026-47892"), "^pkg:maven/org\\.springframework/spring-core@6\\.2\\.19$",
            finding("pkg:maven/org.springframework/spring-core@6.2.19", "CVE-2026-59313"), "^pkg:maven/org\\.springframework/spring-core@6\\.2\\.19$",
            finding("pkg:maven/org.springframework/spring-core@6.2.19", "CVE-2026-59283"), "^pkg:maven/org\\.springframework/spring-core@6\\.2\\.19$",
            finding("pkg:maven/org.springframework.security/spring-security-core@6.5.11", "CVE-2026-59270"), "^pkg:maven/org\\.springframework\\.security/spring-security-core@6\\.5\\.11$"
    );

    @Test
    void suppressionsAreLimitedToTheEightApprovedFindingsAndExpire() throws Exception {
        List<Suppression> suppressions = readSuppressions();

        assertThat(suppressions).hasSize(8);
        assertThat(suppressions).allSatisfy(suppression -> {
            assertThat(suppression.until()).isEqualTo(EXPIRY);
            assertThat(suppression.regexEnabled()).isTrue();
            assertThat(suppression.packageUrlRegex()).startsWith("^").endsWith("$").doesNotContain(".*");
        });

        Set<ApprovedFinding> actualScope = suppressions.stream()
                .flatMap(suppression -> APPROVED_FINDINGS.stream()
                        .filter(finding -> suppression.cve().equals(finding.cve()))
                        .filter(finding -> suppression.packageUrlPattern().matcher(finding.packageUrl()).matches())
                        .map(finding -> finding))
                .collect(java.util.stream.Collectors.toSet());
        assertThat(actualScope).containsExactlyInAnyOrderElementsOf(APPROVED_FINDINGS);
        assertThat(suppressions.stream()
                .map(suppression -> new ApprovedFindingRegex(
                        APPROVED_FINDINGS.stream()
                                .filter(finding -> suppression.cve().equals(finding.cve()))
                                .filter(finding -> suppression.packageUrlPattern().matcher(finding.packageUrl()).matches())
                                .findFirst()
                                .orElseThrow(),
                        suppression.packageUrlRegex()))
                .collect(java.util.stream.Collectors.toMap(
                        scopedRegex -> scopedRegex.finding(),
                        scopedRegex -> scopedRegex.regex())))
                .containsExactlyInAnyOrderEntriesOf(APPROVED_PACKAGE_URL_REGEXES);

        for (ApprovedFinding finding : APPROVED_FINDINGS) {
            assertThat(isSuppressed(suppressions, finding.packageUrl(), finding.cve(), EXPIRY.minusSeconds(1)))
                    .as("approved finding %s %s", finding.cve(), finding.packageUrl())
                    .isTrue();
            assertThat(isSuppressed(suppressions, finding.packageUrl(), finding.cve(), EXPIRY.plusSeconds(1)))
                    .as("expired suppression %s %s", finding.cve(), finding.packageUrl())
                    .isFalse();
        }
    }

    @Test
    void unrelatedCvesAndChangedDependencyVersionsRemainUnsuppressed() throws Exception {
        List<Suppression> suppressions = readSuppressions();
        Instant beforeExpiry = EXPIRY.minusSeconds(1);

        assertThat(isSuppressed(suppressions, "pkg:maven/org.springframework/spring-core@6.2.20", "CVE-2026-47884", beforeExpiry)).isFalse();
        assertThat(isSuppressed(suppressions, "pkg:maven/org.springframework/spring-context@6.2.19", "CVE-2026-47884", beforeExpiry)).isFalse();
        assertThat(isSuppressed(suppressions, "pkg:maven/org.springframework/spring-core@6.2.19", "CVE-2099-00001", beforeExpiry)).isFalse();
        assertThat(isSuppressed(suppressions, "pkg:maven/com.example/unrelated-library@1.0.0", "CVE-2099-00002", beforeExpiry)).isFalse();
    }

    @Test
    void dependencyCheckWorkflowRetainsFailClosedGateAndReportUpload() throws Exception {
        String workflow = Files.readString(Path.of("..", ".github", "workflows", "security-scans.yml"));

        assertThat(workflow).contains(
                "-DfailBuildOnCVSS=8",
                "-DfailBuildOnUnusedSuppressionRule=true",
                "-DsuppressionFile=config/dependency-check-suppressions.xml",
                "if: always()",
                "path: backend/target/dependency-check-report.*"
        );
        assertThat(workflow).doesNotContain("continue-on-error");
    }

    private static boolean isSuppressed(List<Suppression> suppressions, String packageUrl, String cve, Instant at) {
        return suppressions.stream().anyMatch(suppression -> at.isBefore(suppression.until())
                && suppression.cve().equals(cve)
                && suppression.packageUrlPattern().matcher(packageUrl).matches());
    }

    private static List<Suppression> readSuppressions() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setNamespaceAware(true);

        Document document = factory.newDocumentBuilder()
                .parse(Path.of("config", "dependency-check-suppressions.xml").toFile());
        NodeList nodes = document.getElementsByTagNameNS("https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd", "suppress");
        List<Suppression> suppressions = new ArrayList<>();
        for (int index = 0; index < nodes.getLength(); index++) {
            Element element = (Element) nodes.item(index);
            Element packageUrl = (Element) element.getElementsByTagNameNS(element.getNamespaceURI(), "packageUrl").item(0);
            String packageUrlRegex = packageUrl.getTextContent();
            String cve = element.getElementsByTagNameNS(element.getNamespaceURI(), "cve").item(0).getTextContent();
            suppressions.add(new Suppression(Instant.parse(element.getAttribute("until")), cve, packageUrlRegex, Pattern.compile(packageUrlRegex), Boolean.parseBoolean(packageUrl.getAttribute("regex"))));
        }
        return suppressions;
    }

    private static ApprovedFinding finding(String packageUrl, String cve) {
        return new ApprovedFinding(packageUrl, cve);
    }

    private record ApprovedFinding(String packageUrl, String cve) {
    }

    private record Suppression(Instant until, String cve, String packageUrlRegex, Pattern packageUrlPattern, boolean regexEnabled) {
    }

    private record ApprovedFindingRegex(ApprovedFinding finding, String regex) {
    }
}
