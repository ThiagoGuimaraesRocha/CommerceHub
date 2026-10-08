package com.commercehub.user.infrastructure;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * When {@code commercehub.migrate-only=true}, Flyway has already run at start and this process
 * exits so a Kubernetes Job can complete (Sprint 7). Application pods leave the flag false.
 */
@ApplicationScoped
public class FlywayMigrateOnly {

    private static final Logger LOG = Logger.getLogger(FlywayMigrateOnly.class);

    @ConfigProperty(name = "commercehub.migrate-only", defaultValue = "false")
    boolean migrateOnly;

    void onStart(@Observes @Priority(Integer.MAX_VALUE) StartupEvent event) {
        if (!migrateOnly) {
            return;
        }
        LOG.info("Flyway migrate-only job finished; exiting");
        Quarkus.asyncExit(0);
    }
}
