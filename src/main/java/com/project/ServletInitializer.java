package com.project;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Necessaire pour le packaging WAR : permet de deployer l'application dans un
 * conteneur de servlets externe (Tomcat, etc.) en plus du lancement embarque.
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(ProjectApplication.class);
    }
}
