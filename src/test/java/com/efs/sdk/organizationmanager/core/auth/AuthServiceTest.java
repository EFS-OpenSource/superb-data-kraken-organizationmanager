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
package com.efs.sdk.organizationmanager.core.auth;

import com.efs.sdk.organizationmanager.commons.OrganizationmanagerException;
import com.efs.sdk.organizationmanager.core.auth.model.TokenModel;
import com.efs.sdk.organizationmanager.security.oauth.OAuthConfiguration;
import com.efs.sdk.organizationmanager.security.oauth.OAuthConfigurationHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

class AuthServiceTest {

    @MockBean
    private RestTemplate restTemplate;
    private AuthService service;
    @MockBean
    private OAuthConfigurationHelper oAuthConfigurationHelper;
    @MockBean
    private OAuthConfiguration oauthConfig;
    private ObjectMapper objectMapper;

    private static final String TOKEN_ENDPOINT = "http://localhost:8080/auth/realms/efs-sdk/protocol/openid-connect/token";

    @BeforeEach
    void setup() {
        this.restTemplate = Mockito.mock(RestTemplate.class);
        this.oAuthConfigurationHelper = Mockito.mock(OAuthConfigurationHelper.class);
        String clientSecret = "my-client-secret";
        String clientId = "my-client-id";
        this.oauthConfig = Mockito.mock(OAuthConfiguration.class);
        given(oAuthConfigurationHelper.getOpenidConfigProperty()).willReturn(oauthConfig);
        this.objectMapper = new ObjectMapper();
        this.service = new AuthService(restTemplate, oAuthConfigurationHelper, clientId, clientSecret);
    }

    @Test
    void givenFetchingTokenOk_thenOk() throws Exception {
        String jwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9" +
                ".eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE2MjAxMTg1NTUsImV4cCI6MTY1MTY1NDU1NSwiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIkdpdmVuTmFtZSI6IkpvaG5ueSIsIlN1cm5hbWUiOiJSb2NrZXQiLCJFbWFpbCI6Impyb2NrZXRAZXhhbXBsZS5jb20iLCJSb2xlIjpbIk1hbmFnZXIiLCJQcm9qZWN0IEFkbWluaXN0cmF0b3IiXX0.yL6MTJ24A2KtU1RgUCnAuh8Q0PRr9PxtTgQLPWF_0r4";
        String response = format("""
                {
                    "access_token": "%s",
                    "expires_in": 300,
                    "refresh_expires_in": 1800,
                    "refresh_token": "something",
                    "token_type": "bearer",
                    "not-before-policy": 0,
                    "session_state": "c34d128d-3949-4686-9c6f-9afd02e09c9c",
                    "scope": "profile email"
                }""", jwt);
        given(oauthConfig.tokenEndpoint()).willReturn(TOKEN_ENDPOINT);
        given(restTemplate.postForEntity(eq(TOKEN_ENDPOINT), any(), eq(TokenModel.class))).willReturn(ResponseEntity.ok(objectMapper.readValue(response,
                TokenModel.class)));
        assertDoesNotThrow(() -> service.getSAaccessToken());
    }

    @Test
    void givenUnableFetchingToken_thenError() throws Exception {
        given(oauthConfig.tokenEndpoint()).willReturn(TOKEN_ENDPOINT);
        given(restTemplate.postForEntity(eq(TOKEN_ENDPOINT), any(), any())).willThrow(new HttpClientErrorException(HttpStatusCode.valueOf(409)));
        assertThrows(OrganizationmanagerException.class, () -> service.getSAaccessToken());
    }

}