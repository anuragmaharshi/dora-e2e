package com.dora.e2e.support;

import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Scenario;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

/**
 * Suite-level and scenario-level hooks.
 *
 * <p>@BeforeAll — verifies that both the API and the frontend are reachable before
 * any scenario runs. Fails the entire suite with a clear message if either is not.
 *
 * <p>@Before(order=10) — seeds a WebDriver only when the scenario is tagged @ui.
 *
 * <p>@After — tears down the WebDriver (if open) and captures a screenshot on failure.
 */
public class Hooks {

    private final World world;

    public Hooks(World world) {
        this.world = world;
    }

    // -------------------------------------------------------------------------
    // Suite-level health gate — runs once before all scenarios
    // -------------------------------------------------------------------------

    @BeforeAll
    public static void verifyStackIsReachable() {
        checkApiHealth();
        checkFrontendRoot();
    }

    private static void checkApiHealth() {
        try {
            Response response = RestAssured
                    .given()
                    .baseUri(Config.API_BASE_URL)
                    .when()
                    .get("/actuator/health");

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Stack not reachable: GET " + Config.HEALTH_ENDPOINT +
                        " returned HTTP " + response.statusCode() +
                        ". Ensure the DORA API is running at " + Config.API_BASE_URL);
            }
        } catch (Exception e) {
            if (e instanceof IllegalStateException) throw e;
            throw new IllegalStateException(
                    "Stack not reachable: cannot connect to API at " + Config.API_BASE_URL +
                    ". Ensure the DORA stack is running. Root cause: " + e.getMessage(), e);
        }
    }

    private static void checkFrontendRoot() {
        try {
            Response response = RestAssured
                    .given()
                    .baseUri(Config.FRONTEND_BASE_URL)
                    .when()
                    .get("/");

            if (response.statusCode() < 200 || response.statusCode() >= 400) {
                throw new IllegalStateException(
                        "Stack not reachable: GET " + Config.FRONTEND_BASE_URL +
                        "/ returned HTTP " + response.statusCode() +
                        ". Ensure the Angular frontend is running at " + Config.FRONTEND_BASE_URL);
            }
        } catch (Exception e) {
            if (e instanceof IllegalStateException) throw e;
            throw new IllegalStateException(
                    "Stack not reachable: cannot connect to frontend at " + Config.FRONTEND_BASE_URL +
                    ". Ensure the Angular frontend is running. Root cause: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Scenario-level hooks
    // -------------------------------------------------------------------------

    /**
     * Spin up a WebDriver for scenarios tagged @ui.
     * API-only scenarios do not pay the browser startup cost.
     */
    @Before(value = "@ui", order = 10)
    public void openBrowser(Scenario scenario) {
        WebDriver driver = DriverFactory.createChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0)); // explicit waits only
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        world.setDriver(driver);
    }

    /**
     * Tear down the WebDriver after every scenario.
     * Captures a screenshot on failure if a driver is open.
     */
    @After(order = 10)
    public void closeBrowser(Scenario scenario) {
        WebDriver driver = world.getDriver();
        if (driver != null) {
            if (scenario.isFailed()) {
                try {
                    byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                    scenario.attach(screenshot, "image/png", "screenshot-on-failure");
                } catch (Exception ignored) {
                    // Screenshot capture is best-effort; never fail the teardown because of it.
                }
            }
            driver.quit();
            world.setDriver(null);
        }
    }
}
