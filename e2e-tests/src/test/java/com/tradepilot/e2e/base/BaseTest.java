package com.tradepilot.e2e.base;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class BaseTest {
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected String baseUrl;

    @RegisterExtension
    FailureArtifactWatcher failureArtifacts = new FailureArtifactWatcher();

    @BeforeEach
    void setUp() {
        baseUrl = E2eConfig.baseUrl();
        ChromeOptions options = new ChromeOptions();
        if (E2eConfig.headless()) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1440,1000", "--disable-dev-shm-usage", "--no-sandbox");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        failureArtifacts.driver = driver;
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        if (driver == null) return;
        try {
            driver.quit();
        } finally {
            driver = null;
            failureArtifacts.driver = null;
        }
    }

    protected String requiredEnv(String name) {
        return E2eConfig.requiredEnv(name);
    }

    protected void rejectProductionMutation(String testName) {
        E2eConfig.rejectProductionMutation(baseUrl, testName);
    }

    public static class FailureArtifactWatcher implements TestWatcher {
        private WebDriver driver;

        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            if (!(driver instanceof TakesScreenshot)) return;
            try {
                Path output = Path.of("test-output");
                Files.createDirectories(output);
                String className = context.getRequiredTestClass().getSimpleName();
                String methodName = context.getRequiredTestMethod().getName();
                String unique = className + "-" + methodName + "-" + System.currentTimeMillis() + ".png";
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                Files.write(output.resolve(unique), screenshot);
                Files.writeString(output.resolve(unique.replace(".png", ".txt")),
                        "Test failed: " + className + "." + methodName + System.lineSeparator()
                                + "Reason: " + sanitize(cause.getMessage()) + System.lineSeparator());
            } catch (Exception ignored) {
                // Failure artifacts must not hide the original test failure.
            }
        }

        private static String sanitize(String value) {
            if (value == null) return "";
            return value.replaceAll("(?i)(bearer\\s+)[A-Za-z0-9._~-]+", "$1[REDACTED]")
                    .replaceAll("(?i)(token[=:]\\s*)[A-Za-z0-9._~-]+", "$1[REDACTED]");
        }
    }
}
