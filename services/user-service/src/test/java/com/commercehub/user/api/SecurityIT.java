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
class SecurityIT {

    @Inject
    UserService userService;

    @Test
    void missingTokenOnUsersIs401() {
        given().when().get("/api/v1/users/me")
                .then()
                .statusCode(401)
                .contentType("application/problem+json")
                .body("type", equalTo("urn:commercehub:problem:unauthorized"));
    }

    @Test
    void invalidBearerOnUsersIs401() {
        given().header("Authorization", "Bearer not-a-jwt")
                .when().get("/api/v1/users/me")
                .then()
                .statusCode(401);
    }

    @Test
    void createUserWithoutTokenIs401() {
        given().contentType(ContentType.JSON)
                .body(Map.of("email", "x@commercehub.local", "password", "s3cret-pass",
                        "fullName", "X", "roleCode", "CUSTOMER"))
                .when().post("/api/v1/users")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "customer-user", roles = "CUSTOMER")
    void customerCannotCreateUsers() {
        given().contentType(ContentType.JSON)
                .body(Map.of("email", "x@commercehub.local", "password", "s3cret-pass",
                        "fullName", "X", "roleCode", "CUSTOMER"))
                .when().post("/api/v1/users")
                .then()
                .statusCode(403)
                .contentType("application/problem+json")
                .body("type", equalTo("urn:commercehub:problem:forbidden"));
    }

    @Test
    void loginThenMeWithRealJwt() {
        String email = "jwt-" + UUID.randomUUID() + "@commercehub.local";
        userService.create(new CreateUserRequest(email, "s3cret-pass", "Jwt User", "CUSTOMER"));

        String token = given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", "s3cret-pass"))
                .when().post("/api/v1/auth/login")
                .then().statusCode(200)
                .extract().path("accessToken");

        given().header("Authorization", "Bearer " + token)
                .when().get("/api/v1/users/me")
                .then()
                .statusCode(200)
                .body("email", equalTo(email))
                .body("id", notNullValue());
    }

    @Test
    void healthAndOpenApiRemainPublic() {
        given().when().get("/q/health/live").then().statusCode(200);
        given().when().get("/q/openapi?format=json")
                .then()
                .statusCode(200)
                .body("info.title", equalTo("CommerceHub User Service API"));
    }
}
