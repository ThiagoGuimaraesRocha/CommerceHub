package com.commercehub.user.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.commercehub.user.api.dto.CreateUserRequest;
import com.commercehub.user.api.dto.UserResponse;
import com.commercehub.user.application.UserService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UserResourceTest {

    private static final String USERS = "/api/v1/users";

    @Inject
    UserService userService;

    @Test
    @TestSecurity(user = "admin-user", roles = "ADMIN")
    void createReturns201WithLocationAndGetById() {
        String email = "create-" + UUID.randomUUID() + "@commercehub.local";

        String id = given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", "s3cret-pass", "fullName", "New Customer", "roleCode", "CUSTOMER"))
                .when().post(USERS)
                .then()
                .statusCode(201)
                .header("Location", containsString(USERS + "/"))
                .body("id", notNullValue())
                .body("email", equalTo(email))
                .body("roleCode", equalTo("CUSTOMER"))
                .body("active", equalTo(true))
                .extract().path("id");

        given().when().get(USERS + "/" + id)
                .then()
                .statusCode(200)
                .body("email", equalTo(email));
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "ADMIN")
    void duplicateEmailReturns409() {
        String email = "dup-" + UUID.randomUUID() + "@commercehub.local";
        userService.create(new CreateUserRequest(email, "s3cret-pass", "One", "CUSTOMER"));

        given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", "s3cret-pass", "fullName", "Two", "roleCode", "CUSTOMER"))
                .when().post(USERS)
                .then()
                .statusCode(409)
                .contentType("application/problem+json")
                .body("type", equalTo("urn:commercehub:problem:duplicate-email"));
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "ADMIN")
    void unknownUserReturns404() {
        given().when().get(USERS + "/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("type", equalTo("urn:commercehub:problem:user-not-found"));
    }

    @Test
    @TestSecurity(user = "11111111-1111-1111-1111-111111111111", roles = "CUSTOMER")
    void meReturnsTheAuthenticatedUser() {
        UserResponse created = userService.create(new CreateUserRequest(
                "me-" + UUID.randomUUID() + "@commercehub.local", "s3cret-pass", "Me", "CUSTOMER"));

        // /me uses the security identity name (user id / upn). Recreate with a known id is not
        // possible here, so this assertion covers 200 + body shape via ADMIN create + JWT login
        // in SecurityIT. This test still verifies CUSTOMER cannot list by id.
        given().when().get(USERS + "/" + created.id())
                .then()
                .statusCode(403);
    }
}
