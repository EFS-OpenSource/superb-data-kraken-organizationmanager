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
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.core.space.model.Space;
import com.efs.sdk.organizationmanager.helper.RoleHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RestClientTest(RoleService.class)
class RoleServiceTest {
    private final String ACCESS_TOKEN = "test";
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private RoleService service;
    private MockRestServiceServer mockServer;
    private String roleEndpoint;
    @MockBean
    private RoleHelper roleHelper;

    @BeforeEach
    public void setup() {
        this.mockServer = MockRestServiceServer.createServer(restTemplate);
        this.roleHelper = Mockito.mock(RoleHelper.class);
        String realmEndpoint = "http://localhost:8080/auth/admin/realms/efs-sdk";
        this.roleEndpoint = format("%s/roles", realmEndpoint);
        this.service = new RoleService(restTemplate, roleHelper, realmEndpoint);
    }

    @Test
    void givenSpace_whenCreateRoles_thenOk() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space space = new Space();
        space.setName("test");
        space.setId(1L);
        space.setOrganizationId(orga.getId());
        String jwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9" +
                ".eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE2MjAxMTg1NTUsImV4cCI6MTY1MTY1NDU1NSwiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIkdpdmVuTmFtZSI6IkpvaG5ueSIsIlN1cm5hbWUiOiJSb2NrZXQiLCJFbWFpbCI6Impyb2NrZXRAZXhhbXBsZS5jb20iLCJSb2xlIjpbIk1hbmFnZXIiLCJQcm9qZWN0IEFkbWluaXN0cmF0b3IiXX0.yL6MTJ24A2KtU1RgUCnAuh8Q0PRr9PxtTgQLPWF_0r4";

        String roleName = format("%s_%s_user", orga.getName(), space.getName());
        given(roleHelper.getRoles(any(), any())).willReturn(List.of(roleName));

