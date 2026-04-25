package com.dora.e2e.steps;

import com.dora.e2e.clients.AuthApiClient;
import com.dora.e2e.pages.HomePage;
import com.dora.e2e.pages.LoginPage;
import com.dora.e2e.support.Config;
import com.dora.e2e.support.World;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for all Angular UI auth scenarios (Selenium WebDriver).
 *
 * <p>These steps cover AC-6 and AC-8:
 * <ul>
 *   <li>AC-6 — unauthenticated navigation redirects to /login; login form works</li>
 *   <li>AC-8 — token lives in sessionStorage (not localStorage); clearing it + refresh → /login</li>
 * </ul>
 *
 * <p>Every step that needs a browser reads {@link World#getDriver()} which is provisioned
 * by {@code Hooks.@Before(value="@ui")}. Selectors are kept in the page-object classes.
 */
public class UiAuthSteps {

    private final World world;

    public UiAuthSteps(World world) {
        this.world = world;
    }

    // ---- helpers ----

    private WebDriver driver() {
        WebDriver d = world.getDriver();
        assertThat(d)
                .as("WebDriver must be initialised (scenario must be tagged @ui)")
                .isNotNull();
        return d;
    }

    private WebDriverWait explicitWait() {
        return new WebDriverWait(driver(), Duration.ofSeconds(Config.WAIT_TIMEOUT_SECONDS));
    }

    private LoginPage loginPage() {
        return new LoginPage(driver());
    }

    private HomePage homePage() {
        return new HomePage(driver());
    }

    // -------------------------------------------------------------------------
    // Given
    // -------------------------------------------------------------------------

    @Given("the frontend is running at {string}")
    public void theFrontendIsRunningAt(String url) {
        // Stack health verified in Hooks.@BeforeAll; this step is for Gherkin readability.
        assertThat(Config.FRONTEND_BASE_URL)
                .as("Frontend base URL must match the scenario expectation")
                .isEqualTo(url);
    }

    @Given("I have logged in through the Angular UI as {string}")
    public void iHaveLoggedInThroughTheAngularUiAs(String email) {
        LoginPage page = loginPage();
        page.open();
        page.login(email, Config.DEFAULT_PASSWORD);

        // Wait for successful redirect away from /login
        homePage().waitForRedirectAwayFromLogin();
    }

    // -------------------------------------------------------------------------
    // When
    // -------------------------------------------------------------------------

    @When("I navigate to {string}")
    public void iNavigateTo(String url) {
        driver().get(url);
    }

    @When("I enter email {string} and password {string}")
    public void iEnterEmailAndPassword(String email, String password) {
        loginPage().enterEmail(email);
        loginPage().enterPassword(password);
    }

    @When("I click the submit button")
    public void iClickTheSubmitButton() {
        loginPage().clickSubmit();
    }

    @When("I clear sessionStorage")
    public void iClearSessionStorage() {
        homePage().clearSessionStorage();
    }

    @When("I refresh the page")
    public void iRefreshThePage() {
        homePage().refresh();
    }

    // -------------------------------------------------------------------------
    // Then / And
    // -------------------------------------------------------------------------

    @Then("I am redirected to /login")
    public void iAmRedirectedToLogin() {
        explicitWait().until(ExpectedConditions.urlContains("/login"));
        assertThat(driver().getCurrentUrl())
                .as("Browser should be on the /login page")
                .contains("/login");
    }

    @Then("I am on the /login page")
    public void iAmOnTheLoginPage() {
        // After a refresh, Angular re-evaluates the auth guard and redirects.
        explicitWait().until(ExpectedConditions.urlContains("/login"));
        assertThat(driver().getCurrentUrl())
                .as("After clearing sessionStorage and refreshing, browser should redirect to /login")
                .contains("/login");
    }

    @Then("I am redirected to the home page (not /login)")
    public void iAmRedirectedToTheHomePage() {
        homePage().waitForRedirectAwayFromLogin();
        assertThat(driver().getCurrentUrl())
                .as("After successful login the browser must navigate away from /login")
                .doesNotContain("/login");
    }

    @Then("sessionStorage contains key {string}")
    public void sessionStorageContainsKey(String key) {
        String value = homePage().getSessionStorageItem(key);
        assertThat(value)
                .as("sessionStorage must contain a non-empty value for key '%s'", key)
                .isNotBlank();
    }

    @And("localStorage does not contain key {string}")
    public void localStorageDoesNotContainKey(String key) {
        String value = homePage().getLocalStorageItem(key);
        assertThat(value)
                .as("localStorage must NOT contain key '%s' — token must be sessionStorage only", key)
                .isNull();
    }
}
