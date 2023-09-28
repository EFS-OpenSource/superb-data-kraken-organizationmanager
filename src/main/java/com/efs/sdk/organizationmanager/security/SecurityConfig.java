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

import com.efs.sdk.organizationmanager.security.oauth.OAuthConfiguration;
import com.efs.sdk.organizationmanager.security.oauth.OAuthConfigurationHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

/**
 * Security configuration.
 *
 * @author e:fs TechHub GmbH
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] WHITELIST_URLS = {"/actuator/health", "/actuator/health/**"};
    private static final String[] PROMETHEUS_URLS = {"/actuator/prometheus", "/actuator/prometheus/**"};
    private final OAuthConfiguration oauthConfig;

    public SecurityConfig(OAuthConfigurationHelper configHelper) {
        this.oauthConfig = configHelper.getOpenidConfigProperty();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return JwtDecoders.fromIssuerLocation(oauthConfig.issuer());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults()).csrf(AbstractHttpConfigurer::disable); // set cors and disable csrf

        // enable anonymous
        // access to whitelist-urls
        http.authorizeHttpRequests(authorize -> authorize.requestMatchers(WHITELIST_URLS).permitAll()).anonymous(Customizer.withDefaults());

        // ip based access to prometheus, whitelist for private ip ranges
        http.authorizeHttpRequests(authorize -> authorize.requestMatchers(PROMETHEUS_URLS).access(new WebExpressionAuthorizationManager("hasIpAddress('192" +
                ".168.0.0/16') or hasIpAddress('172.16.0.0/12')" + "or hasIpAddress('127.0.0.1/8') or hasIpAddress('10.0.0.0/8')")));

        // convert OAuth2AuthenticationToken (as provided by oauthLogin()) to JwtAuthenticationToken (as required by
        // Controllers)
        http.authorizeHttpRequests(ar -> ar.anyRequest().authenticated())// any request should be authenticated
                .oauth2Login(Customizer.withDefaults()) // login before accessing (browser-access)
                .oauth2ResourceServer(resolver -> resolver.jwt(it -> it.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(new CustomJwtGrantedAuthoritiesConverter());
        return jwtConverter;
    }
}
