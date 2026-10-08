package com.commercehub.user.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.commercehub.user.api.dto.CreateUserRequest;
import com.commercehub.user.application.UserService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AuthResourceTest {

    @Inject
    UserService userService;

    @Test
    @TestSecurity(user = "bootstrap-admin", roles = "ADMIN")
    void loginReturnsBearerJwtForValidCredentials() {
        String email = "login-" + UUID.randomUUID() + "@commercehub.local";
        userService.create(new CreateUserRequest(email, "s3cret-pass", "Login User", "CUSTOMER"));

        given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", "s3cret-pass"))
                .when().post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .body("tokenType", equalTo("Bearer"))
                .body("accessToken", notNullValue())
                .body("expiresIn", equalTo(3600))
                .body("user.email", equalTo(email.toLowerCase()))
                .body("user.roleCode", equalTo("CUSTOMER"));
    }

    @Test
    void loginWithWrongPasswordReturns401Problem() {
        given().contentType(ContentType.JSON)
                .body(Map.of("email", "nobody@commercehub.local", "password", "wrong-password"))
                .when().post("/api/v1/auth/login")
                .then()
                .statusCode(401)
                .contentType("application/problem+json")
                .body("type", equalTo("urn:commercehub:problem:invalid-credentials"));
    }

    @Test
    void loginValidationReturns400() {
        given().contentType(ContentType.JSON)
                .body(Map.of("email", "not-an-email", "password", ""))
                .when().post("/api/v1/auth/login")
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("type", equalTo("urn:commercehub:problem:validation-error"));
    }
}
