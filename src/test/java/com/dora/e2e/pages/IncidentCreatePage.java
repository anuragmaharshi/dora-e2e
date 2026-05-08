package com.dora.e2e.pages;

import com.dora.e2e.support.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page object for the Angular IncidentCreateComponent at /incidents/new.
 *
 * <p>All {@link By} selectors are declared as constants here.
 * Step definitions call user-level action methods; they never reference raw selectors.
 */
public class IncidentCreatePage {

    // ---- Selectors --------------------------------------------------------

    /** Title input field on the incident creation form. */
    private static final By TITLE_INPUT =
            By.cssSelector("input[formcontrolname='title'], input[name='title'], input[id='title']");

    /**
     * Severity selector — Angular mat-select, native select, or custom dropdown.
     * Tries native select first; falls back to Angular Material pattern in actions below.
     */
    private static final By SEVERITY_SELECT =
            By.cssSelector("select[formcontrolname='severity'], mat-select[formcontrolname='severity']");

    /** Submit / Save button on the incident creation form. */
    private static final By SUBMIT_BUTTON =
            By.cssSelector("button[type='submit'], button.submit-btn, button[data-testid='submit-incident']");

    /**
     * An element that indicates the form page has fully loaded.
     * The title input appearing is a reliable signal.
     */
    private static final By PAGE_LOADED_SIGNAL = TITLE_INPUT;

    // ---- Infrastructure ---------------------------------------------------

    private final WebDriver driver;
    private final WebDriverWait wait;

    public IncidentCreatePage(WebDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(Config.WAIT_TIMEOUT_SECONDS));
    }

    // ---- Actions ----------------------------------------------------------

    /**
     * Navigate directly to /incidents/new.
     * The caller must have already logged in so the roleGuard allows access.
     */
    public void open() {
        driver.get(Config.FRONTEND_BASE_URL + "/incidents/new");
        waitForPageLoad();
    }

    /** Wait until the title input is visible — indicates the form is ready. */
    public void waitForPageLoad() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(PAGE_LOADED_SIGNAL));
    }

    /**
     * Type the incident title into the title field.
     *
     * @param title incident title text
     */
    public void enterTitle(String title) {
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(TITLE_INPUT));
        input.clear();
        input.sendKeys(title);
    }

    /**
     * Select a severity option.
     *
     * <p>Handles both native {@code <select>} elements and Angular Material
     * {@code <mat-select>} components.
     *
     * @param severity severity value (e.g. "HIGH", "MEDIUM", "LOW")
     */
    public void selectSeverity(String severity) {
        WebElement severityEl = wait.until(ExpectedConditions.elementToBeClickable(SEVERITY_SELECT));
        String tagName = severityEl.getTagName();

        if ("select".equalsIgnoreCase(tagName)) {
            // Native HTML select
            new Select(severityEl).selectByVisibleText(severity);
        } else {
            // Angular Material mat-select: click to open overlay, then pick option
            severityEl.click();
            By optionLocator = By.cssSelector(
                    "mat-option[value='" + severity + "'], " +
                    "mat-option[data-value='" + severity + "']");
            wait.until(ExpectedConditions.visibilityOfElementLocated(optionLocator)).click();
        }
    }

    /**
     * Click the form submit button.
     * After this call the browser should navigate to the new incident's detail page.
     */
    public void submit() {
        wait.until(ExpectedConditions.elementToBeClickable(SUBMIT_BUTTON)).click();
    }

    /**
     * Convenience: fill the form with title and severity, then submit.
     *
     * @param title    incident title
     * @param severity severity value
     */
    public void createIncident(String title, String severity) {
        enterTitle(title);
        selectSeverity(severity);
        submit();
    }

    /** @return the current browser URL */
    public String currentUrl() {
        return driver.getCurrentUrl();
    }
}
