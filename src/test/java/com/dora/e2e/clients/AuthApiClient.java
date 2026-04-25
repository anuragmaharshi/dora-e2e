package com.dora.e2e.clients;

import com.dora.e2e.support.Config;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Thin RestAssured wrapper for the /api/v1/auth endpoints.
 * All methods return the raw {@link Response} so step definitions can assert on it.
 */
public class AuthApiClient {

    private RequestSpecification baseSpec() {
        return RestAssured
                .given()
                .baseUri(Config.API_BASE_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    /**
     * POST /api/v1/auth/login
     *
     * @param email    the user's email / username
     * @param password the raw password
     * @return full RestAssured response
     */
    public Response login(String email, String password) {
        String body = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", email, password);
        return baseSpec()
                .body(body)
                .when()
                .post("/api/v1/auth/login");
    }

    /**
     * GET /api/v1/auth/me — authenticated call.
     *
     * @param jwt bearer token (without the "Bearer " prefix)
     * @return full RestAssured response
     */
    public Response getMe(String jwt) {
        return baseSpec()
                .header("Authorization", "Bearer " + jwt)
                .when()
                .get("/api/v1/auth/me");
    }

    /**
     * GET /api/v1/auth/me — unauthenticated call (no Authorization header).
     *
     * @return full RestAssured response
     */
    public Response getMeWithoutToken() {
        return baseSpec()
                .when()
                .get("/api/v1/auth/me");
    }

    /**
     * GET /api/v1/incidents/_probe — RBAC probe endpoint.
     *
     * @param jwt bearer token
     * @return full RestAssured response
     */
    public Response getIncidentProbe(String jwt) {
        return baseSpec()
                .header("Authorization", "Bearer " + jwt)
                .when()
                .get("/api/v1/incidents/_probe");
    }
}
