@LLD-03
Feature: Audit Trail Core — smoke tests

  # All scenarios in this file are pure API-level (no browser).
  # The stack health gate in Hooks.@BeforeAll already verified /actuator/health before
  # any scenario runs.
  #
  # Base URL is read from system property "base.url" (default: http://localhost:8080).
  # DB connection string is read from system property "db.url"
  #   (default: jdbc:postgresql://localhost:5432/dora).
  #
  # Scenario 4 (immutability via DB) requires direct PostgreSQL access.
  # If the DB is unreachable, that scenario is tagged @Pending and is skipped without
  # failing the suite.

  Background:
    Given the DORA stack is healthy
    And a unique probe entity id is generated for this scenario

  @AC-1 @AC-4
  Scenario: Seed a probe audit row via the test endpoint
    When the test audit-emit endpoint is called with entityType "PROBE" and action "SYSTEM"
    Then the response status is one of 200 or 201

  @AC-4
  Scenario: Permitted role reads the audit log
    Given the test audit row for the probe entity exists
    And the user "compliance@dora.local" has logged in with password "password"
    When they call GET /api/v1/audit with entity "PROBE" and the probe entity id
    Then the response status is 200
    And the response content array is not empty

  @AC-5
  Scenario: Denied role receives 403
    Given the test audit row for the probe entity exists
    And the user "platform@dora.local" has logged in with password "password"
    When they call GET /api/v1/audit with entity "PROBE" and the probe entity id
    Then the response status is 403

  @AC-2
  Scenario: Audit entry fields are all non-null
    Given the test audit row for the probe entity exists
    And the user "compliance@dora.local" has logged in with password "password"
    When they call GET /api/v1/audit with entity "PROBE" and the probe entity id
    Then the response status is 200
    And the response content array is not empty
    And the first audit entry has non-null fields: entityType, entityId, action, actorUsername, createdAt

  @AC-2 @AC-3 @db-required
  Scenario: Audit row is immutable — UPDATE is rejected by the DB trigger
    Given the test audit row for the probe entity exists
    When a direct SQL UPDATE attempts to change the action to "TAMPERED" for the probe entity
    Then the UPDATE is rejected or affects zero rows
