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

import com.efs.sdk.common.domain.dto.OrganizationCreateDTO;
import com.efs.sdk.common.domain.dto.OrganizationReadDTO;
import com.efs.sdk.common.domain.dto.OrganizationUpdateDTO;
import com.efs.sdk.organizationmanager.commons.OrganizationmanagerException;
import com.efs.sdk.organizationmanager.core.OrganizationManagerService;
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.helper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static com.efs.sdk.common.domain.model.Confidentiality.PUBLIC;
import static com.efs.sdk.organizationmanager.commons.OrganizationmanagerException.ORGANIZATIONMANAGER_ERROR.*;
import static com.efs.sdk.organizationmanager.core.organization.OrganizationController.ENDPOINT;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizationController.class)
@ActiveProfiles("test")
class OrganizationControllerTest {

    /* required for tests to run */
    @MockBean
    private AuthHelper authHelper;
    @MockBean
    private EntityConverter converter;
    @MockBean
    private JwtDecoder jwtDecoder;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private OrganizationManagerService orgaManagerService;
    @MockBean
    private OrganizationService service;

    @Test
    void givenAuthentication_whenGetAllOrganizations_thenOk() throws Exception {
        Organization organization = new Organization();
        organization.setName("test");
        organization.setDescription("test description");
        organization.setConfidentiality(PUBLIC);
        organization.setId(1L);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", organization.getName()))});
        authModel.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_%s", organization.getName(), "something", "trustee"))});

        List<Organization> orgas = Collections.singletonList(organization);
        given(service.getAllOrganizations(any(), any(), any())).willReturn(orgas);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        OrganizationReadDTO orgaDTO = modelMapper.map(organization, OrganizationReadDTO.class);
        given(converter.convertToDTO(any(Organization.class), eq(OrganizationReadDTO.class))).willReturn(orgaDTO);
        given(authHelper.getAuthenticationModel(any())).willReturn(authModel);

        mvc.perform(get(ENDPOINT).with(jwt())).andExpect(status().isOk());
    }

