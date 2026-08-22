package com.ispautomation.config;


import com.ispautomation.modules.rbac.entity.User;
import com.ispautomation.modules.rbac.repository.UserRepository;
import com.ispautomation.security.PasswordEncoder;


import io.quarkus.runtime.StartupEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;


import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.jboss.logging.Logger;



@ApplicationScoped
public class DataInitializer {



    private static final Logger LOG =
            Logger.getLogger(DataInitializer.class);



    private static final String PLACEHOLDER =
            "PLACEHOLDER_TO_BE_REPLACED_BY_BCRYPT";



    @Inject
    UserRepository userRepository;



    @Inject
    PasswordEncoder passwordEncoder;



    @ConfigProperty(
        name="app.default-admin.username",
        defaultValue="admin"
    )
    String adminUsername;



    @ConfigProperty(
        name="app.default-admin.password",
        defaultValue="Admin@123"
    )
    String adminPassword;




    @Transactional
    void onStart(@Observes StartupEvent event){



        User admin =
            userRepository.find(
                "username",
                adminUsername
            ).firstResult();



        if(admin == null){
            LOG.warn("Default admin user not found.");
            return;
        }



        if(PLACEHOLDER.equals(admin.getPasswordHash())){


            admin.setPasswordHash(
                passwordEncoder.encode(adminPassword)
            );


            admin.setPasswordChangedAt(
                java.time.LocalDateTime.now()
            );


            // Hibernate automatically updates managed entity


            LOG.infof(
                "Default admin password initialized for '%s'",
                adminUsername
            );


            LOG.warn(
                "Change default password immediately after first login."
            );
        }

    }

}