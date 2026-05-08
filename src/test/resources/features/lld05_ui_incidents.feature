@LLD-05
Feature: Incident Logging — UI scenarios

  # All scenarios use Selenium (tagged @ui — browser will be spun up by Hooks.@Before).
  # Routes:
  #   /incidents/new       → IncidentCreateComponent (roleGuard: OPS_ANALYST, INCIDENT_MANAGER, etc.)
  #   /incidents/:id       → IncidentDetailComponent
  #
  # Test user: ops@dora.local / password (OPS_ANALYST role, seeded by V1_1_1 migration)
  # Stack assumed running at http://localhost:4200 (frontend) and http://localhost:8080 (API).

  Background:
    Given the DORA stack is healthy
    And the ops analyst is logged in via the browser

  # ---------------------------------------------------------------------------
  # AC-1 UI — OPS_ANALYST creates an incident via the form and sees it confirmed
  # ---------------------------------------------------------------------------

  @AC-1 @ui
  Scenario: OPS_ANALYST submits the incident creation form and sees the new incident ID
    When the ops analyst navigates to the incident creation page
    And fills in the incident title "UI Browser Outage Test"
    And selects severity "HIGH"
    And submits the incident form
    Then the browser navigates to the incident detail page
    And the incident detail page shows an incident ID matching "INC-"

  # ---------------------------------------------------------------------------
  # AC-3 UI — OPS_ANALYST uploads an attachment via the AttachmentUploaderComponent
  # ---------------------------------------------------------------------------

  @AC-3 @ui
  Scenario: OPS_ANALYST uploads an attachment via the UI and sees status READY
    Given an incident has been created via the API by the ops analyst with title "UI Attachment Upload Test"
    When the ops analyst navigates to the incident detail page for that incident
    And uploads a file named "test-evidence.txt" via the attachment uploader
    Then the attachment appears in the incident detail with status "READY"

  # ---------------------------------------------------------------------------
  # AC-6 UI — OPS_ANALYST views full incident detail including attachments and assets
  # ---------------------------------------------------------------------------

  @AC-6 @ui
  Scenario: OPS_ANALYST views full incident detail with attachments and linked assets
    Given an incident with an attachment and an asset exists via the API
    When the ops analyst navigates to the incident detail page for that incident
    Then the incident detail page shows the attachments section
    And the incident detail page shows the assets section
    And the incident detail page shows the linked services section
