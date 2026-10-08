package com.commercehub.user.application;

import com.commercehub.user.api.dto.CreateUserRequest;
import com.commercehub.user.exception.ConflictException;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Creates the demonstration ADMIN user on first start when credentials are provided via env.
 * Does not log the password. No-op when email or password is blank (tests, fresh clones).
 */
@ApplicationScoped
public class AdminBootstrap {

    private static final Logger LOG = Logger.getLogger(AdminBootstrap.class);

    private final UserService userService;
    private final Optional<String> email;
    private final Optional<String> password;
    private final String fullName;

    public AdminBootstrap(
            UserService userService,
            @ConfigProperty(name = "commercehub.bootstrap.admin.email") Optional<String> email,
            @ConfigProperty(name = "commercehub.bootstrap.admin.password") Optional<String> password,
            @ConfigProperty(name = "commercehub.bootstrap.admin.full-name", defaultValue = "CommerceHub Admin")
                    String fullName) {
        this.userService = userService;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    void onStart(@Observes StartupEvent event) {
        String adminEmail = email.map(String::trim).filter(value -> !value.isEmpty()).orElse(null);
        String adminPassword = password.filter(value -> !value.isBlank()).orElse(null);
        if (adminEmail == null || adminPassword == null) {
            return;
        }
        try {
            userService.create(new CreateUserRequest(adminEmail, adminPassword, fullName, "ADMIN"));
            LOG.info("Bootstrap admin user is ready");
        } catch (ConflictException ignored) {
            LOG.debug("Bootstrap admin user already exists");
        }
    }
}
