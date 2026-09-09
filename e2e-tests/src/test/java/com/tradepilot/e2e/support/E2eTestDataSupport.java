package com.tradepilot.e2e.support;

import com.tradepilot.e2e.base.E2eConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class E2eTestDataSupport {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static E2eTestData cached;

    private E2eTestDataSupport() {
    }

    public static synchronized E2eTestData prepare() {
        if (cached != null) {
            return cached;
        }

        String apiBaseUrl = E2eConfig.apiBaseUrl();
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiBaseUrl + "/dev/e2e/prepare"))
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Unable to prepare deterministic Selenium data. POST "
                        + apiBaseUrl + "/dev/e2e/prepare returned HTTP " + response.statusCode()
                        + ". Start the backend with DEV_AUTH_ENABLED=true and DEV_TOOLS_ENABLED=true.");
            }
            cached = parse(response.body());
            return cached;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to reach TradePilot backend at " + apiBaseUrl
                    + " while preparing deterministic Selenium data.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while preparing deterministic Selenium data.", ex);
        }
    }

    private static E2eTestData parse(String json) {
        return new E2eTestData(
                userData(section(json, "proUser")),
                userData(section(json, "freeUser")),
                userData(section(json, "unverifiedUser"))
        );
    }

    private static E2eUserData userData(String json) {
        return new E2eUserData(
                longField(json, "userId"),
                stringField(json, "email"),
                stringField(json, "password"),
                longField(json, "primaryAccountId"),
                longField(json, "secondaryAccountId"),
                longField(json, "tradeId")
        );
    }

    private static String section(String json, String name) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\\{([^}]*)}").matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Missing '" + name + "' in E2E setup response.");
        }
        return matcher.group(1);
    }

    private static long longField(String json, String name) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*(\\d+)").matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Missing numeric field '" + name + "' in E2E setup response.");
        }
        return Long.parseLong(matcher.group(1));
    }

    private static String stringField(String json, String name) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Missing string field '" + name + "' in E2E setup response.");
        }
        return matcher.group(1);
    }
}
