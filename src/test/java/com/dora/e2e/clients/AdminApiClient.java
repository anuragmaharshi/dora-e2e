package com.dora.e2e.clients;

import com.dora.e2e.support.Config;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.time.LocalDate;

/**
 * Thin RestAssured wrapper for all /api/v1/admin/** endpoints.
 *
 * <p>All methods return the raw {@link Response} so step definitions can assert on it.
 * No assertions live here — this client is data-access only.
 *
 * <p>Covers:
 * <ul>
 *   <li>GET|PUT /api/v1/admin/tenant          (AC-1, AC-8)</li>
 *   <li>GET     /api/v1/admin/critical-services (AC-2)</li>
 *   <li>POST    /api/v1/admin/critical-services (AC-2)</li>
 *   <li>DELETE  /api/v1/admin/critical-services/{id} (AC-2 archive)</li>
 *   <li>GET     /api/v1/admin/client-base       (AC-3)</li>
 *   <li>POST    /api/v1/admin/client-base       (AC-3)</li>
 *   <li>GET|PUT /api/v1/admin/nca-email         (AC-4)</li>
 * </ul>
 */
public class AdminApiClient {

    private RequestSpecification baseSpec(String jwt) {
        return RestAssured
                .given()
                .baseUri(Config.API_BASE_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Bearer " + jwt);
    }

    private RequestSpecification baseSpecNoAuth() {
        return RestAssured
                .given()
                .baseUri(Config.API_BASE_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    // -------------------------------------------------------------------------
    // Tenant config
    // -------------------------------------------------------------------------

    /**
     * GET /api/v1/admin/tenant — retrieve current tenant configuration.
     *
     * @param jwt bearer token for PLATFORM_ADMIN
     * @return full RestAssured response
     */
    public Response getTenantConfig(String jwt) {
        return baseSpec(jwt)
                .when()
                .get("/api/v1/admin/tenant");
    }

    /**
     * PUT /api/v1/admin/tenant — update tenant configuration fields.
     *
     * @param jwt          bearer token for PLATFORM_ADMIN
     * @param legalName    new legal name
     * @param lei          legal entity identifier
     * @param ncaName      NCA authority name
     * @param ncaEmail     NCA contact email
     * @param jurisdictionIso ISO country code for jurisdiction
     * @return full RestAssured response
     */
    public Response updateTenantConfig(String jwt, String legalName, String lei,
                                       String ncaName, String ncaEmail, String jurisdictionIso) {
        String body = String.format(
                "{\"legalName\":\"%s\",\"lei\":\"%s\",\"ncaName\":\"%s\"," +
                "\"ncaEmail\":\"%s\",\"jurisdictionIso\":\"%s\"}",
                legalName, lei, ncaName, ncaEmail, jurisdictionIso);
        return baseSpec(jwt)
                .body(body)
                .when()
                .put("/api/v1/admin/tenant");
    }

    /**
     * PUT /api/v1/admin/tenant — partial update: only legalName and ncaEmail.
     * Other fields are left unchanged by passing reasonable placeholder values.
     *
     * @param jwt       bearer token for PLATFORM_ADMIN
     * @param legalName new legal name
     * @param ncaEmail  new NCA email
     * @return full RestAssured response
     */
    public Response updateTenantConfigPartial(String jwt, String legalName, String ncaEmail) {
        String body = String.format(
                "{\"legalName\":\"%s\",\"lei\":\"TESTLEI0000001\"," +
                "\"ncaName\":\"Test NCA\",\"ncaEmail\":\"%s\",\"jurisdictionIso\":\"IE\"}",
                legalName, ncaEmail);
        return baseSpec(jwt)
                .body(body)
                .when()
                .put("/api/v1/admin/tenant");
    }

    // -------------------------------------------------------------------------
    // Critical services
    // -------------------------------------------------------------------------

    /**
     * GET /api/v1/admin/critical-services — list all critical services for this tenant.
     *
     * @param jwt bearer token for PLATFORM_ADMIN
     * @return full RestAssured response
     */
    public Response getCriticalServices(String jwt) {
        return baseSpec(jwt)
                .when()
                .get("/api/v1/admin/critical-services");
    }

    /**
     * POST /api/v1/admin/critical-services — create a new critical service.
     *
     * @param jwt         bearer token for PLATFORM_ADMIN
     * @param name        service name
     * @param description optional description (may be null)
     * @return full RestAssured response
     */
    public Response createCriticalService(String jwt, String name, String description) {
        String descPart = description != null
                ? String.format(",\"description\":\"%s\"", description)
                : "";
        String body = String.format("{\"name\":\"%s\"%s}", name, descPart);
        return baseSpec(jwt)
                .body(body)
                .when()
                .post("/api/v1/admin/critical-services");
    }

    /**
     * POST /api/v1/admin/critical-services — create with name only.
     *
     * @param jwt  bearer token for PLATFORM_ADMIN
     * @param name service name
     * @return full RestAssured response
     */
    public Response createCriticalService(String jwt, String name) {
        return createCriticalService(jwt, name, null);
    }

    /**
     * DELETE /api/v1/admin/critical-services/{id} — archive (soft-delete) a critical service.
     *
     * @param jwt bearer token for PLATFORM_ADMIN
     * @param id  UUID of the critical service to archive
     * @return full RestAssured response
     */
    public Response archiveCriticalService(String jwt, String id) {
        return baseSpec(jwt)
                .when()
                .delete("/api/v1/admin/critical-services/" + id);
    }

    // -------------------------------------------------------------------------
    // Client base
    // -------------------------------------------------------------------------

    /**
     * GET /api/v1/admin/client-base — get client base history.
     *
     * @param jwt bearer token for PLATFORM_ADMIN
     * @return full RestAssured response
     */
    public Response getClientBase(String jwt) {
        return baseSpec(jwt)
                .when()
                .get("/api/v1/admin/client-base");
    }

    /**
     * POST /api/v1/admin/client-base — record a new client base count.
     *
     * @param jwt          bearer token for PLATFORM_ADMIN
     * @param clientCount  number of clients
     * @param effectiveDate the effective date (ISO date string, e.g. "2026-05-07")
     * @return full RestAssured response
     */
    public Response createClientBaseEntry(String jwt, long clientCount, String effectiveDate) {
        String body = String.format("{\"clientCount\":%d,\"effectiveDate\":\"%s\"}",
                clientCount, effectiveDate);
        return baseSpec(jwt)
                .body(body)
                .when()
                .post("/api/v1/admin/client-base");
    }

    /**
     * POST /api/v1/admin/client-base — record a new client base count effective today.
     *
     * @param jwt         bearer token for PLATFORM_ADMIN
     * @param clientCount number of clients
     * @return full RestAssured response
     */
    public Response createClientBaseEntry(String jwt, long clientCount) {
        return createClientBaseEntry(jwt, clientCount, LocalDate.now().toString());
    }

    // -------------------------------------------------------------------------
    // NCA email config
    // -------------------------------------------------------------------------

    /**
     * GET /api/v1/admin/nca-email — retrieve current NCA email configuration.
     *
     * @param jwt bearer token for PLATFORM_ADMIN
     * @return full RestAssured response
     */
    public Response getNcaEmailConfig(String jwt) {
        return baseSpec(jwt)
                .when()
                .get("/api/v1/admin/nca-email");
    }

    /**
     * PUT /api/v1/admin/nca-email — update NCA email configuration.
     *
     * @param jwt             bearer token for PLATFORM_ADMIN
     * @param sender          sender email address
     * @param recipient       recipient email address
     * @param subjectTemplate subject template string (may contain {{incidentId}})
     * @return full RestAssured response
     */
    public Response updateNcaEmailConfig(String jwt, String sender, String recipient,
                                         String subjectTemplate) {
        // Escape the template string for JSON
        String escapedTemplate = subjectTemplate.replace("\"", "\\\"");
        String body = String.format(
                "{\"sender\":\"%s\",\"recipient\":\"%s\",\"subjectTemplate\":\"%s\"}",
                sender, recipient, escapedTemplate);
        return baseSpec(jwt)
                .body(body)
                .when()
                .put("/api/v1/admin/nca-email");
    }

    // -------------------------------------------------------------------------
    // Incident endpoint — used for AC-6 (PLATFORM_ADMIN blocked)
    // -------------------------------------------------------------------------

    /**
     * GET /api/v1/incidents — attempt to list incidents with the supplied JWT.
     * Used to verify that PLATFORM_ADMIN is blocked (AC-6).
     *
     * @param jwt bearer token
     * @return full RestAssured response (expected 403 for PLATFORM_ADMIN)
     */
    public Response getIncidents(String jwt) {
        return baseSpec(jwt)
                .when()
                .get("/api/v1/incidents");
    }

    // -------------------------------------------------------------------------
    // Unauthenticated calls — used for AC-7 (BANK_USER blocked)
    // -------------------------------------------------------------------------

    /**
     * GET /api/v1/admin/tenant — with an arbitrary JWT (used for BANK_USER scenarios).
     *
     * @param jwt bearer token for any user (BANK_USER, etc.)
     * @return full RestAssured response
     */
    public Response getTenantConfigAs(String jwt) {
        return baseSpec(jwt)
                .when()
                .get("/api/v1/admin/tenant");
    }

    /**
     * GET /api/v1/admin/critical-services — with an arbitrary JWT.
     *
     * @param jwt bearer token for any user
     * @return full RestAssured response
     */
    public Response getCriticalServicesAs(String jwt) {
        return baseSpec(jwt)
                .when()
                .get("/api/v1/admin/critical-services");
    }

    /**
     * GET /api/v1/admin/client-base — with an arbitrary JWT.
     *
     * @param jwt bearer token for any user
     * @return full RestAssured response
     */
    public Response getClientBaseAs(String jwt) {
        return baseSpec(jwt)
                .when()
                .get("/api/v1/admin/client-base");
    }

    /**
     * GET /api/v1/admin/nca-email — with an arbitrary JWT.
     *
     * @param jwt bearer token for any user
     * @return full RestAssured response
     */
    public Response getNcaEmailConfigAs(String jwt) {
        return baseSpec(jwt)
                .when()
                .get("/api/v1/admin/nca-email");
    }
}
