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
package com.efs.sdk.organizationmanager.core.space;

import com.efs.sdk.common.domain.model.State;
import com.efs.sdk.organizationmanager.commons.OrganizationmanagerException;
import com.efs.sdk.organizationmanager.core.organization.OrganizationService;
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.core.space.model.Space;
import com.efs.sdk.organizationmanager.helper.AuthEntityOrganization;
import com.efs.sdk.organizationmanager.helper.AuthEntitySpace;
import com.efs.sdk.organizationmanager.helper.AuthenticationModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.beans.PropertyChangeEvent;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.efs.sdk.common.domain.model.Confidentiality.PUBLIC;
import static com.efs.sdk.organizationmanager.helper.AuthConfiguration.GET;
import static com.efs.sdk.organizationmanager.helper.AuthConfiguration.READ;
import static com.efs.sdk.organizationmanager.helper.AuthEntityOrganization.ACCESS_ROLE;
import static com.efs.sdk.organizationmanager.helper.AuthEntityOrganization.ADMIN_ROLE;
import static com.efs.sdk.organizationmanager.utils.TestUtils.*;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;

class SpaceServiceTest {

    @MockBean
    private OrganizationService orgaService;
    @MockBean
    private SpaceRepository repo;
    private SpaceService service;

    @BeforeEach
    public void setup() {
        this.repo = Mockito.mock(SpaceRepository.class);
        this.orgaService = Mockito.mock(OrganizationService.class);
        this.service = new SpaceService(repo, orgaService);
    }

    @Test
    void givenId_whenCreateSpace_thenError() {
        Organization orga = new Organization();
        orga.setName("orga");
        orga.setId(1L);

        Space space = new Space();
        space.setId(1L);

        assertThrows(OrganizationmanagerException.class, () -> service.createSpaceEntity(orga, space));
    }

