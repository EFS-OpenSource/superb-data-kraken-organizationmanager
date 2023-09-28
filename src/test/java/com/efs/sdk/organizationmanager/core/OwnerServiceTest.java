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

import com.efs.sdk.organizationmanager.commons.OrganizationmanagerException;
import com.efs.sdk.organizationmanager.core.auth.OwnerService;
import com.efs.sdk.organizationmanager.core.auth.UserService;
import com.efs.sdk.organizationmanager.core.auth.model.OrganizationUserDTO;
import com.efs.sdk.organizationmanager.core.organization.OrganizationService;
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.core.space.SpaceService;
import com.efs.sdk.organizationmanager.core.space.model.Space;
import com.efs.sdk.organizationmanager.helper.AuthEntityOrganization;
import com.efs.sdk.organizationmanager.helper.AuthenticationModel;
import com.efs.sdk.organizationmanager.helper.RoleHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.efs.sdk.organizationmanager.utils.TestUtils.*;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

class OwnerServiceTest {

    @MockBean
    private OrganizationService orgaService;
    @MockBean
    private SpaceService spaceService;

    @MockBean
    private UserService userService;


    private OwnerService service;

    @BeforeEach
    public void setup() {
        this.orgaService = Mockito.mock(OrganizationService.class);
        this.spaceService = Mockito.mock(SpaceService.class);
        this.userService = Mockito.mock(UserService.class);

        this.service = new OwnerService(orgaService, spaceService, userService);
    }


    @Test
    void givenAnyUser_whenListOwnersInOrga_thenReturnOwners() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");
        List<String> owners = Collections.singletonList("ownerID");
        orga.setOwners(owners);
        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId("ownerID");
        userDTO.setFirstName("firstname");
        userDTO.setFirstName("lastname");
        given(userService.getUserView(anyString())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertEquals(owners.get(0), service.listOwners(authModel, orga.getId()).get(0).getId());
    }


    @Test
    void givenSuperuser_whenSetOwnersInOrga_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        given(userService.getUsers(any())).willReturn(Set.of(userDTO));
        given(userService.userExists(any())).willReturn(true);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        List<String> owners = Collections.singletonList("owner");

