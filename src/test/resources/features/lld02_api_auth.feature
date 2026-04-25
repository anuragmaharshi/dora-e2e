@LLD-02
Feature: API Authentication and RBAC
  As the DORA platform security model
  I need the /api/v1/auth endpoints to enforce JWT issuance and role-based access
  So that only authenticated, authorised users can reach protected resources

  # All scenarios in this feature use RestAssured — no browser is required.
  # The stack health gate in Hooks.@BeforeAll verifies the API is reachable
  # before any scenario runs.

  # ---------------------------------------------------------------------------
  # AC-1: Valid login returns a signed JWT
  # ---------------------------------------------------------------------------

  @AC-1
  Scenario: Valid login returns JWT with token and expiry
    Given the API is running at "http://localhost:8080"
    When I POST /api/v1/auth/login with email "ops@dora.local" and password "ChangeMe!23"
    Then the response status is 200
    And the response body contains a non-empty "token" field
    And the response body contains "expiresAt"

  # ---------------------------------------------------------------------------
  # AC-2: Authenticated user can fetch their own profile
  # ---------------------------------------------------------------------------

  @AC-2
  Scenario: Authenticated user can fetch their profile via /me
    Given the API is running at "http://localhost:8080"
    And I have logged in as "ops@dora.local" with password "ChangeMe!23"
    When I GET /api/v1/auth/me with the JWT
    Then the response status is 200
    And the response body contains email "ops@dora.local"
    And the response body contains role "OPS_ANALYST"

  # ---------------------------------------------------------------------------
  # AC-3: Request without bearer token is rejected
  # ---------------------------------------------------------------------------

  @AC-3
  Scenario: Request without a bearer token is rejected with 401
    Given the API is running at "http://localhost:8080"
    When I GET /api/v1/auth/me without any Authorization header
    Then the response status is 401

  # ---------------------------------------------------------------------------
  # AC-4: RBAC enforcement — PLATFORM_ADMIN blocked from incident data
  # BR-011 / NFR-009: service-company staff must not see bank incident data
  # ---------------------------------------------------------------------------

  @AC-4
  Scenario: PLATFORM_ADMIN is blocked from the incident probe endpoint
    Given the API is running at "http://localhost:8080"
    And I have logged in as "platform@dora.local" with password "ChangeMe!23"
    When I GET /api/v1/incidents/_probe with the JWT
    Then the response status is 403

  @AC-4
  Scenario: OPS_ANALYST (bank role) can access the incident probe endpoint
    Given the API is running at "http://localhost:8080"
    And I have logged in as "ops@dora.local" with password "ChangeMe!23"
    When I GET /api/v1/incidents/_probe with the JWT
    Then the response status is 200

  # ---------------------------------------------------------------------------
  # AC-5: Wrong password returns 401 with a generic, non-leaking message
  # ---------------------------------------------------------------------------

  @AC-5
  Scenario: Wrong password returns 401 with a generic error message
    Given the API is running at "http://localhost:8080"
    When I POST /api/v1/auth/login with email "ops@dora.local" and password "WrongPassword!"
    Then the response status is 401
    And the response body "message" field does not contain "password"
    And the response body "message" field does not contain "stack"
