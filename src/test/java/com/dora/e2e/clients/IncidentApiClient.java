package com.dora.e2e.clients;

import com.dora.e2e.support.Config;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Thin RestAssured wrapper for all /api/v1/incidents/** endpoints.
 *
 * <p>All methods return the raw {@link Response} so step definitions can assert on it.
 * No assertions live here — this client is data-access only.
 *
 * <p>Covers:
 * <ul>
 *   <li>POST   /api/v1/incidents                                    (AC-1, AC-4, AC-7)</li>
 *   <li>GET    /api/v1/incidents                                    (AC-8)</li>
 *   <li>GET    /api/v1/incidents/{id}                               (AC-6, AC-7)</li>
 *   <li>POST   /api/v1/incidents/{id}/attachments                   (AC-3)</li>
 *   <li>POST   /api/v1/incidents/{id}/attachments/{attachmentId}/complete (AC-3)</li>
 *   <li>POST   /api/v1/incidents/{id}/services                      (AC-4)</li>
 *   <li>POST   /api/v1/incidents/{id}/assets                        (AC-5)</li>
 * </ul>
 */
public class IncidentApiClient {

    // -------------------------------------------------------------------------
    // Request spec helpers
    // -------------------------------------------------------------------------

    private RequestSpecification authedSpec(String jwt) {
        return RestAssured
                .given()
                .baseUri(Config.API_BASE_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Bearer " + jwt);
    }

    private RequestSpecification authedSpecNoContentType(String jwt) {
        return RestAssured
                .given()
                .baseUri(Config.API_BASE_URL)
                .accept(ContentType.JSON)
                .header("Authorization", "Bearer " + jwt);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/incidents
    // -------------------------------------------------------------------------

    /**
     * Create a new incident with only title and severity.
     *
     * @param jwt      bearer token
     * @param title    incident title
     * @param severity severity string (e.g. "HIGH")
     * @return full RestAssured response (201 on success)
     */
    public Response createIncident(String jwt, String title, String severity) {
        String body = String.format(
                "{\"title\":\"%s\",\"severity\":\"%s\"}",
                escapeJson(title), severity);
        return authedSpec(jwt)
                .body(body)
                .when()
                .post("/api/v1/incidents");
    }

    /**
     * Create a new incident linking to a specific critical service.
     *
     * @param jwt       bearer token
     * @param title     incident title
     * @param severity  severity string
     * @param serviceId UUID of the critical service to link
     * @return full RestAssured response
     */
    public Response createIncidentWithService(String jwt, String title,
                                              String severity, String serviceId) {
        String body = String.format(
                "{\"title\":\"%s\",\"severity\":\"%s\",\"affectedServiceIds\":[\"%s\"]}",
                escapeJson(title), severity, serviceId);
        return authedSpec(jwt)
                .body(body)
                .when()
                .post("/api/v1/incidents");
    }

    /**
     * Attempt to create an incident with a mutated (caller-supplied) detectionDatetime.
     * This is the AC-2 negative test — the server must reject this with 422.
     *
     * @param jwt                 bearer token
     * @param incidentId          UUID of the existing incident
     * @param mutatedDatetime     ISO-8601 datetime string to attempt to set
     * @return full RestAssured response (expected 422)
     */
    public Response updateDetectionDatetime(String jwt, String incidentId,
                                            String mutatedDatetime) {
        String body = String.format("{\"detectionDatetime\":\"%s\"}", mutatedDatetime);
        return authedSpec(jwt)
                .body(body)
                .when()
                .put("/api/v1/incidents/" + incidentId);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/incidents
    // -------------------------------------------------------------------------

    /**
     * List all incidents (pageable).
     *
     * @param jwt bearer token
     * @return full RestAssured response
     */
    public Response listIncidents(String jwt) {
        return authedSpec(jwt)
                .when()
                .get("/api/v1/incidents");
    }

    /**
     * List incidents using an arbitrary JWT (used to test 403 for PLATFORM_ADMIN).
     *
     * @param jwt bearer token for the caller
     * @return full RestAssured response
     */
    public Response listIncidentsAs(String jwt) {
        return authedSpec(jwt)
                .when()
                .get("/api/v1/incidents");
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/incidents/{id}
    // -------------------------------------------------------------------------

    /**
     * Retrieve a single incident by its UUID.
     *
     * @param jwt        bearer token
     * @param incidentId UUID of the incident
     * @return full RestAssured response
     */
    public Response getIncident(String jwt, String incidentId) {
        return authedSpec(jwt)
                .when()
                .get("/api/v1/incidents/" + incidentId);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/incidents/{id}/attachments
    // -------------------------------------------------------------------------

    /**
     * Request a presigned upload URL for an attachment.
     *
     * @param jwt         bearer token
     * @param incidentId  UUID of the incident
     * @param filename    original filename (e.g. "evidence.pdf")
     * @param contentType MIME type (e.g. "application/pdf")
     * @return full RestAssured response containing uploadUrl and attachmentId (201 on success)
     */
    public Response requestAttachmentUpload(String jwt, String incidentId,
                                            String filename, String contentType) {
        String body = String.format(
                "{\"filename\":\"%s\",\"contentType\":\"%s\"}",
                escapeJson(filename), escapeJson(contentType));
        return authedSpec(jwt)
                .body(body)
                .when()
                .post("/api/v1/incidents/" + incidentId + "/attachments");
    }

    // -------------------------------------------------------------------------
    // PUT (upload) to presigned MinIO URL — uses JDK HttpClient, not RestAssured
    // -------------------------------------------------------------------------

    /**
     * Upload raw bytes to a presigned MinIO URL using the JDK {@link HttpClient}.
     *
     * <p>This is intentionally NOT using RestAssured because the presigned URL points
     * directly to MinIO (not to the Spring Boot API) and may have a different base URL.
     *
     * @param presignedUrl the full presigned URL returned by the attachments endpoint
     * @param bytes        the file content to upload
     * @param contentType  MIME type (must match what was used when requesting the URL)
     * @return HTTP status code from MinIO (expected 200)
     */
    public int uploadToPresignedUrl(String presignedUrl, byte[] bytes, String contentType) {
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(presignedUrl))
                    .header("Content-Type", contentType)
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                    .build();
            HttpResponse<Void> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to PUT file bytes to presigned URL: " + presignedUrl +
                    ". Root cause: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/incidents/{id}/attachments/{attachmentId}/complete
    // -------------------------------------------------------------------------

    /**
     * Mark an attachment upload as complete.
     * The API verifies the object exists in MinIO, then sets status to READY.
     *
     * @param jwt          bearer token
     * @param incidentId   UUID of the incident
     * @param attachmentId UUID of the attachment (returned in the presigned URL response)
     * @return full RestAssured response (expected 200 with status READY)
     */
    public Response completeAttachmentUpload(String jwt, String incidentId,
                                             String attachmentId) {
        return authedSpecNoContentType(jwt)
                .when()
                .post("/api/v1/incidents/" + incidentId +
                      "/attachments/" + attachmentId + "/complete");
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/incidents/{id}/services
    // -------------------------------------------------------------------------

    /**
     * Link a critical service to an incident.
     *
     * @param jwt        bearer token
     * @param incidentId UUID of the incident
     * @param serviceId  UUID of the critical service
     * @return full RestAssured response
     */
    public Response linkService(String jwt, String incidentId, String serviceId) {
        String body = String.format("{\"serviceIds\":[\"%s\"]}", serviceId);
        return authedSpec(jwt)
                .body(body)
                .when()
                .post("/api/v1/incidents/" + incidentId + "/services");
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/incidents/{id}/assets
    // -------------------------------------------------------------------------

    /**
     * Link an ICT asset to an incident.
     *
     * @param jwt        bearer token
     * @param incidentId UUID of the incident
     * @param name       asset name (e.g. "Core Router")
     * @param type       asset type (e.g. "HARDWARE")
     * @return full RestAssured response (expected 201)
     */
    public Response linkAsset(String jwt, String incidentId, String name, String type) {
        String body = String.format("{\"name\":\"%s\",\"type\":\"%s\"}",
                escapeJson(name), escapeJson(type));
        return authedSpec(jwt)
                .body(body)
                .when()
                .post("/api/v1/incidents/" + incidentId + "/assets");
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Minimal JSON string escaping for values used in inline JSON bodies. */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
