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
package com.efs.sdk.organizationmanager.core;

import com.efs.sdk.common.domain.model.State;
import com.efs.sdk.organizationmanager.commons.OrganizationmanagerException;
import com.efs.sdk.organizationmanager.core.auth.AuthService;
import com.efs.sdk.organizationmanager.core.auth.RoleService;
import com.efs.sdk.organizationmanager.core.auth.UserService;
import com.efs.sdk.organizationmanager.core.auth.model.OrganizationUserDTO;
import com.efs.sdk.organizationmanager.core.auth.model.SpaceUserDTO;
import com.efs.sdk.organizationmanager.core.clients.AbstractServiceRestClient;
import com.efs.sdk.organizationmanager.core.events.EventPublisher;
import com.efs.sdk.organizationmanager.core.organization.OrganizationService;
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.core.space.SpaceService;
import com.efs.sdk.organizationmanager.core.space.model.Space;
import com.efs.sdk.organizationmanager.core.userrequest.UserRequestService;
import com.efs.sdk.organizationmanager.core.userrequest.model.OrganizationUserRequest;
import com.efs.sdk.organizationmanager.core.userrequest.model.SpaceUserRequest;
import com.efs.sdk.organizationmanager.core.userrequest.model.UserRequestState;
import com.efs.sdk.organizationmanager.helper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.*;

import static com.efs.sdk.organizationmanager.commons.OrganizationmanagerException.ORGANIZATIONMANAGER_ERROR.GET_SINGLE_SPACE_NOT_FOUND;
import static com.efs.sdk.organizationmanager.helper.AuthConfiguration.GET;
import static com.efs.sdk.organizationmanager.utils.TestUtils.assumeAuthToken;
import static com.efs.sdk.organizationmanager.utils.TestUtils.getJwt;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

class OrganizationManagerServiceTest {

    @MockBean
    private OrganizationService orgaService;
    @MockBean
    private SpaceService spaceService;
    @MockBean
    private RoleService roleService;
    @MockBean
    private AuthService authService;
    @MockBean
    private UserService userService;
    @MockBean
    private UserRequestService userRequestService;
    @MockBean
    private RoleHelper roleHelper;

    private OrganizationManagerService service;
    @MockBean
    private EventPublisher eventPublisher;

    @BeforeEach
    public void setup() {
        this.orgaService = Mockito.mock(OrganizationService.class);
        this.spaceService = Mockito.mock(SpaceService.class);
        this.roleService = Mockito.mock(RoleService.class);
        this.authService = Mockito.mock(AuthService.class);
        this.userService = Mockito.mock(UserService.class);
        this.roleHelper = Mockito.mock(RoleHelper.class);
        this.userRequestService = Mockito.mock(UserRequestService.class);
        EntityConverter converter = Mockito.mock(EntityConverter.class);
        List<AbstractServiceRestClient> serviceRestClients = List.of(Mockito.mock(AbstractServiceRestClient.class));
        this.service = new OrganizationManagerService(orgaService, spaceService, roleService, authService, userService, userRequestService, roleHelper,
                serviceRestClients, converter, eventPublisher);
    }

    @Test
    void givenNoSuperuser_whenDeleteSpace_thenError() {
        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setName("test");
        spc.setId(1L);
        spc.setOrganizationId(orga.getId());

        assertThrows(OrganizationmanagerException.class, () -> service.deleteSpace(authModel, orga.getId(), spc.getId()), "Expected OrganizationManager " +
                "throw, but not accrued.");
    }

    @Test
    void givenSpaceNotFound_whenDeleteSpace_thenError() throws OrganizationmanagerException {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setName("test");
        spc.setId(1L);
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);

        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willThrow(new OrganizationmanagerException(GET_SINGLE_SPACE_NOT_FOUND));

