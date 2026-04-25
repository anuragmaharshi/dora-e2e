@LLD-02 @ui
Feature: Angular Authentication Flow
  As a DORA platform user
  I need the Angular app to guard routes, persist sessions in sessionStorage only,
  and redirect to /login when my session is absent or cleared
  So that unauthenticated access is prevented at the UI layer

  # All scenarios in this feature use Selenium WebDriver (headless Chrome).
  # Scenarios are tagged @ui so that Hooks.@Before spins up the browser.
  # The stack health gate in Hooks.@BeforeAll verifies the frontend is reachable.

  # ---------------------------------------------------------------------------
  # AC-6: Unauthenticated users are redirected to /login by authGuard
  # ---------------------------------------------------------------------------

  @AC-6
  Scenario: Unauthenticated user navigating to the app root is redirected to /login
    Given the frontend is running at "http://localhost:4200"
    When I navigate to "http://localhost:4200"
    Then I am redirected to /login

  @AC-6
  Scenario: User can log in through the Angular login form and reach the home page
    Given the frontend is running at "http://localhost:4200"
    When I navigate to "http://localhost:4200/login"
    And I enter email "ops@dora.local" and password "ChangeMe!23"
    And I click the submit button
    Then I am redirected to the home page (not /login)

  # ---------------------------------------------------------------------------
  # AC-8: Token storage — sessionStorage only, never localStorage
  # D-LLD02-2: Token storage: sessionStorage only (tab-scoped); never localStorage
  # ---------------------------------------------------------------------------

  @AC-8
  Scenario: After login, token is in sessionStorage and absent from localStorage
    Given the frontend is running at "http://localhost:4200"
    And I have logged in through the Angular UI as "ops@dora.local"
    Then sessionStorage contains key "dora_token"
    And localStorage does not contain key "dora_token"

  @AC-8
  Scenario: Clearing sessionStorage and refreshing redirects to /login
    Given the frontend is running at "http://localhost:4200"
    And I have logged in through the Angular UI as "ops@dora.local"
    When I clear sessionStorage
    And I refresh the page
    Then I am on the /login page
