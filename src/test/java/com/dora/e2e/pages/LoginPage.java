package com.dora.e2e.pages;

import com.dora.e2e.support.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page object for the Angular login page at /login.
 *
 * <p>All selectors are declared as constants here; step definitions never reference
 * raw {@link By} locators directly.
 */
public class LoginPage {

    private static final By EMAIL_INPUT    = By.cssSelector("input[type='email'], input[formcontrolname='username'], input[name='username'], input[name='email']");
    private static final By PASSWORD_INPUT = By.cssSelector("input[type='password']");
    private static final By SUBMIT_BUTTON  = By.cssSelector("button[type='submit'], button.login-btn, button.submit-btn");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(Config.WAIT_TIMEOUT_SECONDS));
    }

    /** Navigate directly to /login. */
    public void open() {
        driver.get(Config.FRONTEND_BASE_URL + "/login");
        waitForPageLoad();
    }

    /** Wait until the email input is visible on the login form. */
    public void waitForPageLoad() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_INPUT));
    }

    /** Type the user's email/username into the email field. */
    public void enterEmail(String email) {
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(EMAIL_INPUT));
        input.clear();
        input.sendKeys(email);
    }

    /** Type the password into the password field. */
    public void enterPassword(String password) {
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(PASSWORD_INPUT));
        input.clear();
        input.sendKeys(password);
    }

    /** Click the form's submit button. */
    public void clickSubmit() {
        wait.until(ExpectedConditions.elementToBeClickable(SUBMIT_BUTTON)).click();
    }

    /**
     * Convenience: fill the form and submit in one call.
     *
     * @param email    user email
     * @param password raw password
     */
    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickSubmit();
    }

    /** @return the current browser URL */
    public String currentUrl() {
        return driver.getCurrentUrl();
    }
}