        assertThrows(OrganizationmanagerException.class, () -> service.deleteSpace(authModel, orga.getId(), spc.getId()), "Expected OrganizationManager " +
                "throw, but not accrued.");
    }

    @Test
    void givenSuperuserSpaceFound_whenDeleteSpace_thenOk() throws OrganizationmanagerException {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setName("test");
        spc.setId(1L);
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);

        given(authService.getSAaccessToken()).willReturn("test");
        given(roleService.deleteRoles(anyString(), any(Organization.class), any(Space.class))).willReturn(true);
        given(spaceService.deleteSpaceEntity(any(Space.class))).willReturn(true);
        assertTrue(service.deleteSpace(authModel, orga.getId(), spc.getId()));
    }

    // TODO: rewrite with exceptions
    //    @Test
    //    void givenNotDeleteSpaceEntity_whenDeleteSpace_thenFalse() throws OrganizationmanagerException {
    //        Organization orga = new Organization();
    //        orga.setId(1L);
    //        orga.setName("test");
    //
    //        Space spc = new Space();
    //        spc.setName("test");
    //        spc.setId(1L);
    //        spc.setOrganizationId(orga.getId());
    //
    //        AuthenticationModel authModel = new AuthenticationModel();
    //        authModel.setSuperuser(true);
    //
    //        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
    //        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);
    //
    //        given(authService.getSAaccessToken()).willReturn("test");
    //        given(spaceService.deleteSpaceEntity(any(Space.class))).willReturn(false);
    //        given(roleService.deleteRoles(anyString(), any(Organization.class), any(Space.class))).willReturn(true);
    //        assertFalse(service.deleteSpace(authModel, orga.getId(), spc.getId()));
    //    }

    // TODO: rewrite with exceptions
    //    @Test
    //    void givenNotDeleteSpaceRoles_whenDeleteSpace_thenFalse() throws OrganizationmanagerException {
    //        Organization orga = new Organization();
    //        orga.setId(1L);
    //        orga.setName("test");
    //
    //        Space spc = new Space();
    //        spc.setName("test");
    //        spc.setId(1L);
    //        spc.setOrganizationId(orga.getId());
    //
    //        AuthenticationModel authModel = new AuthenticationModel();
    //        authModel.setSuperuser(true);
    //
    //        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
    //        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);
    //
    //        given(authService.getSAaccessToken()).willReturn("test");
    //        given(spaceService.deleteSpaceEntity(any(Space.class))).willReturn(true);
    //        given(roleService.deleteRoles(anyString(), any(Organization.class), any(Space.class))).willReturn(false);
    //        assertFalse(service.deleteSpace(authModel, orga.getId(), spc.getId()));
    //    }

    @Test
    void givenSuperuser_whenListUsersInOrga_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        given(userService.getUsers(any())).willReturn(Set.of(userDTO));
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertEquals(1, service.listUsers(authModel, orga.getId()).size());
    }

    @Test
    void givenAdmin_whenListUsersInOrga_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        given(userService.getUsers(any())).willReturn(Set.of(userDTO));
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertEquals(1, service.listUsers(authModel, orga.getId()).size());
    }

    @Test
    void givenNoAdmin_whenListUsersInOrga_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ACCESS).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        given(userService.getUsers(any())).willReturn(Set.of(userDTO));
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertThrows(OrganizationmanagerException.class, () -> service.listUsers(authModel, orga.getId()));
    }

    @Test
    void givenSuperuser_whenSetOrgaRoles_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId(randomUUID().toString());
        userDTO.setUsername("test");
        userDTO.setEmail("test.test@abc.com");

        given(userService.getUser(anyString())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertEquals(service.setRoles(authModel, orga.getId(), userDTO.getId(), List.of(RoleHelper.OrganizationScopeRole.ACCESS)), userDTO);
    }

    @Test
    void givenAdmin_whenSetOrgaRoles_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));
        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId(randomUUID().toString());
        userDTO.setUsername("test");
        userDTO.setEmail("test.test@abc.com");

        given(userService.getUser(anyString())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertEquals(service.setRoles(authModel, orga.getId(), userDTO.getId(), List.of(RoleHelper.OrganizationScopeRole.ACCESS)), userDTO);
    }

    @Test
    void givenNoAdmin_whenSetOrgaRoles_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ACCESS).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId(randomUUID().toString());
        userDTO.setUsername("test");
        userDTO.setEmail("test.test@abc.com");

        given(userService.getUser(anyString())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertThrows(OrganizationmanagerException.class, () -> service.setRoles(authModel, orga.getId(), userDTO.getId(),
                List.of(RoleHelper.OrganizationScopeRole.ACCESS)));
    }

    @Test
    void givenSuperuser_whenSetOrgaRolesByName_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId(randomUUID().toString());
        userDTO.setUsername("test");
        userDTO.setEmail("test.test@abc.com");

        given(userService.getUserByName(anyString())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertEquals(service.setRolesByName(authModel, orga.getId(), userDTO.getUsername(), List.of(RoleHelper.OrganizationScopeRole.ACCESS)), userDTO);
    }

    @Test
    void givenAdmin_whenSetOrgaRolesByName_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId(randomUUID().toString());
        userDTO.setUsername("test");
        userDTO.setEmail("test.test@abc.com");

        given(userService.getUserByName(anyString())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertEquals(service.setRolesByName(authModel, orga.getId(), userDTO.getUsername(), List.of(RoleHelper.OrganizationScopeRole.ACCESS)), userDTO);
    }

    @Test
    void givenNoAdmin_whenSetOrgaRolesByName_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ACCESS).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId(randomUUID().toString());
        userDTO.setUsername("test");
        userDTO.setEmail("test.test@abc.com");

        given(userService.getUserByName(anyString())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertThrows(OrganizationmanagerException.class, () -> service.setRolesByName(authModel, orga.getId(), userDTO.getUsername(),
                List.of(RoleHelper.OrganizationScopeRole.ACCESS)));
    }

    @Test
    void givenSuperuser_whenSetOrgaRolesByEmail_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId(randomUUID().toString());
        userDTO.setUsername("test");
        userDTO.setEmail("test.test@abc.com");

        given(userService.getUserByEmail(anyString())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertEquals(service.setRolesByEmail(authModel, orga.getId(), userDTO.getEmail(), List.of(RoleHelper.OrganizationScopeRole.ACCESS)), userDTO);
    }

    @Test
    void givenAdmin_whenSetOrgaRolesByEmail_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId(randomUUID().toString());
        userDTO.setUsername("test");
        userDTO.setEmail("test.test@abc.com");

        given(userService.getUserByEmail(anyString())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertEquals(service.setRolesByEmail(authModel, orga.getId(), userDTO.getEmail(), List.of(RoleHelper.OrganizationScopeRole.ACCESS)), userDTO);
    }

    @Test
    void givenNoAdmin_whenSetOrgaRolesByEmail_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ACCESS).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId(randomUUID().toString());
        userDTO.setUsername("test");
        userDTO.setEmail("test.test@abc.com");

        given(userService.getUserByEmail(anyString())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertThrows(OrganizationmanagerException.class, () -> service.setRolesByEmail(authModel, orga.getId(), userDTO.getEmail(),
                List.of(RoleHelper.OrganizationScopeRole.ACCESS)));
    }

    @Test
    void givenSuperuser_whenListOrgaRequests_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);

        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setState(UserRequestState.OPEN);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(randomUUID().toString());
        List<OrganizationUserRequest> requests = List.of(orgaRequest);

        given(userRequestService.listUserRequests(any())).willReturn(requests);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertEquals(service.listOrganizationRequests(authModel, orga.getId()), requests);
    }

    @Test
    void givenAdmin_whenListOrgaRequests_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});

        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setState(UserRequestState.OPEN);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(randomUUID().toString());
        List<OrganizationUserRequest> requests = List.of(orgaRequest);

        given(userRequestService.listUserRequests(any())).willReturn(requests);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertEquals(service.listOrganizationRequests(authModel, orga.getId()), requests);
    }

    @Test
    void givenNoAdmin_whenListOrgaRequests_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ACCESS).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setState(UserRequestState.OPEN);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(randomUUID().toString());
        List<OrganizationUserRequest> requests = List.of(orgaRequest);

        given(userRequestService.listUserRequests(any())).willReturn(requests);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertThrows(OrganizationmanagerException.class, () -> service.listOrganizationRequests(authModel, orga.getId()));
    }

    @Test
    void givenSuperuser_whenAcceptOrgaRequest_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setState(UserRequestState.OPEN);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(randomUUID().toString());

        given(userRequestService.acceptUserRequest(any(), anyLong())).willReturn(orgaRequest);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        assumeAuthToken();

        assertEquals(service.acceptOrganizationRequest(authModel, orga.getId(), orgaRequest.getId()), orgaRequest);
    }

    @Test
    void givenAdmin_whenAcceptOrgaRequest_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setState(UserRequestState.OPEN);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(randomUUID().toString());

        given(userRequestService.acceptUserRequest(any(), anyLong())).willReturn(orgaRequest);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        assumeAuthToken();

        assertEquals(service.acceptOrganizationRequest(authModel, orga.getId(), orgaRequest.getId()), orgaRequest);
    }

    @Test
    void givenNoAdmin_whenAcceptOrgaRequest_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ACCESS).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setState(UserRequestState.OPEN);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(randomUUID().toString());

        given(userRequestService.acceptUserRequest(any(), anyLong())).willReturn(orgaRequest);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertThrows(OrganizationmanagerException.class, () -> service.acceptOrganizationRequest(authModel, orga.getId(), orgaRequest.getId()));
    }

    @Test
    void givenSuperuser_whenDeclineOrgaRequest_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);

        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setState(UserRequestState.OPEN);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(UUID.randomUUID().toString());

        given(userRequestService.declineUserRequest(any(), anyLong())).willReturn(orgaRequest);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertEquals(service.declineOrganizationRequest(authModel, orga.getId(), orgaRequest.getId()), orgaRequest);
    }

    @Test
    void givenAdmin_whenDeclineOrgaRequest_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});

        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setState(UserRequestState.OPEN);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(UUID.randomUUID().toString());

        given(userRequestService.declineUserRequest(any(), anyLong())).willReturn(orgaRequest);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertEquals(service.declineOrganizationRequest(authModel, orga.getId(), orgaRequest.getId()), orgaRequest);
    }

    @Test
    void givenNoAdmin_whenDeclineOrgaRequest_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ACCESS).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setState(UserRequestState.OPEN);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(UUID.randomUUID().toString());

        given(userRequestService.declineUserRequest(any(), anyLong())).willReturn(orgaRequest);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertThrows(OrganizationmanagerException.class, () -> service.declineOrganizationRequest(authModel, orga.getId(), orgaRequest.getId()));
    }


    @Test
    void givenSuperuser_whenSetSpaceRoles_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        SpaceUserDTO userDTO = new SpaceUserDTO();
        userDTO.setId(randomUUID().toString());
        userDTO.setUsername("test");
        userDTO.setEmail("test.test@abc.com");

        given(userService.getUser(anyString())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);

        assertEquals(service.setRoles(authModel, orga.getId(), spc.getId(), userDTO.getId(), List.of(RoleHelper.SpaceScopeRole.USER)), userDTO);
    }

    @Test
    void givenAdmin_whenSetSpaceRoles_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        SpaceUserDTO userDTO = new SpaceUserDTO();
        userDTO.setId(randomUUID().toString());
        userDTO.setUsername("test");
        userDTO.setEmail("test.test@abc.com");

        given(userService.getUser(anyString())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);

        assertEquals(service.setRoles(authModel, orga.getId(), spc.getId(), userDTO.getId(), List.of(RoleHelper.SpaceScopeRole.USER)), userDTO);
    }

    @Test
    void givenNoAdmin_whenSetSpaceRoles_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ACCESS).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        SpaceUserDTO userDTO = new SpaceUserDTO();
        userDTO.setId(randomUUID().toString());
        userDTO.setUsername("test");
        userDTO.setEmail("test.test@abc.com");

        given(userService.getUser(anyString())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);

        assertThrows(OrganizationmanagerException.class, () -> service.setRoles(authModel, orga.getId(), spc.getId(), userDTO.getId(),
                List.of(RoleHelper.SpaceScopeRole.USER)));
    }

    @Test
    void givenSuperuser_whenSetSpaceRolesByName_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        SpaceUserDTO userDTO = new SpaceUserDTO();
        userDTO.setId(randomUUID().toString());
        userDTO.setUsername("test");
        userDTO.setEmail("test.test@abc.com");

        given(userService.getUserByName(anyString())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);

        assertEquals(service.setRolesByName(authModel, orga.getId(), spc.getId(), userDTO.getUsername(), List.of(RoleHelper.SpaceScopeRole.USER)), userDTO);
    }

    @Test
    void givenAdmin_whenSetSpaceRolesByName_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        SpaceUserDTO userDTO = new SpaceUserDTO();
        userDTO.setId(randomUUID().toString());
        userDTO.setUsername("test");
        userDTO.setEmail("test.test@abc.com");

        given(userService.getUserByName(anyString())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);

        assertEquals(service.setRolesByName(authModel, orga.getId(), spc.getId(), userDTO.getUsername(), List.of(RoleHelper.SpaceScopeRole.USER)), userDTO);
    }

    @Test
    void givenNoAdmin_whenSetSpaceRolesByName_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ACCESS).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        SpaceUserDTO userDTO = new SpaceUserDTO();
        userDTO.setId(randomUUID().toString());
        userDTO.setUsername("test");
        userDTO.setEmail("test.test@abc.com");

        given(userService.getUserByName(anyString())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);

        assertThrows(OrganizationmanagerException.class, () -> service.setRolesByName(authModel, orga.getId(), spc.getId(), userDTO.getUsername(),
                List.of(RoleHelper.SpaceScopeRole.USER)));
    }

    @Test
    void givenSuperuser_whenSetSpaceRolesByEmail_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        SpaceUserDTO userDTO = new SpaceUserDTO();
        userDTO.setId(randomUUID().toString());
        userDTO.setUsername("test");
        userDTO.setEmail("test.test@abc.com");

        given(userService.getUserByEmail(anyString())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);

        assertEquals(service.setRolesByEmail(authModel, orga.getId(), spc.getId(), userDTO.getEmail(), List.of(RoleHelper.SpaceScopeRole.USER)), userDTO);
    }

    @Test
    void givenAdmin_whenSetSpaceRolesByEmail_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        SpaceUserDTO userDTO = new SpaceUserDTO();
        userDTO.setId(randomUUID().toString());
        userDTO.setUsername("test");
        userDTO.setEmail("test.test@abc.com");

        given(userService.getUserByEmail(anyString())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);

        assertEquals(service.setRolesByEmail(authModel, orga.getId(), spc.getId(), userDTO.getEmail(), List.of(RoleHelper.SpaceScopeRole.USER)), userDTO);
    }

    @Test
    void givenNoAdmin_whenSetSpaceRolesByEmail_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ACCESS).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        SpaceUserDTO userDTO = new SpaceUserDTO();
        userDTO.setId(randomUUID().toString());
        userDTO.setUsername("test");
        userDTO.setEmail("test.test@abc.com");

        given(userService.getUserByEmail(anyString())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);

        assertThrows(OrganizationmanagerException.class, () -> service.setRolesByEmail(authModel, orga.getId(), spc.getId(), userDTO.getEmail(),
                List.of(RoleHelper.SpaceScopeRole.USER)));
    }

    @Test
    void givenSuperuser_whenListSpaceRequests_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);

        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(randomUUID().toString());
        List<SpaceUserRequest> requests = List.of(spaceRequest);

        given(userRequestService.listUserRequests(any(Organization.class), any(Space.class))).willReturn(requests);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);

        assertEquals(requests, service.listSpaceRequests(authModel, orga.getId(), spc.getId()));
    }

    @Test
    void givenAdmin_whenListSpaceRequests_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});

        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(randomUUID().toString());
        List<SpaceUserRequest> requests = List.of(spaceRequest);

        given(userRequestService.listUserRequests(any(), any(Space.class))).willReturn(requests);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);

        assertEquals(requests, service.listSpaceRequests(authModel, orga.getId(), spc.getId()));
    }

    @Test
    void givenNoAdmin_whenListSpaceRequests_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ACCESS).toLowerCase(Locale.getDefault()))});

        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(randomUUID().toString());
        List<SpaceUserRequest> requests = List.of(spaceRequest);

        given(userRequestService.listUserRequests(any(), any(Space.class))).willReturn(requests);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);

        assertThrows(OrganizationmanagerException.class, () -> service.listSpaceRequests(authModel, orga.getId(), spc.getId()));
    }

    @Test
    void givenSuperuser_whenAcceptSpaceRequest_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(randomUUID().toString());

        given(userRequestService.acceptUserRequest(any(), any(), anyLong())).willReturn(spaceRequest);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);
        assumeAuthToken();

        assertEquals(service.acceptSpaceRequest(authModel, orga.getId(), spc.getId(), spaceRequest.getId()), spaceRequest);
    }

    @Test
    void givenAdmin_whenAcceptSpaceRequest_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(randomUUID().toString());

        given(userRequestService.acceptUserRequest(any(), any(), anyLong())).willReturn(spaceRequest);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);
        assumeAuthToken();

        assertEquals(service.acceptSpaceRequest(authModel, orga.getId(), spc.getId(), spaceRequest.getId()), spaceRequest);
    }

    @Test
    void givenNoAdmin_whenAcceptSpaceRequest_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ACCESS).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(randomUUID().toString());

        given(userRequestService.acceptUserRequest(any(), any(), anyLong())).willReturn(spaceRequest);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);
        assumeAuthToken();

        assertThrows(OrganizationmanagerException.class, () -> service.acceptSpaceRequest(authModel, orga.getId(), spc.getId(), spaceRequest.getId()));
    }

    @Test
    void givenSuperuser_whenDeclineSpaceRequest_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(randomUUID().toString());

        given(userRequestService.declineUserRequest(any(), any(), anyLong())).willReturn(spaceRequest);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertEquals(service.declineSpaceRequest(authModel, orga.getId(), spc.getId(), spaceRequest.getId()), spaceRequest);
    }

    @Test
    void givenAdmin_whenDeclineSpaceRequest_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(randomUUID().toString());

        given(userRequestService.declineUserRequest(any(), any(), anyLong())).willReturn(spaceRequest);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertEquals(service.declineSpaceRequest(authModel, orga.getId(), spc.getId(), spaceRequest.getId()), spaceRequest);
    }

    @Test
    void givenNoAdmin_whenDeclineSpaceRequest_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ACCESS).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(randomUUID().toString());

        given(userRequestService.declineUserRequest(any(), any(), anyLong())).willReturn(spaceRequest);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertThrows(OrganizationmanagerException.class, () -> service.declineSpaceRequest(authModel, orga.getId(), spc.getId(), spaceRequest.getId()));
    }

    @Test
    void givenSuperuser_whenListUsersInSpace_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);

        SpaceUserDTO userDTO = new SpaceUserDTO();
        given(userService.getUsers(any(), any())).willReturn(Set.of(userDTO));

        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);

        assertEquals(1, service.listUsers(authModel, orga.getId(), spc.getId()).size());
    }

    @Test
    void givenAdmin_whenListUsersInSpace_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});

        SpaceUserDTO userDTO = new SpaceUserDTO();
        given(userService.getUsers(any(), any())).willReturn(Set.of(userDTO));

        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);

        assertEquals(1, service.listUsers(authModel, orga.getId(), spc.getId()).size());
    }

    @Test
    void givenNoAdmin_whenListUsersInSpace_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ACCESS).toLowerCase(Locale.getDefault()))});

        SpaceUserDTO userDTO = new SpaceUserDTO();
        given(userService.getUsers(any(), any())).willReturn(Set.of(userDTO));

        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);

        assertThrows(OrganizationmanagerException.class, () -> service.listUsers(authModel, orga.getId(), spc.getId()));
    }

    @Test
    void givenSpaces_whenGetAllSpaces_thenOk() throws Exception {
        Space space = new Space();
        space.setId(1L);
        space.setName("test");

        Space deletionSpace = new Space();
        deletionSpace.setId(2L);
        deletionSpace.setName("test-deletion");
        deletionSpace.setState(State.DELETION);

        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        given(spaceService.getSpaces(any(), anyLong(), any())).willReturn(List.of(space, deletionSpace));
        given(orgaService.getAllOrganizations(any(), any(), any())).willReturn(Collections.singletonList(orga));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga.getName()))});
        authModel.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_%s", orga.getName(), space.getName(), "user"))});

        List<String> spaces = service.getSpaceNamesWithOrganizationPrefix(authModel, GET);
        assertThat(spaces, hasSize(2));
    }

}
