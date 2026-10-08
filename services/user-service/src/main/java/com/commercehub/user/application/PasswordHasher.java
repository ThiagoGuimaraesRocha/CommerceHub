package com.commercehub.user.application;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PasswordHasher {

    public String hash(String rawPassword) {
        return BcryptUtil.bcryptHash(rawPassword);
    }

    public boolean matches(String rawPassword, String passwordHash) {
        return BcryptUtil.matches(rawPassword, passwordHash);
    }
}