        assertEquals(owners, service.setOwners(authModel, orga.getId(), owners).getOwners());
    }


    @Test
    void givenOrgaAdmin_whenSetOwnersInOrga_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        given(userService.getUsers(any())).willReturn(Set.of(userDTO));
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(userService.userExists(any())).willReturn(true);
        List<String> owners = Collections.singletonList("owner");

        assertThrows(OrganizationmanagerException.class, () -> service.setOwners(authModel, orga.getId(), owners));
    }

    @Test
    void givenOwner_whenSetOwnersInOrga_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");
        orga.setOwners(List.of(MY_USERNAME));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        given(userService.getUsers(any())).willReturn(Set.of(userDTO));
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(userService.userExists(any())).willReturn(true);
        assumeAuthToken();
        List<String> owners = List.of(MY_USERNAME, "owner");

        assertEquals(owners, service.setOwners(authModel, orga.getId(), owners).getOwners());
    }

    @Test
    void givenNoAdmin_whenSetOwnersInOrga_thenError() throws Exception {
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
        given(userService.userExists(any())).willReturn(true);
        assertThrows(OrganizationmanagerException.class, () -> service.setOwners(authModel, orga.getId(), Collections.singletonList("owner")));
    }

    @Test
    void givenSuperuser_whenAddOwnerByNameInOrga_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId("ownerID");
        userDTO.setUsername("ownername");
        given(userService.getUserByName(any())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        List<String> owners = Collections.singletonList("ownerID");

        assertEquals(owners, service.addOwnerByName(authModel, orga.getId(), "ownername").getOwners());
    }

    @Test
    void givenOwner_whenAddOwnerByNameInOrga_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");
        orga.setOwners(List.of(MY_USERNAME));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId("ownerID");
        userDTO.setUsername("ownername");
        given(userService.getUserByName(any())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        assumeAuthToken();

        List<String> owners = List.of(MY_USERNAME, "ownerID");

        assertEquals(owners, service.addOwnerByName(authModel, orga.getId(), "ownername").getOwners());
    }

    @Test
    void givenAdmin_whenAddOwnerByNameInOrga_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId("ownerID");
        userDTO.setUsername("ownername");
        given(userService.getUserByName(any())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        List<String> owners = List.of("ownerID");

        assertThrows(OrganizationmanagerException.class, () -> service.addOwnerByName(authModel, orga.getId(), "ownername").getOwners());
    }

    @Test
    void givenNoAdmin_whenAddOwnerByNameInOrga_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ACCESS).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId("ownerID");
        userDTO.setUsername("ownername");
        given(userService.getUserByName(any())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertThrows(OrganizationmanagerException.class, () -> service.addOwnerByName(authModel, orga.getId(), "ownername"));
    }

    @Test
    void givenSuperuser_whenAddOwnerByEmailInOrga_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId("ownerID");
        userDTO.setEmail("email");
        given(userService.getUserByEmail(any())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        List<String> owners = Collections.singletonList("ownerID");

        assertEquals(owners, service.addOwnerByEmail(authModel, orga.getId(), "email").getOwners());
    }

    @Test
    void givenAdmin_whenAddOwnerByEmailInOrga_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId("ownerID");
        userDTO.setEmail("email");
        given(userService.getUserByEmail(any())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        List<String> owners = Collections.singletonList("ownerID");

        assertThrows(OrganizationmanagerException.class, () -> service.addOwnerByEmail(authModel, orga.getId(), "email"));
    }

    @Test
    void givenOwner_whenAddOwnerByEmailInOrga_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");
        orga.setOwners(List.of(MY_USERNAME));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId("ownerID");
        userDTO.setEmail("email");
        given(userService.getUserByEmail(any())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        assumeAuthToken();

        List<String> owners = List.of(MY_USERNAME, "ownerID");

        assertEquals(owners, service.addOwnerByEmail(authModel, orga.getId(), "email").getOwners());
    }

    @Test
    void givenNoAdmin_whenAddOwnerByEmailInOrga_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ACCESS).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId("ownerID");
        userDTO.setEmail("email");
        given(userService.getUserByEmail(any())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertThrows(OrganizationmanagerException.class, () -> service.addOwnerByEmail(authModel, orga.getId(), "email"));
    }


    @Test
    void givenAnyUser_whenListOwnersInSpace_thenReturnOwners() throws Exception {

        long orgId = 1L;
        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orgId);

        List<String> owners = Collections.singletonList("ownerID");
        spc.setOwners(owners);
        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId("ownerID");
        userDTO.setFirstName("firstname");
        userDTO.setFirstName("lastname");
        given(userService.getUserView(any())).willReturn(userDTO);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);

        assertEquals(owners.get(0), service.listOwners(authModel, orgId, spc.getId()).get(0).getId());
    }


    @Test
    void givenSuperuser_whenSetOwnersInSpace_thenOk() throws Exception {
        long orgId = 1L;
        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orgId);

        List<String> owners = Collections.singletonList("ownerID");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        given(userService.getUsers(any())).willReturn(Set.of(userDTO));
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);
        given(userService.userExists(any())).willReturn(true);

        assertEquals(owners, service.setOwners(authModel, orgId, spc.getId(), owners).getOwners());
    }


    @Test
    void givenAdmin_whenSetOwnersInSpace_thenError() throws Exception {
        long orgId = 1L;
        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orgId);

        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        List<String> owners = Collections.singletonList("ownerID");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        given(userService.getUsers(any())).willReturn(Set.of(userDTO));
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(userService.userExists(any())).willReturn(true);

        assertThrows(OrganizationmanagerException.class, () -> service.setOwners(authModel, orgId, spc.getId(), owners));
    }

    @Test
    void givenOwner_whenSetOwnersInSpace_thenOk() throws Exception {
        long orgId = 1L;
        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orgId);
        spc.setOwners(List.of(MY_USERNAME));

        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        List<String> owners = List.of(MY_USERNAME, "ownerID");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        given(userService.getUsers(any())).willReturn(Set.of(userDTO));
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(userService.userExists(any())).willReturn(true);
        assumeAuthToken();

        assertEquals(owners, service.setOwners(authModel, orgId, spc.getId(), owners).getOwners());
    }

    @Test
    void givenSpaceOwner_whenSetOwnersInSpace_thenOk() throws Exception {
        long orgId = 1L;
        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orgId);
        spc.setOwners(List.of(MY_USERNAME));

        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ACCESS).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        given(userService.getUsers(any())).willReturn(Set.of(userDTO));
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(userService.userExists(any())).willReturn(true);
        assumeAuthToken();

        List<String> owners = List.of(MY_USERNAME, "ownerID");

        assertEquals(owners, service.setOwners(authModel, orgId, spc.getId(), owners).getOwners());
    }

    @Test
    void givenNoAdmin_whenSetOwnersInSpace_thenError() throws Exception {
        long orgId = 1L;
        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orgId);

        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", "orga",
                RoleHelper.OrganizationScopeRole.ACCESS).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        given(userService.getUsers(any())).willReturn(Set.of(userDTO));
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(userService.userExists(any())).willReturn(true);
        assertThrows(OrganizationmanagerException.class, () -> service.setOwners(authModel, orgId, spc.getId(), Collections.singletonList("ownerID")));
    }

    @Test
    void givenSuperuser_whenAddOwnerByNameInSpace_thenOk() throws Exception {
        Long orgId = 1L;
        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orgId);

        List<String> owners = Collections.singletonList("ownerID");
        spc.setOwners(owners);

        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId("ownerID");
        userDTO.setUsername("ownername");
        given(userService.getUserByName(any())).willReturn(userDTO);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertEquals(owners, service.addOwnerByName(authModel, spc.getOrganizationId(), spc.getId(), "ownername").getOwners());
    }

    @Test
    void givenAdmin_whenAddOwnerByNameInSpace_thenError() throws Exception {
        Long orgId = 1L;
        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orgId);

        List<String> owners = Collections.singletonList("ownerID");
        spc.setOwners(owners);

        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId("ownerID");
        userDTO.setUsername("ownername");
        given(userService.getUserByName(any())).willReturn(userDTO);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertThrows(OrganizationmanagerException.class, () -> service.addOwnerByName(authModel, spc.getOrganizationId(), spc.getId(), "ownername"));
    }

    @Test
    void givenAdmin_whenAddOwnerByNameInSpace_thenOk() throws Exception {
        Long orgId = 1L;
        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orgId);
        spc.setOwners(List.of(MY_USERNAME));

        List<String> owners = List.of(MY_USERNAME, "ownerID");
        spc.setOwners(owners);

        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId("ownerID");
        userDTO.setUsername("ownername");
        given(userService.getUserByName(any())).willReturn(userDTO);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        assumeAuthToken();

        assertEquals(owners, service.addOwnerByName(authModel, spc.getOrganizationId(), spc.getId(), "ownername").getOwners());
    }

    @Test
    void givenNoAdmin_whenAddOwnerByNameInSpace_thenError() throws Exception {
        Long orgId = 1L;
        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orgId);

        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        List<String> owners = Collections.singletonList("ownerID");
        spc.setOwners(owners);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", "test",
                RoleHelper.OrganizationScopeRole.ACCESS).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId("ownerID");
        userDTO.setUsername("ownername");
        given(userService.getUserByName(any())).willReturn(userDTO);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        assertThrows(OrganizationmanagerException.class, () -> service.addOwnerByName(authModel, spc.getOrganizationId(), spc.getId(), "ownername"));
    }


    @Test
    void givenSuperuser_whenAddOwnerByEmailInSpace_thenOk() throws Exception {
        Long orgId = 1L;
        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orgId);

        List<String> owners = Collections.singletonList("ownerID");
        spc.setOwners(owners);

        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId("ownerID");
        userDTO.setEmail("email");
        given(userService.getUserByEmail(any())).willReturn(userDTO);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertEquals(owners, service.addOwnerByEmail(authModel, spc.getOrganizationId(), spc.getId(), "email").getOwners());
    }


    @Test
    void givenAdmin_whenAddOwnerByEmailInSpace_thenError() throws Exception {
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

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId("ownerID");
        userDTO.setEmail("email");
        given(userService.getUserByEmail(any())).willReturn(userDTO);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        List<String> owners = Collections.singletonList("ownerID");

        assertThrows(OrganizationmanagerException.class, () -> service.addOwnerByEmail(authModel, spc.getOrganizationId(), spc.getId(), "email"));
    }

    @Test
    void givenOwner_whenAddOwnerByEmailInSpace_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setId(1L);
        spc.setName("test");
        spc.setOrganizationId(orga.getId());
        spc.setOwners(List.of(MY_USERNAME));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orga.getName(),
                RoleHelper.OrganizationScopeRole.ADMIN).toLowerCase(Locale.getDefault()))});
        authModel.setToken(new JwtAuthenticationToken(getJwt(Collections.emptyList())));

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId("ownerID");
        userDTO.setEmail("email");
        given(userService.getUserByEmail(any())).willReturn(userDTO);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        assumeAuthToken();

        List<String> owners = List.of(MY_USERNAME, "ownerID");

        assertEquals(owners, service.addOwnerByEmail(authModel, spc.getOrganizationId(), spc.getId(), "email").getOwners());
    }

    @Test
    void givenNoAdmin_whenAddOwnerByEmailInSpace_thenError() throws Exception {
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

        OrganizationUserDTO userDTO = new OrganizationUserDTO();
        userDTO.setId("ownerID");
        userDTO.setEmail("email");
        given(userService.getUserByEmail(any())).willReturn(userDTO);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(spaceService.getSpaceById(any(), anyLong(), anyLong())).willReturn(spc);
        assertThrows(OrganizationmanagerException.class, () -> service.addOwnerByEmail(authModel, spc.getOrganizationId(), spc.getId(), "email"));
    }
}
