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
import com.efs.sdk.organizationmanager.core.OrganizationManagerService;
import com.efs.sdk.organizationmanager.core.auth.model.UserDTO;
import com.efs.sdk.organizationmanager.helper.AuthHelper;
import com.efs.sdk.organizationmanager.helper.RoleHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.efs.sdk.organizationmanager.commons.OrganizationmanagerException.ORGANIZATIONMANAGER_ERROR.UNABLE_GET_ROLE;
import static com.efs.sdk.organizationmanager.core.auth.OrgaUserController.ENDPOINT;
import static java.lang.String.*;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrgaUserController.class)
@ActiveProfiles("test")
class OrgaUserControllerTest {

    /* required for tests to run */
    @MockBean
    private AuthHelper authHelper;
    /* required for tests to run */
    @MockBean
    private JwtDecoder jwtDecoder;
    @Autowired
    private MockMvc mvc;
    @MockBean
    private OrganizationManagerService orgaManagerService;

    private static String getEndpoint(Long orgaId) {
        return ENDPOINT.replace("{orgaId}", valueOf(orgaId));
    }

    private static String getEndpointName(Long orgaId, String name) {
        return format("%s/name/%s", getEndpoint(orgaId), name);
    }

    private static String getEndpointEmail(Long orgaId, String email) {
        return format("%s/email/%s", getEndpoint(orgaId), email);
    }

    @Test
    void givenNoAuthentication_whenGetUsers_thenError() throws Exception {
        mvc.perform(get(getEndpoint(1L))).andExpect(status().is4xxClientError());
    }

    @Test
    void givenAuthentication_whenGetUsers_thenOk() throws Exception {
        mvc.perform(get(getEndpoint(1L)).with(jwt())).andExpect(status().isOk());
    }

    @Test
    void givenException_whenGetUsers_then5xxError() throws Exception {
        IllegalArgumentException except = new IllegalArgumentException("anything");
        given(orgaManagerService.listUsers(any(), anyLong())).willThrow(except);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(get(getEndpoint(1L)).with(jwt())).andExpect(status().is5xxServerError());
    }

    @Test
    void givenOrgamanException_whenGetUsers_then4xxError() throws Exception {
        given(orgaManagerService.listUsers(any(), anyLong())).willThrow(new OrganizationmanagerException(UNABLE_GET_ROLE));
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(get(getEndpoint(1L)).with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenNoAuthentication_whenSetRoles_thenError() throws Exception {
        String roleScopes = join(",", List.of(RoleHelper.OrganizationScopeRole.ACCESS.name()));
        mvc.perform(post(getEndpoint(1L) + "/" + randomUUID()).param("roleScopes", roleScopes)).andExpect(status().is4xxClientError());
    }

    @Test
    void givenAuthentication_whenSetRoles_thenOk() throws Exception {
        UserDTO user = new UserDTO();
        given(orgaManagerService.setRoles(any(), anyLong(), anyString(), any())).willReturn(user);
        String roleScopes = join(",", List.of(RoleHelper.OrganizationScopeRole.ACCESS.name()));
        mvc.perform(put(getEndpoint(1L) + "/" + randomUUID()).param("roleScopes", roleScopes).with(jwt())).andExpect(status().isOk());
    }

    @Test
    void givenAuthentication_whenSetRolesByName_thenOk() throws Exception {
        UserDTO user = new UserDTO();
        given(orgaManagerService.setRolesByName(any(), anyLong(), anyString(), any())).willReturn(user);
        String roleScopes = join(",", List.of(RoleHelper.OrganizationScopeRole.ACCESS.name()));
        mvc.perform(put(getEndpointName(1L, "test")).param("roleScopes", roleScopes).with(jwt())).andExpect(status().isOk());
    }

    @Test
    void givenAuthentication_whenSetRolesByEmail_thenOk() throws Exception {
        UserDTO user = new UserDTO();
        given(orgaManagerService.setRolesByEmail(any(), anyLong(), anyString(), any())).willReturn(user);
        String roleScopes = join(",", List.of(RoleHelper.OrganizationScopeRole.ACCESS.name()));
        mvc.perform(put(getEndpointEmail(1L, "email")).param("roleScopes", roleScopes).with(jwt())).andExpect(status().isOk());
    }

    @Test
    void givenNoAuthentication_whenSetRolesByName_thenError() throws Exception {
        String roleScopes = join(",", List.of(RoleHelper.OrganizationScopeRole.ACCESS.name()));
        mvc.perform(post(getEndpointName(1L, "name")).param("roleScopes", roleScopes)).andExpect(status().is4xxClientError());
    }

    @Test
    void givenNoAuthentication_whenSetRolesByEmail_thenError() throws Exception {
        String roleScopes = join(",", List.of(RoleHelper.OrganizationScopeRole.ACCESS.name()));
        mvc.perform(post(getEndpointEmail(1L, "email")).param("roleScopes", roleScopes)).andExpect(status().is4xxClientError());
    }

    @Test
    void givenNoRoleScopes_whenSetRolesByEmail_thenBadRequest() throws Exception {
        mvc.perform(put(getEndpointEmail(1L, "test.test@abc.com"))
                        .with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenNoRoleScopes_whenSetRolesByUsername_thenBadRequest() throws Exception {
        mvc.perform(put(getEndpointName(1L, "username"))
                        .with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenNoRoleScopes_whenSetRolesByUserId_thenBadRequest() throws Exception {
        mvc.perform(put(getEndpoint(1L) + "/123")
                        .with(jwt()))
                .andExpect(status().isBadRequest());
    }
}
