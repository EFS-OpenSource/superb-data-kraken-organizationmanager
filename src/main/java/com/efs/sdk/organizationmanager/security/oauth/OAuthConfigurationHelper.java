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
package com.efs.sdk.organizationmanager.security.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class OAuthConfigurationHelper {

    private final RestTemplate restTemplate;
    @Value("${sdk.oauth2.config-url}")
    private String openidConfiguration;

    public OAuthConfigurationHelper(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public OAuthConfiguration getOpenidConfigProperty() {
        return getOpenidConfigProperty(openidConfiguration);
    }

    public OAuthConfiguration getOpenidConfigProperty(String openidConfiguration) {
        ResponseEntity<OAuthConfiguration> response = restTemplate.exchange(openidConfiguration, HttpMethod.GET,
                new HttpEntity<>(new LinkedMultiValueMap<>()), OAuthConfiguration.class);
        OAuthConfiguration body = response.getBody();
        if (body == null) {
            throw new IllegalArgumentException("conflicting auth-configuration found!");
        }
        return body;
    }
}
