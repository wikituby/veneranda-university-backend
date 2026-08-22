package com.ispautomation.security;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt-based password encoder.
 * Uses the jbcrypt library (bundled via the smallrye-jwt transitive deps).
 */
@ApplicationScoped
public class PasswordEncoder {

    @ConfigProperty(name = "app.security.password.bcrypt-strength", defaultValue = "12")
    int bcryptStrength;

    /**
     * Hash a raw password using BCrypt.
     */
    public String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(bcryptStrength));
    }

    /**
     * Verify a raw password against a BCrypt hash.
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, encodedPassword);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}