@LLD-05
Feature: Incident Logging — API scenarios

  # All scenarios use RestAssured (no browser).
  # The suite health gate in Hooks.@BeforeAll already verified /actuator/health.
  #
  # Test users (seeded by V1_1_1 migration, password = "password"):
  #   ops@dora.local         — OPS_ANALYST
  #   incident@dora.local    — INCIDENT_MANAGER
  #   platform@dora.local    — PLATFORM_ADMIN (must get 403 on all incident endpoints)
  #   compliance@dora.local  — COMPLIANCE_OFFICER (used to verify audit log)
  #
  # Each scenario creates its own test data — scenarios are parallel-safe.

  Background:
    Given the DORA stack is healthy
    And the ops analyst is authenticated for incidents

  # ---------------------------------------------------------------------------
  # AC-1 — OPS_ANALYST can create an incident; ID follows INC-YYYYMMDD-NNNN;
  #         detection_datetime is server-stamped; INCIDENT_CREATED audit written.
  # ---------------------------------------------------------------------------

  @AC-1
  Scenario: OPS_ANALYST creates a valid incident and receives a structured Incident ID
    When the ops analyst creates an incident with title "Core Banking Outage" and severity "HIGH"
    Then the response status is 201
    And the incident ID matches the pattern "INC-\\d{8}-\\d{4}"
    And the incident response contains a non-blank "detectionDatetime" field
    And the incident response contains a non-blank "id" field

  @AC-1
  Scenario: INCIDENT_MANAGER can also create a valid incident
    Given the incident manager is authenticated for incidents
    When the incident manager creates an incident with title "Payment Gateway Failure" and severity "MEDIUM"
    Then the response status is 201
    And the incident ID matches the pattern "INC-\\d{8}-\\d{4}"

  @AC-1
  Scenario: INCIDENT_CREATED audit entry is written after incident creation
    When the ops analyst creates an incident with title "Audit Probe Incident" and severity "LOW"
    Then the response status is 201
    And an INCIDENT_CREATED audit entry is recorded for the new incident

  # ---------------------------------------------------------------------------
  # AC-2 — Mutating detection_datetime on an existing incident returns HTTP 422
  # ---------------------------------------------------------------------------

  @AC-2
  Scenario: Attempting to change detection_datetime on an existing incident is rejected
    Given an existing incident created by the ops analyst with title "Immutable Detection Test"
    When the ops analyst attempts to update the incident detection_datetime
    Then the response status is 422

  # ---------------------------------------------------------------------------
  # AC-3 — Attachment presigned URL flow:
  #         POST /incidents/{id}/attachments → presigned URL → PUT to MinIO → POST .../complete → READY
  # ---------------------------------------------------------------------------

  @AC-3
  Scenario: OPS_ANALYST requests a presigned upload URL and the attachment moves to READY
    Given an existing incident created by the ops analyst with title "Attachment Flow Test"
    When the ops analyst requests a presigned upload URL for file "evidence.pdf" with content type "application/pdf"
    Then the response status is 201
    And the presigned upload response contains a non-blank "uploadUrl" field
    And the presigned upload response contains a non-blank "attachmentId" field
    When the ops analyst uploads the file bytes to the presigned URL
    And the ops analyst marks the attachment upload as complete
    Then the response status is 200
    And the attachment status is "READY"

  # ---------------------------------------------------------------------------
  # AC-4 — Creating an incident with an inactive (archived) service ID is rejected
  # ---------------------------------------------------------------------------

  @AC-4
  Scenario: Incident creation with an inactive service ID is rejected
    Given an archived critical service exists
    When the ops analyst creates an incident that links to the archived service
    Then the response status is 422

  @AC-4
  Scenario: Incident creation with an active service ID is accepted
    Given an active critical service exists
    When the ops analyst creates an incident that links to the active service
    Then the response status is 201

  # ---------------------------------------------------------------------------
  # AC-5 — POST /incidents/{id}/assets records a linked ICT asset
  # ---------------------------------------------------------------------------

  @AC-5
  Scenario: OPS_ANALYST links an ICT asset to an incident
    Given an existing incident created by the ops analyst with title "Asset Link Test"
    When the ops analyst links asset named "Core Router" of type "HARDWARE" to the incident
    Then the response status is 201
    And the asset response contains name "Core Router"
    And the asset response contains type "HARDWARE"

  # ---------------------------------------------------------------------------
  # AC-6 — GET /incidents/{id} as bank role returns full detail
  #         (attachments + linked services + assets)
  # ---------------------------------------------------------------------------

  @AC-6
  Scenario: Bank user retrieves full incident detail including attachments and assets
    Given an incident with an attachment and an asset created by the ops analyst
    When the ops analyst retrieves the incident detail
    Then the response status is 200
    And the incident detail contains an "attachments" list
    And the incident detail contains an "assets" list
    And the incident detail contains a non-blank "incidentId" field

  # ---------------------------------------------------------------------------
  # AC-7 — Non-major incident has no special flag; behaves identically to major
  # ---------------------------------------------------------------------------

  @AC-7
  Scenario: Non-major incident is created and retrieved without error
    When the ops analyst creates an incident with title "Minor Connectivity Blip" and severity "LOW"
    Then the response status is 201
    And the incident ID matches the pattern "INC-\\d{8}-\\d{4}"
    When the ops analyst retrieves the last created incident
    Then the response status is 200

  # ---------------------------------------------------------------------------
  # AC-8 — PLATFORM_ADMIN receives HTTP 403 on ALL incident endpoints
  # ---------------------------------------------------------------------------

  @AC-8
  Scenario: PLATFORM_ADMIN is blocked from POST /api/v1/incidents
    Given the platform admin is authenticated for the incident check
    When the platform admin attempts to POST /api/v1/incidents
    Then the response status is 403

  @AC-8
  Scenario: PLATFORM_ADMIN is blocked from GET /api/v1/incidents
    Given the platform admin is authenticated for the incident check
    When the platform admin attempts to GET /api/v1/incidents
    Then the response status is 403

  @AC-8
  Scenario: PLATFORM_ADMIN is blocked from GET /api/v1/incidents/{id}
    Given the platform admin is authenticated for the incident check
    And an existing incident ID is known from a prior ops analyst call
    When the platform admin attempts to GET that incident by ID
    Then the response status is 403
