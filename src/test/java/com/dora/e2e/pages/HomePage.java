package com.dora.e2e.pages;

import com.dora.e2e.support.Config;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page object representing the post-login home/dashboard view.
 *
 * <p>After a successful login Angular routes away from /login. This page object
 * provides helpers for verifying post-login state and for interacting with
 * session storage.
 */
public class HomePage {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final JavascriptExecutor js;

    public HomePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(Config.WAIT_TIMEOUT_SECONDS));
        this.js = (JavascriptExecutor) driver;
    }

    /** @return the path portion of the current URL (everything after the origin). */
    public String currentPath() {
        String url = driver.getCurrentUrl();
        // strip scheme + host + port to get just the path
        try {
            java.net.URI uri = new java.net.URI(url);
            String path = uri.getPath();
            return (path == null || path.isEmpty()) ? "/" : path;
        } catch (Exception e) {
            return url;
        }
    }

    /** @return the full current URL */
    public String currentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * Wait until the browser URL no longer contains "/login".
     * Used to confirm that post-login navigation has completed.
     */
    public void waitForRedirectAwayFromLogin() {
        wait.until(driver -> !driver.getCurrentUrl().contains("/login"));
    }

    // ---- sessionStorage helpers ----

    /**
     * @param key the sessionStorage key to look up
     * @return the value, or null if not present
     */
    public String getSessionStorageItem(String key) {
        Object value = js.executeScript("return sessionStorage.getItem(arguments[0]);", key);
        return value == null ? null : value.toString();
    }

    /**
     * @param key the localStorage key to look up
     * @return the value, or null if not present
     */
    public String getLocalStorageItem(String key) {
        Object value = js.executeScript("return localStorage.getItem(arguments[0]);", key);
        return value == null ? null : value.toString();
    }

    /** Clear all entries from sessionStorage. */
    public void clearSessionStorage() {
        js.executeScript("sessionStorage.clear();");
    }

    /** Hard-reload the current page (equivalent to F5). */
    public void refresh() {
        driver.navigate().refresh();
    }

    /** Navigate to the application root. */
    public void navigateToRoot() {
        driver.get(Config.FRONTEND_BASE_URL + "/");
    }
}
