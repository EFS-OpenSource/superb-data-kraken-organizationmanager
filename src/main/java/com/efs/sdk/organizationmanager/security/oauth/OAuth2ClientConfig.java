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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;

@Configuration
public class OAuth2ClientConfig {

    private static final String REDIRECT_URI = "{baseUrl}/{action}/oauth2/code/{registrationId}";
    private final OAuthConfigurationHelper oauthConfigHelper;
    private final OAuth2Properties oauthProperties;

    public OAuth2ClientConfig(OAuthConfigurationHelper oauthConfigHelper,
                              OAuth2Properties oauthProperties) {
        this.oauthConfigHelper = oauthConfigHelper;
        this.oauthProperties = oauthProperties;
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> registrations = new ArrayList<>();
        registrations.add(getClientRegistration(UUID.randomUUID().toString(), oauthProperties));

        return new InMemoryClientRegistrationRepository(registrations);
    }

    private ClientRegistration getClientRegistration(String registrationId, OAuth2Properties instance) {
        OAuthConfiguration oauthConfig = oauthProperties.getIssuerUri() == null ? oauthConfigHelper.getOpenidConfigProperty() :
                oauthConfigHelper.getOpenidConfigProperty(format("%s/.well-known/openid-configuration", instance.getIssuerUri()));
        ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(registrationId)
                .clientId(instance.getClientId())
                .redirectUri(REDIRECT_URI)
                .authorizationGrantType(instance.getAuthorizationGrantType())
                .scope(instance.getScope())
                .authorizationUri(oauthConfig.authorizationEndpoint())
                .tokenUri(oauthConfig.tokenEndpoint())
                .jwkSetUri(oauthConfig.jwksUri())
                .userInfoUri(oauthConfig.userinfoEndpoint())
                .userNameAttributeName("preferred_username")
                .clientName("Client " + instance.getClientId());
        builder = withSecret(builder, oauthProperties.getClientSecret());
        return builder.build();
    }

    private ClientRegistration.Builder withSecret(ClientRegistration.Builder builder, String clientSecret) {
        return clientSecret == null || clientSecret.isEmpty() ? builder : builder.clientSecret(clientSecret);
    }
}