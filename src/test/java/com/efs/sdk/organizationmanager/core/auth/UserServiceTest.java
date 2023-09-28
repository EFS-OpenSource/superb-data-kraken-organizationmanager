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
import com.efs.sdk.organizationmanager.core.auth.model.RoleDTO;
import com.efs.sdk.organizationmanager.core.auth.model.UserDTO;
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.core.space.model.Space;
import com.efs.sdk.organizationmanager.helper.RoleHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;
import java.util.UUID;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;


@RestClientTest(UserService.class)
class UserServiceTest {
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private UserService service;
    @MockBean
    private AuthService authService;
    @MockBean
    private RoleHelper roleHelper;
    @MockBean
    private RoleService roleService;
    private MockRestServiceServer mockServer;
    private final String realmEndpoint = "http://localhost:8080/auth/admin/realms/myrealm";

    @BeforeEach
    public void setup() {
        this.mockServer = MockRestServiceServer.createServer(restTemplate);
        this.authService = Mockito.mock(AuthService.class);
        this.roleHelper = Mockito.mock(RoleHelper.class);
        this.roleService = Mockito.mock(RoleService.class);
        this.service = new UserService(restTemplate, authService, roleHelper, roleService, realmEndpoint);
    }

    @Test
    void givenRolePresent_whenAssignOrgaRole_thenOk() throws Exception {
        RoleHelper.OrganizationScopeRole roleScope = RoleHelper.OrganizationScopeRole.ACCESS;

        Organization orga = new Organization();
        orga.setName("test");

        given(authService.getSAaccessToken()).willReturn("test-token");
        String roleName = format("org_%s_%s", orga.getName(), roleScope.name()).toLowerCase(Locale.getDefault());
        given(roleHelper.buildOrganizationRole(any(Organization.class), any())).willReturn(roleName);
        RoleDTO role = new RoleDTO();
        role.setName(roleName);
        given(roleService.getRoles(anyString())).willReturn(new RoleDTO[]{role});

        UserDTO user = new UserDTO();
        user.setId(UUID.randomUUID().toString());

        String userAssignEndpoint = format("%s/users/%s/role-mappings/realm", realmEndpoint, user.getId());
        this.mockServer.expect(requestTo(userAssignEndpoint)).andExpect(method(POST)).andRespond(withStatus(OK));

        assertDoesNotThrow(() -> service.assignUserToRole(orga, roleScope, user));
    }

    @Test
    void givenRolePresent_whenAssignSpaceRole_thenOk() throws Exception {
        RoleHelper.SpaceScopeRole roleScope = RoleHelper.SpaceScopeRole.USER;

        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("orga");

        Space space = new Space();
        space.setOrganizationId(orga.getId());
        space.setName("space");

        given(authService.getSAaccessToken()).willReturn("test-token");
        String roleName = format("%s_%s_%s", orga.getName(), space.getName(), roleScope.name()).toLowerCase(Locale.getDefault());
        given(roleHelper.buildSpaceRole(any(), any(), any())).willReturn(roleName);
        RoleDTO role = new RoleDTO();
        role.setName(roleName);
        given(roleService.getRoles(anyString())).willReturn(new RoleDTO[]{role});

        UserDTO user = new UserDTO();
        user.setId(UUID.randomUUID().toString());

        String userAssignEndpoint = format("%s/users/%s/role-mappings/realm", realmEndpoint, user.getId());
        this.mockServer.expect(requestTo(userAssignEndpoint)).andExpect(method(POST)).andRespond(withStatus(OK));

        assertDoesNotThrow(() -> service.assignUserToRole(orga, space, roleScope, user));
    }

    @Test
    void givenRoleNotPresent_whenAssignOrgaRole_thenError() throws Exception {
        RoleHelper.OrganizationScopeRole roleScope = RoleHelper.OrganizationScopeRole.ACCESS;

        Organization orga = new Organization();
        orga.setName("test");

        given(authService.getSAaccessToken()).willReturn("test-token");
        String roleName = format("org_%s_%s", orga.getName(), roleScope.name()).toLowerCase(Locale.getDefault());
        given(roleHelper.buildOrganizationRole(any(Organization.class), any())).willReturn(roleName);

        given(roleService.getRoles(anyString())).willReturn(new RoleDTO[0]);

        UserDTO user = new UserDTO();
        user.setId(UUID.randomUUID().toString());

        String userAssignEndpoint = format("%s/users/%s/role-mappings/realm", realmEndpoint, user.getId());
        this.mockServer.expect(requestTo(userAssignEndpoint)).andExpect(method(POST)).andRespond(withStatus(OK));

        assertThrows(OrganizationmanagerException.class, () -> service.assignUserToRole(orga, roleScope, user));
    }

    @Test
    void givenRoleNotPresent_whenAssignSpaceRole_thenError() throws Exception {
        RoleHelper.SpaceScopeRole roleScope = RoleHelper.SpaceScopeRole.USER;

        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("orga");

        Space space = new Space();
        space.setOrganizationId(orga.getId());
        space.setName("space");

        given(authService.getSAaccessToken()).willReturn("test-token");
        String roleName = format("%s_%s_%s", orga.getName(), space.getName(), roleScope.name()).toLowerCase(Locale.getDefault());
        given(roleHelper.buildSpaceRole(any(), any(), any())).willReturn(roleName);

        given(roleService.getRoles(anyString())).willReturn(new RoleDTO[0]);

        UserDTO user = new UserDTO();
        user.setId(UUID.randomUUID().toString());

        String userAssignEndpoint = format("%s/users/%s/role-mappings/realm", realmEndpoint, user.getId());
        this.mockServer.expect(requestTo(userAssignEndpoint)).andExpect(method(POST)).andRespond(withStatus(OK));

        assertThrows(OrganizationmanagerException.class, () -> service.assignUserToRole(orga, space, roleScope, user));
    }


    @Test
    void givenRolePresent_whenwithdrawOrgaRole_thenOk() throws Exception {
        RoleHelper.OrganizationScopeRole roleScope = RoleHelper.OrganizationScopeRole.ACCESS;

        Organization orga = new Organization();
        orga.setName("test");

        given(authService.getSAaccessToken()).willReturn("test-token");
        String roleName = format("org_%s_%s", orga.getName(), roleScope.name()).toLowerCase(Locale.getDefault());
        given(roleHelper.buildOrganizationRole(any(Organization.class), any())).willReturn(roleName);
        RoleDTO role = new RoleDTO();
        role.setName(roleName);
        given(roleService.getRoles(anyString())).willReturn(new RoleDTO[]{role});

        UserDTO user = new UserDTO();
        user.setId(UUID.randomUUID().toString());

        String userAssignEndpoint = format("%s/users/%s/role-mappings/realm", realmEndpoint, user.getId());
        this.mockServer.expect(requestTo(userAssignEndpoint)).andExpect(method(DELETE)).andRespond(withStatus(OK));

        assertDoesNotThrow(() -> service.withdrawUserFromRole(orga, roleScope, user));
    }
}