    @Test
    void givenAuthentication_whenUpdateOrganization_thenOk() throws Exception {
        OrganizationUpdateDTO dto = new OrganizationUpdateDTO();
        dto.setName("test");
        dto.setDescription("test description");
        dto.setConfidentiality(PUBLIC);

        Organization organization = modelMapper.map(dto, Organization.class);
        organization.setId(1L);

        given(converter.convertToEntity(any(OrganizationUpdateDTO.class), eq(Organization.class))).willReturn(organization);
        given(orgaManagerService.updateOrganization(any(), any())).willReturn(organization);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        OrganizationReadDTO orgaDTO = modelMapper.map(organization, OrganizationReadDTO.class);
        given(converter.convertToDTO(any(Organization.class), eq(OrganizationReadDTO.class))).willReturn(orgaDTO);

        mvc.perform(put(ENDPOINT + "/1").with(jwt()).contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(dto))).andExpect(status().isOk());
    }

    @Test
    void givenDelete_whenListOrganizationsByPermission_thenOk() throws Exception {
        Organization orga1 = new Organization();
        orga1.setName("test1");

        Organization orga2 = new Organization();
        orga2.setName("test2");

        List<Organization> orgas = List.of(orga1, orga2);
        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga1.getName())),
                new AuthEntityOrganization(format("org_%s_access", orga2.getName()))});
        authModel.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_%s", orga1.getName(), "something", "trustee")),
                new AuthEntitySpace(format("%s_%s_%s", orga2.getName(), "something", "trustee"))});
        given(service.getAllOrganizations(any(), any(), any())).willReturn(orgas);
        given(authHelper.getAuthenticationModel(any())).willReturn(authModel);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");
        OrganizationReadDTO orgaDTO = new OrganizationReadDTO();
        orgaDTO.setName(orga1.getName());
        given(converter.convertToDTO(any(Organization.class), eq(OrganizationReadDTO.class))).willReturn(orgaDTO);

        mvc.perform(get(ENDPOINT).param("permissions", AuthConfiguration.DELETE.name()).with(jwt())).andExpect(status().isOk());
    }

    @Test
    void givenException_whenCreateOrganization_thenError() throws Exception {

        OrganizationCreateDTO dto = new OrganizationCreateDTO();
        dto.setName("test");
        dto.setDescription("test description");
        dto.setConfidentiality(PUBLIC);

        IllegalArgumentException except = new IllegalArgumentException("anything");
        given(orgaManagerService.createOrganization(any())).willThrow(except);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");
        given(authHelper.isSuperuser(any())).willReturn(true);

        OrganizationReadDTO orgaDTO = new OrganizationReadDTO();
        orgaDTO.setName(dto.getName());
        orgaDTO.setDescription(dto.getDescription());
        orgaDTO.setConfidentiality(dto.getConfidentiality());
        given(converter.convertToDTO(any(Organization.class), eq(OrganizationReadDTO.class))).willReturn(orgaDTO);

        mvc.perform(post(ENDPOINT).with(jwt()).contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(dto))).andExpect(status().is5xxServerError());
    }

    @Test
    void givenException_whenGetAllOrganizations_thenError() throws Exception {
        IllegalArgumentException except = new IllegalArgumentException("anything");
        given(service.getAllOrganizations(any(), any(), any())).willThrow(except);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(get(ENDPOINT).with(jwt())).andExpect(status().is5xxServerError());
    }

    @Test
    void givenException_whenGetOrganizationByName_thenError() throws Exception {
        IllegalArgumentException except = new IllegalArgumentException("anything");
        given(service.getOrganizationByName(anyString(), any())).willThrow(except);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(get(ENDPOINT + "/name/test").with(jwt())).andExpect(status().is5xxServerError());
    }

    @Test
    void givenException_whenGetOrganization_thenError() throws Exception {
        IllegalArgumentException except = new IllegalArgumentException("anything");
        given(service.getOrganization(anyLong(), any())).willThrow(except);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(get(ENDPOINT + "/1").with(jwt())).andExpect(status().is5xxServerError());
    }

    @Test
    void givenException_whenListOrganizationsByPermission_thenError() throws Exception {
        IllegalArgumentException except = new IllegalArgumentException("anything");
        given(service.getAllOrganizations(any(), any(), any())).willThrow(except);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(get(ENDPOINT).param("permissions", AuthConfiguration.READ.name()).with(jwt())).andExpect(status().is5xxServerError());
    }

    @Test
    void givenException_whenUpdateOrganization_thenError() throws Exception {
        OrganizationCreateDTO dto = new OrganizationCreateDTO();
        dto.setName("test");
        dto.setDescription("test description");
        dto.setConfidentiality(PUBLIC);

        IllegalArgumentException except = new IllegalArgumentException("anything");
        given(orgaManagerService.updateOrganization(any(), any())).willThrow(except);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(put(ENDPOINT + "/1").with(jwt()).contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(dto))).andExpect(status().is5xxServerError());
    }

    @Test
    void givenExistingId_whenGetOrganizationByName_thenOk() throws Exception {
        OrganizationReadDTO dto = new OrganizationReadDTO();
        dto.setName("test");
        dto.setDescription("test description");
        dto.setConfidentiality(PUBLIC);

        Organization organization = modelMapper.map(dto, Organization.class);
        organization.setId(1L);

        given(service.getOrganizationByName(anyString(), any())).willReturn(organization);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        OrganizationReadDTO orgaDTO = modelMapper.map(organization, OrganizationReadDTO.class);
        given(converter.convertToDTO(any(Organization.class), eq(OrganizationReadDTO.class))).willReturn(orgaDTO);

        mvc.perform(get(ENDPOINT + "/name/test").with(jwt())).andExpect(status().isOk());
    }

    @Test
    void givenExistingId_whenGetOrganization_thenOk() throws Exception {
        OrganizationReadDTO dto = new OrganizationReadDTO();
        dto.setName("test");
        dto.setDescription("test description");
        dto.setConfidentiality(PUBLIC);

        Organization organization = modelMapper.map(dto, Organization.class);
        organization.setId(1L);

        given(service.getOrganization(anyLong(), any())).willReturn(organization);
        given(converter.convertToDTO(any(Organization.class), eq(OrganizationReadDTO.class))).willReturn(dto);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(get(ENDPOINT + "/1").with(jwt())).andExpect(status().isOk());
    }

    @Test
    void givenNoAuthentication_whenCreateOrganization_thenError() throws Exception {
        mvc.perform(post(ENDPOINT)).andExpect(status().is4xxClientError());
    }

    @Test
    void givenNoAuthentication_whenGetAllOrganizations_thenOk() throws Exception {
        mvc.perform(get(ENDPOINT)).andExpect(status().is4xxClientError());
    }

    @Test
    void givenNoAuthentication_whenGetOrganizationByName_thenError() throws Exception {
        mvc.perform(get(ENDPOINT + "/name/test")).andExpect(status().is4xxClientError());
    }

    @Test
    void givenNoAuthentication_whenGetOrganization_thenError() throws Exception {
        mvc.perform(get(ENDPOINT + "/1")).andExpect(status().is4xxClientError());
    }

    @Test
    void givenNoAuthentication_whenListOrganizationsByPermission_thenError() throws Exception {
        mvc.perform(get(ENDPOINT).param("permissions", AuthConfiguration.READ.name())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenNoAuthentication_whenUpdateOrganization_thenError() throws Exception {
        mvc.perform(put(ENDPOINT)).andExpect(status().is4xxClientError());
    }

    @Test
    void givenNoSuperuser_whenCreateOrganization_thenError() throws Exception {
        OrganizationReadDTO dto = new OrganizationReadDTO();
        dto.setName("test");
        dto.setDescription("test description");
        dto.setConfidentiality(PUBLIC);

        Organization organization = modelMapper.map(dto, Organization.class);
        organization.setId(1L);

        given(orgaManagerService.createOrganization(any())).willReturn(organization);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(post(ENDPOINT)).andExpect(status().is4xxClientError());
    }

    @Test
    void givenOrganizationmanagerExceptionByName_whenGetOrganization_thenError() throws Exception {
        OrganizationmanagerException except = new OrganizationmanagerException(SAVE_ORGANIZATION_NAME_FOUND);
        given(service.getOrganizationByName(anyString(), any())).willThrow(except);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(get(ENDPOINT + "/name/test").with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenOrganizationmanagerException_whenCreateOrganization_thenError() throws Exception {

        OrganizationReadDTO dto = new OrganizationReadDTO();
        dto.setName("test");
        dto.setDescription("test description");
        dto.setConfidentiality(PUBLIC);

        Organization organization = modelMapper.map(dto, Organization.class);

        OrganizationmanagerException except = new OrganizationmanagerException(SAVE_PROVIDE_ID);
        given(orgaManagerService.createOrganization(any())).willThrow(except);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");
        given(converter.convertToEntity(any(OrganizationReadDTO.class), eq(Organization.class))).willReturn(organization);

        OrganizationReadDTO orgaDTO = new OrganizationReadDTO();
        orgaDTO.setId(organization.getId());
        orgaDTO.setName(organization.getName());
        orgaDTO.setDescription(organization.getDescription());
        orgaDTO.setConfidentiality(organization.getConfidentiality());
        given(converter.convertToDTO(any(Organization.class), eq(OrganizationReadDTO.class))).willReturn(orgaDTO);

        mvc.perform(post(ENDPOINT).with(jwt()).contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(dto))).andExpect(status().is4xxClientError());
    }

    @Test
    void givenOrganizationmanagerException_whenGetOrganization_thenError() throws Exception {
        OrganizationmanagerException except = new OrganizationmanagerException(SAVE_ORGANIZATION_NAME_FOUND);
        given(service.getOrganization(anyLong(), any())).willThrow(except);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(get(ENDPOINT + "/1").with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenOrganizationmanagerException_whenUpdateOrganization_thenError() throws Exception {
        OrganizationReadDTO dto = new OrganizationReadDTO();
        dto.setName("test");
        dto.setDescription("test description");
        dto.setConfidentiality(PUBLIC);

        Organization organization = modelMapper.map(dto, Organization.class);
        organization.setId(1L);

        given(converter.convertToEntity(any(OrganizationUpdateDTO.class), eq(Organization.class))).willReturn(organization);
        OrganizationmanagerException except = new OrganizationmanagerException(SAVE_REQUIRED_INFO_MISSING);
        given(orgaManagerService.updateOrganization(any(), any())).willThrow(except);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(put(ENDPOINT + "/1").with(jwt()).contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(dto))).andExpect(status().is4xxClientError());
    }

    @Test
    void givenRead_whenListOrganizationsByPermission_thenOk() throws Exception {
        Organization orga1 = new Organization();
        orga1.setName("test1");

        Organization orga2 = new Organization();
        orga2.setName("test2");

        List<Organization> orgas = List.of(orga1, orga2);
        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga1.getName())),
                new AuthEntityOrganization(format("org_%s_access", orga2.getName()))});
        authModel.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_%s", orga1.getName(), "something", "user")), new AuthEntitySpace(format(
                "%s_%s_%s", orga2.getName(), "something", "user"))});
        given(service.getAllOrganizations(any(), any(), any())).willReturn(orgas);
        given(authHelper.getAuthenticationModel(any())).willReturn(authModel);

        OrganizationReadDTO orgaDTO = modelMapper.map(orga1, OrganizationReadDTO.class);
        given(converter.convertToDTO(any(Organization.class), eq(OrganizationReadDTO.class))).willReturn(orgaDTO);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(get(ENDPOINT).param("permissions", AuthConfiguration.READ.name()).with(jwt())).andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void givenSuperuser_whenCreateOrganization_thenOk() throws Exception {
        OrganizationCreateDTO dto = new OrganizationCreateDTO();
        dto.setName("test");
        dto.setDescription("test description");
        dto.setConfidentiality(PUBLIC);

        Organization organization = modelMapper.map(dto, Organization.class);

        given(orgaManagerService.createOrganization(any())).willReturn(organization);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");
        given(converter.convertToEntity(any(OrganizationReadDTO.class), eq(Organization.class))).willReturn(organization);

        OrganizationReadDTO orgaDTO = modelMapper.map(organization, OrganizationReadDTO.class);
        given(converter.convertToDTO(any(Organization.class), eq(OrganizationReadDTO.class))).willReturn(orgaDTO);

        mvc.perform(post(ENDPOINT)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void givenUnknownPermission_whenListOrganizationsByPermission_thenError() throws Exception {
        mvc.perform(get(ENDPOINT).param("permissions", "something-that-does-not-exist").with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenWrite_whenListOrganizationsByPermission_thenOk() throws Exception {
        Organization orga1 = new Organization();
        orga1.setName("test1");

        Organization orga2 = new Organization();
        orga2.setName("test2");

        List<Organization> orgas = List.of(orga1, orga2);
        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga1.getName())),
                new AuthEntityOrganization(format("org_%s_access", orga2.getName()))});
        authModel.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_%s", orga1.getName(), "something", "supplier")),
                new AuthEntitySpace(format("%s_%s_%s", orga2.getName(), "something", "supplier"))});
        given(service.getAllOrganizations(any(), any(), any())).willReturn(orgas);
        given(authHelper.getAuthenticationModel(any())).willReturn(authModel);

        OrganizationReadDTO orgaDTO = modelMapper.map(orga1, OrganizationReadDTO.class);
        given(converter.convertToDTO(any(Organization.class), eq(OrganizationReadDTO.class))).willReturn(orgaDTO);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(get(ENDPOINT).param("permissions", AuthConfiguration.WRITE.name()).with(jwt())).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {})
    void givenNoAuthorization_whenCreateOrganization_thenReturnForbidden() throws Exception {
        OrganizationCreateDTO organization = new OrganizationCreateDTO();
        organization.setName("New Org");

        mvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(organization)))
                .andExpect(status().isForbidden());
    }
}
