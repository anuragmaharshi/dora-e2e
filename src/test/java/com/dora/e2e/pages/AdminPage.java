package com.dora.e2e.pages;

import com.dora.e2e.support.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page object for the Angular admin section and the /403 access-denied page.
 *
 * <p>Used by AC-5: PLATFORM_ADMIN navigating to /incidents is redirected to /403
 * by the Angular roleGuard.
 *
 * <p>All {@link By} selectors are declared as constants in this class.
 * Step definitions call user-level action methods; they never reference selectors directly.
 */
public class AdminPage {

    // ---- Selectors ----

    /** Login form email input — matches the Angular login component. */
    private static final By EMAIL_INPUT =
            By.cssSelector("input[type='email'], input[formcontrolname='username'], input[name='email']");

    /** Login form password input. */
    private static final By PASSWORD_INPUT =
            By.cssSelector("input[type='password']");

    /** Login form submit button. */
    private static final By SUBMIT_BUTTON =
            By.cssSelector("button[type='submit'], button.login-btn, button.submit-btn");

    /**
     * Any element that indicates a successful post-login landing page load.
     * After login the app typically navigates to /dashboard or /admin.
     * We wait for the URL to change away from /login rather than for a specific element.
     */
    private static final By NAV_ELEMENT =
            By.cssSelector("nav, app-navbar, .navbar, .sidebar, app-root");

    /**
     * An element that should appear on the /403 access-denied page.
     * Falls back to URL check only if no specific element is found.
     */
    private static final By FORBIDDEN_INDICATOR =
            By.cssSelector(".forbidden, .access-denied, [data-testid='403'], h1, h2");

    // ---- Infrastructure ----

    private final WebDriver driver;
    private final WebDriverWait wait;

    public AdminPage(WebDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(Config.WAIT_TIMEOUT_SECONDS));
    }

    // ---- Actions ----

    /**
     * Logs in as the given user via the Angular login form and waits for navigation
     * away from /login to complete.
     *
     * @param email    user email address
     * @param password raw password
     */
    public void loginAs(String email, String password) {
        driver.get(Config.FRONTEND_BASE_URL + "/login");

        // Wait for the email field to be visible
        WebElement emailInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(EMAIL_INPUT));
        emailInput.clear();
        emailInput.sendKeys(email);

        WebElement passwordInput = wait.until(
                ExpectedConditions.elementToBeClickable(PASSWORD_INPUT));
        passwordInput.clear();
        passwordInput.sendKeys(password);

        wait.until(ExpectedConditions.elementToBeClickable(SUBMIT_BUTTON)).click();

        // Wait until the URL is no longer the login page — indicates successful auth
        wait.until(driver -> !driver.getCurrentUrl().contains("/login"));
    }

    /**
     * Navigates the browser to the given path under the configured frontend base URL.
     *
     * @param path path starting with "/" (e.g. "/incidents")
     */
    public void navigateTo(String path) {
        driver.get(Config.FRONTEND_BASE_URL + path);
    }

    /**
     * Waits until the current browser URL contains the given fragment, then returns
     * the current URL.
     *
     * <p>Used to assert on roleGuard redirects (e.g., to "/403").
     *
     * @param fragment URL fragment to wait for (e.g. "/403")
     * @return the current URL once the fragment appears
     * @throws org.openqa.selenium.TimeoutException if the fragment does not appear
     *         within {@link Config#WAIT_TIMEOUT_SECONDS} seconds
     */
    public String waitForUrlContaining(String fragment) {
        wait.until(ExpectedConditions.urlContains(fragment));
        return driver.getCurrentUrl();
    }

    /**
     * Returns the current browser URL without waiting.
     *
     * @return current URL string
     */
    public String currentUrl() {
        return driver.getCurrentUrl();
    }
}
