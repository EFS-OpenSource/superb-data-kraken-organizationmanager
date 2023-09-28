/*
Copyright (C) 2023 e:fs TechHub GmbH (sdk@efs-techhub.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.efs.sdk.organizationmanager.security;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * This is a Configuration class configures Cross-Origin Resource Sharing (CORS) for local development purposes.
 * It implements the WebMvcConfigurer interface which provides callback methods to customize the Java-based configuration for Spring MVC.
 * <p>
 * This configuration is conditional and will be activated only when the property 'organizationmanager.cors.disabled' is set to 'true'.
 * If the property is missing from the configuration, this class will not be activated (matchIfMissing = false) - this should be the default case.
 */
@Configuration
@ConditionalOnProperty(value = "organizationmanager.cors.disabled", havingValue = "true", matchIfMissing = false)
public class CorsConfigLocal implements WebMvcConfigurer {

    /**
     * This is configured to be very permissive for local development. All origins are allowed, meaning the API can be consumed from any location.
     * All HTTP methods and headers are also allowed.
     * <p>
     * This method applies the CORS configuration to all endpoints (/**).
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Apply CORS configuration to all endpoints
                .allowedOrigins("*")  // Allow requests from all origins for local development
                .allowedMethods("*")  // Allow all HTTP methods
                .allowedHeaders("*"); // Allow all headers
    }
}
