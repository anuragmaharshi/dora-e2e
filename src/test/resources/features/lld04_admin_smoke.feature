@LLD-04
Feature: Tenant Configuration & Platform Admin Portal — smoke tests

  # All API-level scenarios use RestAssured (no browser); @ui scenarios use Selenium.
  # The suite health gate in Hooks.@BeforeAll already verified /actuator/health before
  # any scenario runs.
  #
  # Test user: platform@dora.local / password  (seeded by V1_1_1 migration)
  # Bank user:  ops@dora.local / password       (seeded by V1_1_1 migration)
  #
  # Scenarios that require a live stack guard with PendingException if the admin
  # endpoints return 404 (stack not running / Spring profile not active).

  Background:
    Given the DORA stack is healthy
    And the platform admin user is authenticated

  # ---------------------------------------------------------------------------
  # AC-1 — PLATFORM_ADMIN can read and update tenant configuration
  # ---------------------------------------------------------------------------

  @AC-1
  Scenario: Platform admin reads tenant configuration
    When the platform admin calls GET /api/v1/admin/tenant
    Then the response status is 200
    And the tenant config response contains fields: legalName, lei, ncaName, ncaEmail, jurisdictionIso

  @AC-1 @AC-8
  Scenario: Platform admin updates tenant configuration and audit entry is recorded
    When the platform admin updates the tenant config with legalName "Nexus Bank" and ncaEmail "nca@dora.local"
    Then the response status is 200
    And the tenant config response contains legalName "Nexus Bank"
    And an audit entry is recorded for entity "TENANT_CONFIG" with action "UPDATE"

  # ---------------------------------------------------------------------------
  # AC-2 — PLATFORM_ADMIN can list, add, and archive critical services
  # ---------------------------------------------------------------------------

  @AC-2
  Scenario: Platform admin lists critical services
    When the platform admin calls GET /api/v1/admin/critical-services
    Then the response status is 200
    And the critical services response is a list

  @AC-2
  Scenario: Platform admin adds two critical services
    When the platform admin creates a critical service named "Online Banking"
    Then the response status is 201
    And the created critical service has name "Online Banking"
    When the platform admin creates a critical service named "Payment Processing"
    Then the response status is 201
    And the created critical service has name "Payment Processing"

  @AC-2
  Scenario: Platform admin archives a critical service
    Given a critical service named "Archive Target Service" exists
    When the platform admin archives the critical service
    Then the response status is 204

  # ---------------------------------------------------------------------------
  # AC-3 — PLATFORM_ADMIN can set client base count and see history
  # ---------------------------------------------------------------------------

  @AC-3
  Scenario: Platform admin reads client base history
    When the platform admin calls GET /api/v1/admin/client-base
    Then the response status is 200
    And the client base response is a list

  @AC-3
  Scenario: Platform admin sets client base count
    When the platform admin sets the client base count to 1200000 effective today
    Then the response status is 201
    And the client base entry has clientCount 1200000

  # ---------------------------------------------------------------------------
  # AC-4 — PLATFORM_ADMIN can read and configure NCA email settings
  # ---------------------------------------------------------------------------

  @AC-4
  Scenario: Platform admin reads NCA email configuration
    When the platform admin calls GET /api/v1/admin/nca-email
    Then the response status is 200
    And the NCA email response contains fields: sender, recipient, subjectTemplate

  @AC-4
  Scenario: Platform admin updates NCA email configuration
    When the platform admin updates the NCA email config with sender "noreply@dora.local" and recipient "nca@dora.local" and subjectTemplate "[DORA][{{incidentId}}] Initial Notification"
    Then the response status is 200
    And the NCA email response has sender "noreply@dora.local"
    And the NCA email response has recipient "nca@dora.local"

  # ---------------------------------------------------------------------------
  # AC-5 — Angular roleGuard redirects PLATFORM_ADMIN away from /incidents
  # ---------------------------------------------------------------------------

  # @known-bug dora-frontend #18: PLATFORM_ADMIN navigating to /incidents is NOT redirected to /403.
  # The Angular roleGuard at /incidents does not block PLATFORM_ADMIN role.
  # Browser stays at /incidents instead of redirecting.
  @AC-5 @ui @known-bug
  Scenario: Platform admin navigating to incidents is redirected to /403
    Given the platform admin is logged in via the browser
    When the browser navigates to /incidents
    Then the browser URL contains "/403"

  # ---------------------------------------------------------------------------
  # AC-6 — PLATFORM_ADMIN JWT on incident API endpoint returns 403
  # ---------------------------------------------------------------------------

  @AC-6
  Scenario: Platform admin JWT is rejected on incident API endpoint
    When the platform admin calls GET /api/v1/incidents with the platform admin JWT
    Then the response status is 403

  # ---------------------------------------------------------------------------
  # AC-7 — BANK_USER hitting /api/v1/admin/** returns 403
  # ---------------------------------------------------------------------------

  # @known-bug dora-api #22: Admin endpoints return 401 instead of 403 for wrong-role authenticated users.
  # Expected: 403 Forbidden (authenticated, insufficient role).
  # Actual: 401 Unauthorized (misleadingly implies not authenticated).

  @AC-7 @known-bug
  Scenario: Bank user is rejected from tenant config endpoint
    Given the bank user is authenticated
    When the bank user calls GET /api/v1/admin/tenant
    Then the response status is 403

  @AC-7 @known-bug
  Scenario: Bank user is rejected from critical services endpoint
    Given the bank user is authenticated
    When the bank user calls GET /api/v1/admin/critical-services
    Then the response status is 403

  @AC-7 @known-bug
  Scenario: Bank user is rejected from client base endpoint
    Given the bank user is authenticated
    When the bank user calls GET /api/v1/admin/client-base
    Then the response status is 403

  @AC-7 @known-bug
  Scenario: Bank user is rejected from NCA email endpoint
    Given the bank user is authenticated
    When the bank user calls GET /api/v1/admin/nca-email
    Then the response status is 403

  # ---------------------------------------------------------------------------
  # AC-8 — Tenant config mutation writes to audit_log (covered in AC-1 scenario above)
  # Explicit additional audit check for critical-service creation
  # ---------------------------------------------------------------------------

  @AC-8
  Scenario: Creating a critical service records an audit entry
    When the platform admin creates a critical service named "Audit Check Service"
    Then the response status is 201
    And an audit entry is recorded for entity "CRITICAL_SERVICE" with action "CREATE"
