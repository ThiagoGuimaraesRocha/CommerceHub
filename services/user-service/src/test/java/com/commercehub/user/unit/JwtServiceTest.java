package com.commercehub.user.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercehub.user.application.JwtService;
import com.commercehub.user.domain.entity.UserEntity;
import com.commercehub.user.domain.enumtype.UserRole;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

@QuarkusTest
class JwtServiceTest {

    @Inject
    JwtService jwtService;

    @Test
    void issuedTokenCarriesSubjectGroupsAndEmailAndOmitsThePassword() {
        UserEntity user = UserEntity.newUser();
        user.setEmail("ana@commercehub.local");
        user.setPasswordHash("not-a-real-hash");
        user.setFullName("Ana");
        user.setRole(UserRole.CUSTOMER);

        String token = jwtService.issue(user);
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);

        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        assertThat(payload).contains("\"sub\":\"" + user.getId() + "\"");
        assertThat(payload).contains("\"upn\":\"" + user.getId() + "\"");
        assertThat(payload).contains("CUSTOMER");
        assertThat(payload).contains("ana@commercehub.local");
        assertThat(payload).contains("https://commercehub.example/issuer");
        assertThat(payload).doesNotContain("not-a-real-hash");
        assertThat(payload).doesNotContain("password");
        assertThat(jwtService.lifespanSeconds()).isEqualTo(3600);
    }
}
