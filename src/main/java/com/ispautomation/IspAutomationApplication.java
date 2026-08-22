package com.ispautomation;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Main application entry point for the ISP Automation Platform backend.
 * Bootstraps the Quarkus application and performs startup logging.
 */
@ApplicationScoped
public class IspAutomationApplication {

    private static final Logger LOG = Logger.getLogger(IspAutomationApplication.class);

    @ConfigProperty(name = "quarkus.application.version", defaultValue = "1.0.0-SNAPSHOT")
    String version;

    /**
     * Runs on application startup.
     */
    void onStart(@Observes StartupEvent event) {
        LOG.infof("=== ISP Automation Platform backend v%s starting ===", version);
        LOG.info("Clean Architecture | Quarkus | PostgreSQL | Redis | JWT");
    }
}