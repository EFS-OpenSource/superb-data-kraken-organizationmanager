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
package com.efs.sdk.organizationmanager.core.organization;

import com.efs.sdk.organizationmanager.commons.OrganizationmanagerException;
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.helper.AuthConfiguration;
import com.efs.sdk.organizationmanager.helper.AuthEntityOrganization;
import com.efs.sdk.organizationmanager.helper.AuthenticationModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.efs.sdk.common.domain.model.Confidentiality.INTERNAL;
import static com.efs.sdk.common.domain.model.Confidentiality.PUBLIC;
import static com.efs.sdk.organizationmanager.utils.TestUtils.assumeAuthToken;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

class OrganizationServiceTest {

    @MockBean
    private OrganizationRepository repo;
    private OrganizationService service;

    @BeforeEach
    public void setup() {
        this.repo = Mockito.mock(OrganizationRepository.class);
        this.service = new OrganizationService(repo);
    }

    @Test
    void givenId_whenCreateOrganization_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);
        assertThrows(OrganizationmanagerException.class, () -> service.createOrganizationEntity(orga));
    }

    @Test
    void givenNoName_whenCreateOrganization_thenError() {
        Organization orga = new Organization();
        assertThrows(OrganizationmanagerException.class, () -> service.createOrganizationEntity(orga));
    }

    @Test
    void givenEmptyName_whenCreateOrganization_thenError() {
        Organization orga = new Organization();
        orga.setName("");
        assertThrows(OrganizationmanagerException.class, () -> service.createOrganizationEntity(orga));
    }

    @Test
    void givenInvalidName_whenCreateOrganization_thenError() {
        Organization orga = new Organization();
        orga.setName("a_1");
        assertThrows(OrganizationmanagerException.class, () -> service.createOrganizationEntity(orga));
    }

    @Test
    void givenNameExistOther_whenNameValidation_thenError() {
        Organization orga = new Organization();
        orga.setName("test");
        orga.setId(1L);

        Optional<Organization> orgaOpt = Optional.of(orga);
        given(repo.findByName(anyString())).willReturn(orgaOpt);

        Organization other = new Organization();
        other.setName("test");
        other.setId(2L);
        assertThrows(OrganizationmanagerException.class, () -> service.nameValidation(other));
    }

    @Test
    void givenNameExists_whenNameValidation_thenOk() {
        Organization orga = new Organization();
        orga.setName("test");
        orga.setId(1L);

        Optional<Organization> orgaOpt = Optional.of(orga);
        given(repo.findByName(anyString())).willReturn(orgaOpt);

        assertDoesNotThrow(() -> service.nameValidation(orga));
    }

    @Test
    void givenNewOrganization_whenCreateOrganization_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setName("test");
        orga.setConfidentiality(INTERNAL);

        Organization persisted = new Organization();
        persisted.setName("test");
        persisted.setConfidentiality(INTERNAL);
        persisted.setId(1L);

        given(repo.findByName(anyString())).willReturn(Optional.empty());

        given(repo.saveAndFlush(any())).willReturn(persisted);

        Organization actual = service.createOrganizationEntity(orga);

        assertNotNull(actual);
        assertEquals(orga.getName(), actual.getName());
        assertEquals(orga.getConfidentiality(), actual.getConfidentiality());
    }

    @Test
    void givenPublic_whenGetAllOrganizations_thenOk() {
        Organization orga = new Organization();
        orga.setName("test");
        orga.setConfidentiality(PUBLIC);

        Organization orga2 = new Organization();
        orga2.setName("test2");
        orga2.setConfidentiality(PUBLIC);

        List<Organization> expected = List.of(orga, orga2);
        given(repo.findByConfidentiality(any())).willReturn(expected);
        given(repo.findByNameIn(any())).willReturn(Collections.emptyList());

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrgaPublicAccess(true);

        List<Organization> actual = service.getAllOrganizations(authModel, new String[0], AuthConfiguration.GET);
        assertThat(actual, hasSize(expected.size()));
    }

    @Test
    void givenAccess_whenGetAllOrganizations_thenOk() {
        Organization orga = new Organization();
        orga.setName("test");

        Organization orga2 = new Organization();
        orga2.setName("test2");

        List<Organization> expected = List.of(orga, orga2);
        given(repo.findByConfidentiality(any())).willReturn(expected);
        given(repo.findByNameIn(any())).willReturn(List.of(orga, orga2));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga.getName())),
                new AuthEntityOrganization(format("org_%s_access", orga2.getName()))});
        assumeAuthToken();

        List<Organization> actual = service.getAllOrganizations(authModel, null, AuthConfiguration.GET);
        assertThat(actual, hasSize(expected.size()));
    }

    @Test
    void givenExistingOrganization_whenGetOrganization_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setName("test");
        orga.setConfidentiality(PUBLIC);
        orga.setId(1L);

        given(repo.findById(anyLong())).willReturn(Optional.of(orga));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga.getName()))});

        assertNotNull(service.getOrganization(1L, authModel));
    }

    @Test
    void givenNonExistingOrganization_whenGetOrganization_thenError() {
        given(repo.findById(anyLong())).willReturn(Optional.empty());
        assertThrows(OrganizationmanagerException.class, () -> service.getOrganization(1L, new AuthenticationModel()));
    }

    @Test
    void givenNoName_whenUpdateOrganization_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);
        assertThrows(OrganizationmanagerException.class, () -> service.updateOrganizationEntity(orga, new AuthenticationModel()));
    }

    @Test
    void givenNewName_whenUpdateOrganization_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Organization other = new Organization();
        other.setName("new-test");
        other.setId(1L);

        given(repo.findById(anyLong())).willReturn(Optional.of(orga));
        given(repo.saveAndFlush(any())).willReturn(other);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_admin", orga.getName()))});
        Organization actual = service.updateOrganizationEntity(orga, authModel);
        assertNotNull(actual);
        assertEquals(other.getName(), actual.getName());
        assertEquals(other.getCreated(), actual.getCreated());
    }

    @Test
    void givenAccess_whenUpdateOrganization_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Organization other = new Organization();
        other.setName("new-test");
        other.setId(1L);

        given(repo.findById(anyLong())).willReturn(Optional.of(orga));
        given(repo.saveAndFlush(any())).willReturn(other);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga.getName()))});
        assertThrows(OrganizationmanagerException.class, () -> service.updateOrganizationEntity(orga, authModel));
    }

    @Test
    void givenNoAdminRole_whenUpdateOrganization_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        given(repo.findById(anyLong())).willReturn(Optional.of(orga));

        assertThrows(OrganizationmanagerException.class, () -> service.updateOrganizationEntity(orga, new AuthenticationModel()));
    }

    @Test
    void givenPublicAccess_whenUpdateOrganization_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");
        orga.setConfidentiality(PUBLIC);

        given(repo.findById(anyLong())).willReturn(Optional.of(orga));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrgaPublicAccess(true);

        assertThrows(OrganizationmanagerException.class, () -> service.updateOrganizationEntity(orga, authModel));
    }

    @Test
    void givenNoAdminRolePublic_whenUpdateOrganization_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");
        orga.setConfidentiality(PUBLIC);

        given(repo.findById(anyLong())).willReturn(Optional.of(orga));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrgaPublicAccess(true);

        assertThrows(OrganizationmanagerException.class, () -> service.updateOrganizationEntity(orga, authModel));
    }

    @Test
    void givenPublicRole_whenUpdateOrganization_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        given(repo.findById(anyLong())).willReturn(Optional.of(orga));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrgaPublicAccess(true);

        assertThrows(OrganizationmanagerException.class, () -> service.updateOrganizationEntity(orga, authModel));
    }

    @Test
    void givenAdminRole_whenUpdateOrganization_thenOk() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        given(repo.findById(anyLong())).willReturn(Optional.of(orga));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_admin", orga.getName()))});

        assertDoesNotThrow(() -> service.updateOrganizationEntity(orga, authModel));
    }

    @Test
    void givenInternalAndNoAdminRole_whenGetOrganization_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        given(repo.findById(anyLong())).willReturn(Optional.of(orga));

        assertThrows(OrganizationmanagerException.class, () -> service.getOrganization(orga.getId(), new AuthenticationModel()));
    }

    @Test
    void givenPublicAndNoAdminRole_whenGetOrganization_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        given(repo.findById(anyLong())).willReturn(Optional.of(orga));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga.getName()))});
        Organization actual = service.getOrganization(orga.getId(), authModel);

        assertNotNull(actual);
    }

    @Test
    void givenSuperuser_whenUpdateOrganization_thenOk() {
        Organization persisted = new Organization();
        persisted.setId(1L);
        persisted.setName("test");
        persisted.setDescription("old description");

        Organization item = new Organization();
        item.setId(persisted.getId());
        item.setName(persisted.getName());
        item.setDescription("new description");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);

        given(repo.findById(anyLong())).willReturn(Optional.of(persisted));

        assertDoesNotThrow(() -> service.updateOrganizationEntity(item, authModel));
    }

    @Test
    void givenRename_whenUpdateOrganization_thenError() {
        Organization persisted = new Organization();
        persisted.setId(1L);
        persisted.setName("test");

        Organization item = new Organization();
        item.setId(1L);
        item.setName("renametest");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);

        given(repo.findById(anyLong())).willReturn(Optional.of(persisted));

        assertThrows(OrganizationmanagerException.class, () -> service.updateOrganizationEntity(item, authModel));
    }

    @Test
    void givenSuperuser_whenGetAllOrganizations_thenOk() {
        Organization orga1 = new Organization();
        orga1.setId(1L);
        orga1.setName("test");

        Organization orga2 = new Organization();
        orga2.setId(2L);
        orga2.setName("other-test");
        orga2.setConfidentiality(PUBLIC);

        List<Organization> expected = List.of(orga1, orga2);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);

        given(repo.findAll()).willReturn(expected);

        List<Organization> actual = service.getAllOrganizations(authModel, new String[0], AuthConfiguration.GET);
        assertThat(actual, hasSize(expected.size()));
    }

    @Test
    void givenSuperuser_whenGetOrganization_thenOk() {
        Organization item = new Organization();
        item.setId(1L);
        item.setName("rename-test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);

        given(repo.findById(anyLong())).willReturn(Optional.of(item));

        assertDoesNotThrow(() -> service.getOrganization(item.getId(), authModel));
    }


    @Test
    void givenExistingOrganization_whenGetOrganizationByName_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setName("test");
        orga.setConfidentiality(PUBLIC);
        orga.setId(1L);

        given(repo.findByName(anyString())).willReturn(Optional.of(orga));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga.getName()))});

        assertNotNull(service.getOrganizationByName(orga.getName(), authModel));
    }

    @Test
    void givenNonExistingOrganization_whenGetOrganizationByName_thenError() {
        given(repo.findByName(anyString())).willReturn(Optional.empty());
        assertThrows(OrganizationmanagerException.class, () -> service.getOrganizationByName("test", new AuthenticationModel()));
    }

    @Test
    void givenInternalAndNoAdminRole_whenGetOrganizationByName_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        given(repo.findByName(anyString())).willReturn(Optional.of(orga));

        assertThrows(OrganizationmanagerException.class, () -> service.getOrganizationByName(orga.getName(), new AuthenticationModel()));
    }

    @Test
    void givenPublicAndNoAdminRole_whenGetOrganizationByName_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        given(repo.findByName(anyString())).willReturn(Optional.of(orga));

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga.getName()))});
        Organization actual = service.getOrganizationByName(orga.getName(), authModel);

        assertNotNull(actual);
    }

    @Test
    void givenSuperuser_whenGetOrganizationByName_thenOk() {
        Organization item = new Organization();
        item.setId(1L);
        item.setName("rename-test");

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setSuperuser(true);

        given(repo.findByName(anyString())).willReturn(Optional.of(item));

        assertDoesNotThrow(() -> service.getOrganizationByName(item.getName(), authModel));
    }

    @Test
    void givenAllowedOrganizations_whenGetOrganizations_thenOk() {
        Organization orga1 = new Organization();
        orga1.setName("test1");

        Organization orga2 = new Organization();
        orga2.setName("test2");

        List<Organization> orgas = List.of(orga1, orga2);
        List<String> orgaNames = orgas.stream().map(Organization::getName).toList();

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(orgaNames.stream().map(orga -> new AuthEntityOrganization(format("org_%s_access", orga))).toArray(AuthEntityOrganization[]::new));
        assumeAuthToken();

        given(repo.findByNameIn(any())).willReturn(orgas);
        List<Organization> actualOrgas = service.getAllOrganizations(authModel, orgaNames.toArray(String[]::new), AuthConfiguration.GET);
        assertThat(actualOrgas, hasSize(orgas.size()));
        assertTrue(actualOrgas.contains(orga1));
        assertTrue(actualOrgas.contains(orga2));
    }
}
