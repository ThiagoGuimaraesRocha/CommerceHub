package com.commercehub.user.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercehub.user.application.PasswordHasher;
import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashIsNotThePlainPasswordAndMatches() {
        String hash = hasher.hash("correct-horse");
        assertThat(hash).isNotEqualTo("correct-horse");
        assertThat(hash).startsWith("$2");
        assertThat(hasher.matches("correct-horse", hash)).isTrue();
        assertThat(hasher.matches("wrong-password", hash)).isFalse();
    }
}
