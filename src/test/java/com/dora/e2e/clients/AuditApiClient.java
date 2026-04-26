package com.dora.e2e.clients;

import com.dora.e2e.support.Config;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Thin RestAssured wrapper for the LLD-03 audit-related API endpoints.
 *
 * <ul>
 *   <li>POST /api/v1/_test/audit-emit  — test profile only; seeds an audit row.</li>
 *   <li>GET  /api/v1/audit             — read-only audit query (paginated).</li>
 * </ul>
 *
 * All methods return the raw {@link Response} so step definitions can assert on it.
 */
public class AuditApiClient {

    private RequestSpecification baseSpec() {
        return RestAssured
                .given()
                .baseUri(Config.API_BASE_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    private RequestSpecification authedSpec(String jwt) {
        return baseSpec()
                .header("Authorization", "Bearer " + jwt);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/_test/audit-emit
    // -------------------------------------------------------------------------

    /**
     * Seeds a single audit row for an arbitrary entity via the test-profile endpoint.
     *
     * <p>This endpoint is only active when the Spring profile {@code test} is enabled.
     * If the stack is running without that profile, this call returns 404 — callers
     * should mark the scenario {@code @Pending} rather than failing the suite.
     *
     * @param entityType the entity type string (e.g. {@code "PROBE"})
     * @param entityId   UUID string of the entity to audit
     * @param action     the action string (e.g. {@code "SYSTEM"})
     * @return full RestAssured response
     */
    public Response emitAuditRow(String entityType, String entityId, String action) {
        String body = String.format(
                "{\"entityType\":\"%s\",\"entityId\":\"%s\",\"action\":\"%s\"}",
                entityType, entityId, action);

        return baseSpec()
                .body(body)
                .when()
                .post("/api/v1/_test/audit-emit");
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/audit
    // -------------------------------------------------------------------------

    /**
     * Queries the audit log for a specific entity, authenticated as the given JWT holder.
     *
     * @param jwt        bearer token (without the "Bearer " prefix)
     * @param entityType the entity type query parameter (e.g. {@code "PROBE"})
     * @param entityId   the entity id query parameter
     * @return full RestAssured response
     */
    public Response queryAudit(String jwt, String entityType, String entityId) {
        return authedSpec(jwt)
                .queryParam("entity", entityType)
                .queryParam("id", entityId)
                .when()
                .get("/api/v1/audit");
    }

    /**
     * Queries the audit log without any Authorization header (unauthenticated).
     *
     * @param entityType the entity type query parameter
     * @param entityId   the entity id query parameter
     * @return full RestAssured response
     */
    public Response queryAuditUnauthenticated(String entityType, String entityId) {
        return baseSpec()
                .queryParam("entity", entityType)
                .queryParam("id", entityId)
                .when()
                .get("/api/v1/audit");
    }
}