    @Test
    void givenValid_whenCreateSpace_thenOk() throws Exception {

        Organization orga = new Organization();
        orga.setName("orga");
        orga.setId(1L);

        Space space = new Space();
        space.setName("test");

        Space persisted = new Space();
        persisted.setName("test");
        persisted.setId(1L);
        persisted.setCreated(ZonedDateTime.now());
        persisted.setOrganizationId(orga.getId());

        given(repo.saveAndFlush(any())).willReturn(persisted);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga.getName()))});

        Space actual = service.createSpaceEntity(orga, space);
        assertNotNull(actual);
        assertTrue(actual.getId() > -1L);
        assertNotNull(actual.getCreated());
        assertNotNull(actual.getOrganizationId());
    }

    @Test
    void givenExistingSpace_whenGetSpace_thenOk() throws Exception {

        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space space = new Space();
        space.setName("test");
        space.setId(1L);
        space.setOrganizationId(orga.getId());

        given(repo.findByOrganizationIdAndId(anyLong(), anyLong())).willReturn(Optional.of(space));
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga.getName()))});
        authModel.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_%s", orga.getName(), space.getName(), "user"))});

        Space actual = service.getSpaceById(authModel, orga.getId(), space.getId());
        assertNotNull(actual);
        assertEquals(space.getId(), actual.getId());
        assertEquals(space.getName(), actual.getName());
        assertNotNull(actual.getConfidentiality());
    }

    @Test
    void givenNonExistingSpace_whenGetSpace_thenError() {
        given(repo.findByOrganizationIdAndId(anyLong(), anyLong())).willReturn(Optional.empty());
        assertThrows(OrganizationmanagerException.class, () -> service.getSpaceById(new AuthenticationModel(), 1L, 1L));
    }

    @Test
    void givenNoAccess_whenGetSpace_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space space = new Space();
        space.setName("test");
        space.setId(1L);
        space.setOrganizationId(orga.getId());

        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(repo.findByOrganizationIdAndId(anyLong(), anyLong())).willReturn(Optional.of(space));

        assertThrows(OrganizationmanagerException.class, () -> service.getSpaceById(new AuthenticationModel(), orga.getId(), space.getId()));
    }

    @Test
    void givenDeletionSpace_whenGetSpace_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space space = new Space();
        space.setName("test");
        space.setId(1L);
        space.setOrganizationId(orga.getId());
        space.setState(State.DELETION);

        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(repo.findByOrganizationIdAndId(anyLong(), anyLong())).willReturn(Optional.of(space));

        assertThrows(OrganizationmanagerException.class, () -> service.getSpaceById(new AuthenticationModel(), orga.getId(), space.getId()));
    }


    @Test
    void givenPublicAccess_whenGetSpace_thenOk() throws Exception {
        Space space = new Space();
        space.setName("test");
        space.setId(1L);
        space.setConfidentiality(PUBLIC);

        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(repo.findByOrganizationIdAndId(anyLong(), anyLong())).willReturn(Optional.of(space));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSpacePublicAccess(true);

        assertDoesNotThrow(() -> service.getSpaceById(authModel, orga.getId(), space.getId()));
    }

    @Test
    void givenNoName_whenNameValidation_thenError() {
        long orgaId = 1L;
        Space space = new Space();
        assertThrows(OrganizationmanagerException.class, () -> service.nameValidation(space, orgaId));
    }

    @Test
    void givenEmptyName_whenNameValidation_thenError() {
        long orgaId = 1L;
        Space space = new Space();
        space.setName("");
        assertThrows(OrganizationmanagerException.class, () -> service.nameValidation(space, orgaId));
    }

    @Test
    void givenInvalidName_whenNameValidation_thenError() {
        long orgaId = 1L;
        Space space = new Space();
        space.setName("a_1");
        assertThrows(OrganizationmanagerException.class, () -> service.nameValidation(space, orgaId));
    }

    @Test
    void givenExisitingNameOther_whenNameValidation_thenError() {
        long orgaId = 1L;

        Space space = new Space();
        space.setName("test");
        space.setId(1L);
        space.setOrganizationId(orgaId);

        Space other = new Space();
        other.setName("test");
        other.setId(2L);
        other.setOrganizationId(orgaId);

        given(repo.findByOrganizationIdAndName(anyLong(), anyString())).willReturn(Optional.of(other));

        assertThrows(OrganizationmanagerException.class, () -> service.nameValidation(space, orgaId));
    }

    @Test
    void givenExisitingName_whenNameValidation_thenOk() {
        long orgaId = 1L;

        Space space = new Space();
        space.setName("test");
        space.setId(1L);
        space.setOrganizationId(orgaId);

        given(repo.findByOrganizationIdAndName(anyLong(), anyString())).willReturn(Optional.of(space));

        assertDoesNotThrow(() -> service.nameValidation(space, orgaId));
    }

    @Test
    void givenNoSpaceFound_whenUpdateSpace_thenError() {
        Organization orga = new Organization();
        orga.setName("orga");
        orga.setId(1L);

        Space space = new Space();
        space.setName("test");
        space.setId(1L);
        AuthenticationModel authModel = new AuthenticationModel();

        given(repo.findByOrganizationIdAndId(anyLong(), anyLong())).willReturn(Optional.empty());
        assertThrows(OrganizationmanagerException.class, () -> service.updateSpaceEntity(authModel, orga, space));
    }

    @Test
    void givenOrgaAdmin_whenUpdateSpace_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setName("orga");
        orga.setId(1L);

        Space space = new Space();
        space.setName("test");
        space.setId(1L);

        given(repo.findByOrganizationIdAndId(anyLong(), anyLong())).willReturn(Optional.of(space));
        given(repo.saveAndFlush(any())).willReturn(space);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_admin", orga.getName()))});

        Space actual = service.updateSpaceEntity(authModel, orga, space);
        assertNotNull(actual);
        assertEquals(space.getId(), actual.getId());
        assertEquals(space.getName(), actual.getName());
    }

    @Test
    void givenNoOrgaAdmin_whenUpdateSpace_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setName("orga");
        orga.setId(1L);

        Space space = new Space();
        space.setName("test");
        space.setId(1L);

        given(repo.findByOrganizationIdAndId(anyLong(), anyLong())).willReturn(Optional.of(space));
        given(repo.saveAndFlush(any())).willReturn(space);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga.getName()))});

        assertThrows(OrganizationmanagerException.class, () -> service.updateSpaceEntity(authModel, orga, space));
    }

    @Test
    void givenSpaceOwner_whenUpdateSpace_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setName("orga");
        orga.setId(1L);

        Space space = new Space();
        space.setName("test");
        space.setId(1L);
        space.setOwners(List.of(MY_USERNAME));

        given(repo.findByOrganizationIdAndId(anyLong(), anyLong())).willReturn(Optional.of(space));
        given(repo.saveAndFlush(any())).willReturn(space);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        assumeAuthToken();

        AuthenticationModel authModel = new AuthenticationModel();

        Space actual = service.updateSpaceEntity(authModel, orga, space);
        assertNotNull(actual);
        assertEquals(space.getId(), actual.getId());
        assertEquals(space.getName(), actual.getName());
    }

    @Test
    void givenSpaces_whenGetSpaces_thenOk() throws Exception {
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

        given(repo.findByOrganizationIdAndNameIn(anyLong(), any())).willReturn(List.of(space, deletionSpace));
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga.getName()))});
        authModel.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_%s", orga.getName(), space.getName(), "user"))});

        List<Space> spaces = service.getSpaces(authModel, orga.getId(), READ);
        assertThat(spaces, hasSize(1));
    }

    @Test
    void givenPublicAccess_whenGetSpaces_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);

        Space space = new Space();
        space.setName("test");

        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(repo.findByOrganizationIdAndNameIn(anyLong(), any())).willReturn(Collections.singletonList(space));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSpacePublicAccess(true);

        assertDoesNotThrow(() -> service.getSpaces(authModel, orga.getId(), READ));
    }

    @Test
    void givenAccess_whenGetSpaces_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space space = new Space();
        space.setName("test");

        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(repo.findByOrganizationIdAndNameIn(anyLong(), any())).willReturn(Collections.singletonList(space));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga.getName()))});
        authModel.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_%s", orga.getName(), space.getName(), "user"))});

        assertDoesNotThrow(() -> service.getSpaces(authModel, orga.getId(), READ));
    }

    @Test
    void givenSupplier_whenGetSpaces_thenContainsLoadingzone() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space space = new Space();
        space.setName("test");

        Space lz = new Space();
        space.setName("loadingzone");

        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(repo.findByOrganizationIdAndNameIn(anyLong(), any())).willReturn(List.of(space, lz));

        AuthenticationModel authModel = new AuthenticationModel();
        // user has rights to upload to test-space -> will also get space loadingzone
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga.getName()))});
        authModel.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_%s", orga.getName(), space.getName(), "user"))});

        List<Space> spaces = service.getSpaces(authModel, orga.getId(), READ);
        assertThat(spaces, hasSize(2));
    }

    @Test
    void givenNoAccess_whenCreateSpace_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space space = new Space();
        space.setId(1L);
        space.setName("test");

        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        assertThrows(OrganizationmanagerException.class, () -> service.createSpaceEntity(orga, space));
    }

    @Test
    void givenSpaceExistsInOrganization_whenUpdateSpace_thenError() {
        given(repo.findByOrganizationIdAndId(anyLong(), anyLong())).willReturn(Optional.empty());

        Organization orga = new Organization();
        orga.setName("orga");
        orga.setId(1L);

        Space space = new Space();

        AuthenticationModel authModel = new AuthenticationModel();
        assertThrows(OrganizationmanagerException.class, () -> service.updateSpaceEntity(authModel, orga, space));
    }

    @Test
    void givenNoAccess_whenUpdateSpace_thenError() {
        Organization orga = new Organization();
        orga.setName("orga");
        orga.setId(1L);

        Space space = new Space();
        AuthenticationModel authModel = new AuthenticationModel();
        given(repo.findByOrganizationIdAndId(anyLong(), anyLong())).willReturn(Optional.of(space));

        assertThrows(OrganizationmanagerException.class, () -> service.updateSpaceEntity(authModel, orga, space));
    }

    @Test
    void givenSuperuser_whenCreateSpace_thenOk() {
        Organization orga = new Organization();
        orga.setName("orga");
        orga.setId(1L);

        Space space = new Space();
        space.setName("test");

        assertDoesNotThrow(() -> service.createSpaceEntity(orga, space));
    }

    @Test
    void givenRename_whenUpdateSpace_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setName("orga");
        orga.setId(1L);

        Space space = new Space();
        space.setName("test");
        space.setId(1L);

        Space newSpace = new Space();
        newSpace.setId(space.getId());
        newSpace.setName("rename-test");

        given(repo.findByOrganizationIdAndId(anyLong(), anyLong())).willReturn(Optional.of(newSpace));
        given(repo.saveAndFlush(any())).willReturn(newSpace);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);

        assertThrows(OrganizationmanagerException.class, () -> service.updateSpaceEntity(authModel, orga, space));
    }

    @Test
    void givenSuperuser_whenUpdateSpace_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setName("orga");
        orga.setId(1L);

        Space space = new Space();
        space.setName("test");
        space.setId(1L);

        given(repo.findByOrganizationIdAndId(anyLong(), anyLong())).willReturn(Optional.of(space));
        given(repo.saveAndFlush(any())).willReturn(space);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);

        assertDoesNotThrow(() -> service.updateSpaceEntity(authModel, orga, space));
    }

    @Test
    void givenSuperuser_whenGetSpacesRead_thenFalse() {
        Space space1 = new Space();
        space1.setId(1L);
        space1.setName("test");

        Space space2 = new Space();
        space2.setId(2L);
        space2.setName("other-test");

        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        List<Space> expected = List.of(space1, space2);
        given(repo.findByOrganizationId(anyLong())).willReturn(expected);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);

        assertThrows(OrganizationmanagerException.class, () -> service.getSpaces(authModel, orga.getId(), READ));
    }

    @Test
    void givenSuperuser_whenGetSpacesGet_thenOk() throws Exception {
        Space space1 = new Space();
        space1.setId(1L);
        space1.setName("test");

        Space space2 = new Space();
        space2.setId(2L);
        space2.setName("other-test");

        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        List<Space> expected = List.of(space1, space2);
        given(repo.findByOrganizationId(anyLong())).willReturn(expected);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);

        List<Space> spaces = service.getSpaces(authModel, orga.getId(), GET);
        assertThat(spaces, hasSize(expected.size()));
    }

    @Test
    void givenAdmin_whenGetSpacesGet_thenOk() throws Exception {
        Space space1 = new Space();
        space1.setId(1L);
        space1.setName("test");

        Space space2 = new Space();
        space2.setId(2L);
        space2.setName("other-test");

        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        List<Space> expected = List.of(space1, space2);
        given(repo.findByOrganizationId(anyLong())).willReturn(expected);
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_admin", orga.getName()))});

        List<Space> spaces = service.getSpaces(authModel, orga.getId(), GET);
        assertThat(spaces, hasSize(expected.size()));
    }

    @Test
    void givenAdmin_whenGetSpacesRead_thenEmpty() {
        Space space1 = new Space();
        space1.setId(1L);
        space1.setName("test");

        Space space2 = new Space();
        space2.setId(2L);
        space2.setName("other-test");

        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        List<Space> spaces = List.of(space1, space2);
        given(repo.findByOrganizationId(anyLong())).willReturn(spaces);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(false);
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_admin", orga.getName()))});

        assertThrows(OrganizationmanagerException.class, () -> service.getSpaces(authModel, orga.getId(), READ));
    }

    @Test
    void givenSuperuser_whenGetSpace_thenOk() throws Exception {
        long orgaId = 1L;

        Space space = new Space();
        space.setName("test");
        space.setId(1L);
        space.setOrganizationId(orgaId);

        given(repo.findByOrganizationIdAndId(anyLong(), anyLong())).willReturn(Optional.of(space));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);

        Space actual = service.getSpaceById(authModel, orgaId, space.getId());
        assertNotNull(actual);
        assertEquals(space.getId(), actual.getId());
        assertEquals(space.getName(), actual.getName());
        assertNotNull(actual.getConfidentiality());
    }

    @Test
    void givenDeleteOrganizationEvent_whenPropertyChange_thenDeleteSpaces() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");
        PropertyChangeEvent event = new PropertyChangeEvent(orgaService, OrganizationService.PROP_ORG_DELETED, orga, null);
        Space space = new Space();
        space.setName("test");
        space.setId(1L);
        given(repo.findByOrganizationId(anyLong())).willReturn(List.of(space));
        willDoNothing().given(repo).delete(any());
        assertDoesNotThrow(() -> service.propertyChange(event));
    }

    @Test
    void givenExistingSpace_whenGetSpaceByName_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space space = new Space();
        space.setName("test");
        space.setId(1L);
        space.setOrganizationId(orga.getId());

        given(repo.findByOrganizationIdAndName(anyLong(), anyString())).willReturn(Optional.of(space));
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga.getName()))});
        authModel.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_%s", orga.getName(), space.getName(), "user"))});

        Space actual = service.getSpaceByName(authModel, orga.getId(), space.getName());
        assertNotNull(actual);
        assertEquals(space.getId(), actual.getId());
        assertEquals(space.getName(), actual.getName());
        assertNotNull(actual.getConfidentiality());
    }

    @Test
    void givenNonExistingSpace_whenGetSpaceByName_thenError() {
        given(repo.findByOrganizationIdAndName(anyLong(), anyString())).willReturn(Optional.empty());
        assertThrows(OrganizationmanagerException.class, () -> service.getSpaceByName(new AuthenticationModel(), 1L, "test"));
    }

    @Test
    void givenNoAccess_whenGetSpaceByName_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space space = new Space();
        space.setName("test");
        space.setId(1L);
        space.setOrganizationId(orga.getId());

        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(repo.findByOrganizationIdAndName(anyLong(), anyString())).willReturn(Optional.of(space));

        assertThrows(OrganizationmanagerException.class, () -> service.getSpaceByName(new AuthenticationModel(), orga.getId(), space.getName()));
    }

    @Test
    void givenPublicAccess_whenGetSpaceByName_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space space = new Space();
        space.setName("test");
        space.setId(1L);
        space.setConfidentiality(PUBLIC);
        space.setOrganizationId(orga.getId());

        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(repo.findByOrganizationIdAndName(anyLong(), anyString())).willReturn(Optional.of(space));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSpacePublicAccess(true);

        assertDoesNotThrow(() -> service.getSpaceByName(authModel, orga.getId(), space.getName()));
    }

    @Test
    void givenSuperuser_whenGetSpaceByName_thenOk() throws Exception {
        long orgaId = 1L;

        Space space = new Space();
        space.setName("test");
        space.setId(1L);
        space.setOrganizationId(orgaId);

        given(repo.findByOrganizationIdAndName(anyLong(), anyString())).willReturn(Optional.of(space));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);

        Space actual = service.getSpaceByName(authModel, orgaId, space.getName());
        assertNotNull(actual);
        assertEquals(space.getId(), actual.getId());
        assertEquals(space.getName(), actual.getName());
        assertNotNull(actual.getConfidentiality());
    }

    @Test
    void givenNoAdmin_whenMarkForDeletion_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthEntityOrganization[] authEntityOrganizations = {new AuthEntityOrganization()};
        authEntityOrganizations[0].setOrganization(orga.getName());
        authEntityOrganizations[0].setRole(ACCESS_ROLE);

        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(authEntityOrganizations);

        assertThrows(OrganizationmanagerException.class, () -> service.setDeletionState(authModel, 1L, 1L, true),
                "Expected OrganizationManager throw, but " + "not accrued.");
    }

    @Test
    void givenNoSpace_whenMarkForDeletion_thenError() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        AuthEntityOrganization[] authEntityOrganizations = {new AuthEntityOrganization()};
        authEntityOrganizations[0].setOrganization(orga.getName());
        authEntityOrganizations[0].setRole(ADMIN_ROLE);

        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(repo.findById(anyLong())).willReturn(Optional.empty());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(authEntityOrganizations);

        assertThrows(OrganizationmanagerException.class, () -> service.setDeletionState(authModel, 1L, 1L, true),
                "Expected OrganizationManager throw, but " + "not accrued.");
    }

    @Test
    void givenOrgaAuthAndSpace_whenMarkForDeletion_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setName("test");
        spc.setId(1L);
        spc.setOrganizationId(orga.getId());

        AuthEntityOrganization[] authEntityOrganizations = {new AuthEntityOrganization()};
        authEntityOrganizations[0].setOrganization(orga.getName());
        authEntityOrganizations[0].setRole(ADMIN_ROLE);

        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);
        given(repo.findById(anyLong())).willReturn(Optional.of(spc));
        given(repo.findByOrganizationIdAndId(anyLong(), anyLong())).willReturn(Optional.of(spc));

        spc.setState(State.DELETION);
        given(repo.saveAndFlush(any(Space.class))).willReturn(spc);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(authEntityOrganizations);
        authModel.setToken(new JwtAuthenticationToken(getJwt(new ArrayList<>())));

        assertEquals(State.DELETION, service.setDeletionState(authModel, spc.getId(), orga.getId(), true).getState());
        assertEquals(State.CLOSED, service.setDeletionState(authModel, spc.getId(), orga.getId(), false).getState());
    }


    @Test
    void givenSpaceAndSuperuser_whenDeleteSpace_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space spc = new Space();
        spc.setName("test");
        spc.setId(1L);
        spc.setOrganizationId(orga.getId());

        given(repo.findById(anyLong())).willReturn(Optional.of(spc));
        given(orgaService.getOrganization(anyLong(), any())).willReturn(orga);

        assertTrue(service.deleteSpaceEntity(spc));
    }
}
