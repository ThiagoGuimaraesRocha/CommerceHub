package com.commercehub.user.application;

import com.commercehub.user.domain.entity.UserEntity;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class JwtService {

    private final String issuer;
    private final long lifespanSeconds;

    public JwtService(
            @ConfigProperty(name = "mp.jwt.verify.issuer") String issuer,
            @ConfigProperty(name = "smallrye.jwt.new-token.lifespan") long lifespanSeconds) {
        this.issuer = issuer;
        this.lifespanSeconds = lifespanSeconds;
    }

    public String issue(UserEntity user) {
        return Jwt.issuer(issuer)
                .subject(user.getId())
                .upn(user.getId())
                .groups(Set.of(user.getRole().name()))
                .claim("email", user.getEmail())
                .expiresIn(Duration.ofSeconds(lifespanSeconds))
                .sign();
    }

    public long lifespanSeconds() {
        return lifespanSeconds;
    }
}
