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
package com.efs.sdk.organizationmanager;

import com.efs.sdk.organizationmanager.security.oauth.OAuth2Properties;
import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Base class for the organizationmanager backend application.
 *
 * @author e:fs TechHub GmbH
 */
@SpringBootApplication
@EnableConfigurationProperties(OAuth2Properties.class)
public class OrganizationManagerApplication {

    /**
     * Main method.
     *
     * @param args Arguments for the call.
     */
    public static void main(String[] args) {
        SpringApplication.run(OrganizationManagerApplication.class, args);
    }

    /**
     * Creates an instance of the ModelMapper.
     *
     * @return The created {@link ModelMapper}
     */
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