        String existingRoles = "[]";
        this.mockServer.expect(requestTo(roleEndpoint)).andExpect(method(GET)).andRespond(withStatus(OK).contentType(MediaType.APPLICATION_JSON).body(existingRoles));
        this.mockServer.expect(requestTo(roleEndpoint)).andExpect(method(POST)).andRespond(withStatus(OK));
        assertDoesNotThrow(() -> service.createRoles(jwt, orga, space));
    }


    @Test
    void givenRoleNotFound_whenDeleteOrganizationRoles_thenDoNothing() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        String existingRoles = "[]";
        this.mockServer.expect(requestTo(roleEndpoint)).andExpect(method(GET)).andRespond(withStatus(OK).contentType(MediaType.APPLICATION_JSON).body(existingRoles));

        assertDoesNotThrow(() -> service.deleteRoles(ACCESS_TOKEN, orga));
    }

    @Test
    void givenRoleNotFound_whenDeleteSpaceRoles_thenDoNothing() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space space = new Space();
        space.setName("test");
        space.setId(1L);
        space.setOrganizationId(orga.getId());

        String existingRoles = "[]";
        this.mockServer.expect(requestTo(roleEndpoint)).andExpect(method(GET)).andRespond(withStatus(OK).contentType(MediaType.APPLICATION_JSON).body(existingRoles));

        assertDoesNotThrow(() -> service.deleteRoles(ACCESS_TOKEN, orga, space));
    }

    @Test
    void givenUnableFetchingRole_whenDeleteOrganizationRoles_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        String roleName = format("org_%s_access", orga.getName());
        given(roleHelper.getRoles(any(Organization.class))).willReturn(List.of(roleName));
        this.mockServer.expect(requestTo(roleEndpoint)).andExpect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.CONFLICT));

        assertThrows(OrganizationmanagerException.class, () -> service.deleteRoles(ACCESS_TOKEN, orga));
    }

    @Test
    void givenOrganization_whenCreateRoles_thenOk() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");
        String roleName = format("org_%s_access", orga.getName());
        given(roleHelper.getRoles(any(Organization.class))).willReturn(List.of(roleName));

        String existingRoles = "[]";
        this.mockServer.expect(requestTo(roleEndpoint)).andExpect(method(GET)).andRespond(withStatus(OK).contentType(MediaType.APPLICATION_JSON).body(existingRoles));
        this.mockServer.expect(requestTo(roleEndpoint)).andExpect(method(POST)).andRespond(withStatus(OK));
        assertDoesNotThrow(() -> service.createRoles(ACCESS_TOKEN, orga));
    }

    @Test
    void givenException_whenCreateRoles_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        String roleName = format("org_%s_access", orga.getName());
        given(roleHelper.getRoles(any(Organization.class))).willReturn(List.of(roleName));
        String existingRoles = "[]";
        this.mockServer.expect(requestTo(roleEndpoint)).andExpect(method(GET)).andRespond(withStatus(OK).contentType(MediaType.APPLICATION_JSON).body(existingRoles));
        this.mockServer.expect(requestTo(roleEndpoint)).andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.CONFLICT));

        assertThrows(OrganizationmanagerException.class, () -> service.createRoles(ACCESS_TOKEN, orga));
    }

    @Test
    void givenRoleExists_whenCreateRoles_thenDoNothing() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        String roleName = format("org_%s_access", orga.getName());
        given(roleHelper.getRoles(any(Organization.class))).willReturn(List.of(roleName));

        String existingRolesResponse = format("""
                [{
                    "id": "%s",
                    "name": "%s",
                    "composite": false,
                    "clientRole": false,
                    "containerId": "efs-sdk",
                    "attributes": {}
                }]""", UUID.randomUUID(), roleName);

        this.mockServer.expect(requestTo(roleEndpoint)).andExpect(method(GET)).andRespond(withStatus(OK).contentType(MediaType.APPLICATION_JSON).body(existingRolesResponse));

        assertDoesNotThrow(() -> service.createRoles(ACCESS_TOKEN, orga));
    }

    @Test
    void givenOrganization_whenDeleteRoles_thenOk() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        String roleName = format("org_%s_access", orga.getName());
        given(roleHelper.getRoles(any(Organization.class))).willReturn(List.of(roleName));

        String existingRoles = "[]";
        this.mockServer.expect(requestTo(roleEndpoint)).andExpect(method(GET)).andRespond(withStatus(OK).contentType(MediaType.APPLICATION_JSON).body(existingRoles));
        this.mockServer.expect(requestTo(format("%s/%s", roleEndpoint, roleName))).andExpect(method(DELETE)).andRespond(withStatus(OK));
        assertDoesNotThrow(() -> service.deleteRoles(ACCESS_TOKEN, orga));
    }

    @Test
    void givenSpace_whenDeleteRoles_thenOk() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space space = new Space();
        space.setName("test");
        space.setId(1L);
        space.setOrganizationId(orga.getId());

        String roleName = format("%s_%s_user", orga.getName(), space.getName());
        given(roleHelper.getRoles(any(), any())).willReturn(List.of(roleName));

        String existingRolesResponse = format("""
                [{
                    "id": "%s",
                    "name": "%s",
                    "composite": false,
                    "clientRole": false,
                    "containerId": "efs-sdk",
                    "attributes": {}
                }]""", UUID.randomUUID(), roleName);
        this.mockServer.expect(requestTo(roleEndpoint)).andExpect(method(GET)).andRespond(withStatus(OK).contentType(MediaType.APPLICATION_JSON).body(existingRolesResponse));
        this.mockServer.expect(requestTo(format("%s/%s", roleEndpoint, roleName))).andExpect(method(DELETE)).andRespond(withStatus(OK));
        assertDoesNotThrow(() -> service.deleteRoles(ACCESS_TOKEN, orga, space));
    }

    @Test
    void givenExistingOrganizationRole_whenDeleteRoles_thenOk() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        String roleName = format("org_%s_access", orga.getName());
        given(roleHelper.getRoles(any(Organization.class))).willReturn(List.of(roleName));

        String existingRolesResponse = format("""
                [{
                    "id": "%s",
                    "name": "%s",
                    "composite": false,
                    "clientRole": false,
                    "containerId": "efs-sdk",
                    "attributes": {}
                }]""", UUID.randomUUID(), roleName);
        this.mockServer.expect(requestTo(roleEndpoint)).andExpect(method(GET)).andRespond(withStatus(OK).contentType(MediaType.APPLICATION_JSON).body(existingRolesResponse));
        this.mockServer.expect(requestTo(format("%s/%s", roleEndpoint, roleName))).andExpect(method(DELETE)).andRespond(withStatus(OK));
        assertDoesNotThrow(() -> service.deleteRoles(ACCESS_TOKEN, orga));
    }

    @Test
    void givenNonExistingOrganizationRole_whenDeleteRoles_thenOk() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space space = new Space();
        space.setName("test");
        space.setId(1L);
        space.setOrganizationId(orga.getId());

        String roleName = format("org_%s_access", orga.getName());
        given(roleHelper.getRoles(any(Organization.class))).willReturn(List.of(roleName));

        String existingRolesResponse = "[]";
        this.mockServer.expect(requestTo(roleEndpoint)).andExpect(method(GET)).andRespond(withStatus(OK).contentType(MediaType.APPLICATION_JSON).body(existingRolesResponse));
        this.mockServer.expect(requestTo(format("%s/%s", roleEndpoint, roleName))).andExpect(method(DELETE)).andRespond(withStatus(OK));
        assertDoesNotThrow(() -> service.deleteRoles(ACCESS_TOKEN, orga));
    }


}

