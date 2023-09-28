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
package com.efs.sdk.organizationmanager.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;

import static org.mockito.BDDMockito.given;

class AuthHelperTest {

    private AuthHelper authHelper;

    @MockBean
    private JwtAuthenticationToken token;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        this.token = Mockito.mock(JwtAuthenticationToken.class);
        this.objectMapper = new ObjectMapper();
        this.authHelper = new AuthHelper();
    }

    @Test
    void givenSpacePublic_whenGetAuthenticationModel_thenOk() throws Exception {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("spc_all_public"));
        given(token.getAuthorities()).willReturn(authorities);

        AuthenticationModel expected = new AuthenticationModel();
        expected.setSpacePublicAccess(true);
        AuthenticationModel actual = authHelper.getAuthenticationModel(token);

        // set to null for simplicity in comparison
        actual.setToken(null);
        JSONAssert.assertEquals(objectMapper.writeValueAsString(expected), objectMapper.writeValueAsString(actual), false);
    }

    @Test
    void givenRoles_whenGetAuthenticationModel_thenOk() throws Exception {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("spc_all_public"), new SimpleGrantedAuthority("org_all_public"),
                new SimpleGrantedAuthority("org_test_admin"), new SimpleGrantedAuthority("org_test_access"), new SimpleGrantedAuthority("test_test_user"),
                new SimpleGrantedAuthority("SDK_ADMIN"));

        given(token.getAuthorities()).willReturn(authorities);

        AuthenticationModel expected = new AuthenticationModel();
        expected.setOrgaPublicAccess(true);
        expected.setSpacePublicAccess(true);
        expected.setSuperuser(true);
        expected.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization("org_test_admin"), new AuthEntityOrganization("org_test_access")});
        expected.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace("test_test_user")});

        AuthenticationModel actual = authHelper.getAuthenticationModel(token);
        // set to null for simplicity in comparison
        actual.setToken(null);
        JSONAssert.assertEquals(objectMapper.writeValueAsString(expected), objectMapper.writeValueAsString(actual), false);
    }

    @Test
    void givenSuperuser_whenIsSuperuser_thenTrue() throws Exception {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("SDK_ADMIN"));

        given(token.getAuthorities()).willReturn(authorities);

        AuthenticationModel expected = new AuthenticationModel();
        expected.setSuperuser(true);

        AuthenticationModel actual = authHelper.getAuthenticationModel(token);
        // set to null for simplicity in comparison
        actual.setToken(null);
        JSONAssert.assertEquals(objectMapper.writeValueAsString(expected), objectMapper.writeValueAsString(actual), false);
    }
}
