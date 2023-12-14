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

import com.efs.sdk.common.domain.dto.SpaceBaseDTO;
import com.efs.sdk.common.domain.dto.SpaceCreateDTO;
import com.efs.sdk.common.domain.dto.SpaceReadDTO;
import com.efs.sdk.common.domain.dto.SpaceUpdateDTO;
import com.efs.sdk.common.domain.model.State;
import com.efs.sdk.organizationmanager.commons.OrganizationmanagerException;
import com.efs.sdk.organizationmanager.core.OrganizationManagerService;
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.core.space.model.Space;
import com.efs.sdk.organizationmanager.helper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

import static com.efs.sdk.common.domain.model.Confidentiality.PUBLIC;
import static com.efs.sdk.organizationmanager.commons.OrganizationmanagerException.ORGANIZATIONMANAGER_ERROR.*;
import static com.efs.sdk.organizationmanager.core.space.SpaceController.ENDPOINT;
import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpaceController.class)
@ActiveProfiles("test")
class SpaceControllerTest {

    /* required for tests to run */
    @MockBean
    private AuthHelper authHelper;
    /* required for tests to run */
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
    private SpaceService service;

    // create space tests
    @Test
    void givenNoAuthentication_whenCreateSpace_thenError() throws Exception {
        mvc.perform(post(ENDPOINT + "/1")).andExpect(status().is4xxClientError());
    }

    @Test
    void givenAuthentication_whenCreateSpace_thenOk() throws Exception {
        SpaceBaseDTO dto = new SpaceBaseDTO();
        dto.setName("test");
        dto.setDescription("test description");
        dto.setConfidentiality(PUBLIC);

        Space space = modelMapper.map(dto, Space.class);

        given(orgaManagerService.createSpace(any(), anyLong(), any())).willReturn(space);
        given(converter.convertToEntity(any(SpaceCreateDTO.class), eq(Space.class))).willReturn(space);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        SpaceReadDTO spaceDTO = modelMapper.map(space, SpaceReadDTO.class);
        given(converter.convertToDTO(any(Space.class), eq(SpaceReadDTO.class))).willReturn(spaceDTO);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(post(ENDPOINT + "/1").with(jwt()).contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(dto))).andExpect(status().isOk());
    }

    @Test
    void givenOrganizationmanagerException_whenCreateSpace_thenError() throws Exception {

        Space dto = new Space();
        dto.setName("test");
        dto.setDescription("test description");
        dto.setConfidentiality(PUBLIC);
        Space space = modelMapper.map(dto, Space.class);

        OrganizationmanagerException except = new OrganizationmanagerException(SAVE_PROVIDE_ID);
        given(orgaManagerService.createSpace(any(), anyLong(), any())).willThrow(except);
        given(converter.convertToEntity(any(SpaceCreateDTO.class), eq(Space.class))).willReturn(space);

        SpaceReadDTO spaceDTO = modelMapper.map(space, SpaceReadDTO.class);
        given(converter.convertToDTO(any(Space.class), eq(SpaceReadDTO.class))).willReturn(spaceDTO);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(post(ENDPOINT + "/1").with(jwt()).contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(dto))).andExpect(status().is4xxClientError());
    }

    @Test
    void givenException_whenCreateSpace_thenError() throws Exception {

        SpaceCreateDTO dto = new SpaceCreateDTO();
        dto.setName("test");
        dto.setDescription("test description");
        dto.setConfidentiality(PUBLIC);

        IllegalArgumentException except = new IllegalArgumentException("anything");
        given(orgaManagerService.createSpace(any(), anyLong(), any())).willThrow(except);

        SpaceReadDTO spaceDTO = modelMapper.map(dto, SpaceReadDTO.class);
        given(converter.convertToDTO(any(Space.class), eq(SpaceReadDTO.class))).willReturn(spaceDTO);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(post(ENDPOINT + "/1").with(jwt()).contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(dto))).andExpect(status().is5xxServerError());
    }

    // update space tests
    @Test
    void givenNoAuthentication_whenUpdateSpace_thenError() throws Exception {
        mvc.perform(put(ENDPOINT + "/1/1")).andExpect(status().is4xxClientError());
    }

    @Test
    void givenAuthentication_whenUpdateSpace_thenOk() throws Exception {
        SpaceUpdateDTO dto = new SpaceUpdateDTO();
        dto.setName("test");
        dto.setDescription("test description");
        dto.setConfidentiality(PUBLIC);

        Space space = modelMapper.map(dto, Space.class);
        space.setId(1L);

        given(converter.convertToEntity(any(SpaceUpdateDTO.class), eq(Space.class))).willReturn(space);
        given(orgaManagerService.updateSpace(any(), anyLong(), any())).willReturn(space);

        SpaceReadDTO spaceDTO = modelMapper.map(space, SpaceReadDTO.class);
        given(converter.convertToDTO(any(Space.class), eq(SpaceReadDTO.class))).willReturn(spaceDTO);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(put(ENDPOINT + "/1/1").with(jwt()).contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(dto))).andExpect(status().isOk());
    }

    @Test
    void givenOrganizationmanagerException_whenUpdateSpace_thenError() throws Exception {

        Space dto = new Space();
        dto.setName("test");
        dto.setDescription("test description");
        dto.setConfidentiality(PUBLIC);

        OrganizationmanagerException except = new OrganizationmanagerException(SAVE_PROVIDE_ID);

        Space item = modelMapper.map(dto, Space.class);
        given(converter.convertToEntity(any(SpaceUpdateDTO.class), eq(Space.class))).willReturn(item);
        given(orgaManagerService.updateSpace(any(), anyLong(), any())).willThrow(except);

        SpaceReadDTO spaceDTO = modelMapper.map(item, SpaceReadDTO.class);
        given(converter.convertToDTO(any(Space.class), eq(SpaceReadDTO.class))).willReturn(spaceDTO);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(put(ENDPOINT + "/1/1").with(jwt()).contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(dto))).andExpect(status().is4xxClientError());
    }

    @Test
    void givenException_whenUpdateSpace_thenError() throws Exception {

        SpaceUpdateDTO dto = new SpaceUpdateDTO();
        dto.setName("test");
        dto.setDescription("test description");
        dto.setConfidentiality(PUBLIC);

        IllegalArgumentException except = new IllegalArgumentException("anything");
        given(orgaManagerService.updateSpace(any(), anyLong(), any())).willThrow(except);

        SpaceReadDTO spaceDTO = modelMapper.map(dto, SpaceReadDTO.class);
        given(converter.convertToDTO(any(Space.class), eq(SpaceReadDTO.class))).willReturn(spaceDTO);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(put(ENDPOINT + "/1/1").with(jwt()).contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(dto))).andExpect(status().is5xxServerError());
    }

    // get spaces tests
    @Test
    void givenNoAuthentication_whenGetSpaces_thenError() throws Exception {
        mvc.perform(get(ENDPOINT + "/1")).andExpect(status().is4xxClientError());
    }

    @Test
    void givenOrganizationmanagerException_whenGetSpaces_thenError() throws Exception {

        OrganizationmanagerException except = new OrganizationmanagerException(SAVE_PROVIDE_ID);
        given(service.getSpaces(any(), anyLong(), any())).willThrow(except);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(get(ENDPOINT + "/1").with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenException_whenGetSpaces_thenError() throws Exception {

        IllegalArgumentException except = new IllegalArgumentException("anything");
        given(service.getSpaces(any(), anyLong(), any())).willThrow(except);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(get(ENDPOINT + "/1").with(jwt())).andExpect(status().is5xxServerError());
    }

    @Test
    void givenAuthentication_whenGetSpaces_thenOk() throws Exception {
        List<Space> spaces = List.of(new Space());
        given(service.getSpaces(any(), anyLong(), any())).willReturn(spaces);
        SpaceReadDTO spaceDTO = new SpaceReadDTO();
        given(converter.convertToDTO(any(Space.class), eq(SpaceReadDTO.class))).willReturn(spaceDTO);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");
        mvc.perform(get(ENDPOINT + "/1").with(jwt())).andExpect(status().isOk());
    }

    // get space tests
    @Test
    void givenNoAuthentication_whenGetSpace_thenError() throws Exception {
        mvc.perform(get(ENDPOINT + "/1/1")).andExpect(status().is4xxClientError());
    }

    @Test
    void givenOrganizationmanagerException_whenGetSpace_thenError() throws Exception {

        OrganizationmanagerException except = new OrganizationmanagerException(SAVE_PROVIDE_ID);
        given(service.getSpaceById(any(), anyLong(), anyLong())).willThrow(except);

        mvc.perform(get(ENDPOINT + "/1/1").with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenException_whenGetSpace_thenError() throws Exception {

        IllegalArgumentException except = new IllegalArgumentException("anything");
        given(service.getSpaceById(any(), anyLong(), anyLong())).willThrow(except);

        mvc.perform(get(ENDPOINT + "/1/1").with(jwt())).andExpect(status().is5xxServerError());
    }

    @Test
    void givenAuthentication_whenGetSpace_thenOk() throws Exception {
        given(service.getSpaceById(any(), anyLong(), anyLong())).willReturn(new Space());
        SpaceReadDTO spaceDTO = new SpaceReadDTO();
        given(converter.convertToDTO(any(Space.class), eq(SpaceReadDTO.class))).willReturn(spaceDTO);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");
        mvc.perform(get(ENDPOINT + "/1/1").with(jwt())).andExpect(status().isOk());
    }

    @Test
    void givenNoAuthentication_whenGetSpaceByName_thenError() throws Exception {
        mvc.perform(get(ENDPOINT + "/1/name/test")).andExpect(status().is4xxClientError());
    }

    @Test
    void givenOrganizationmanagerException_whenGetSpaceByName_thenError() throws Exception {

        OrganizationmanagerException except = new OrganizationmanagerException(SAVE_PROVIDE_ID);
        given(service.getSpaceByName(any(), anyLong(), anyString())).willThrow(except);

        mvc.perform(get(ENDPOINT + "/1/name/test").with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenException_whenGetSpaceByName_thenError() throws Exception {

        IllegalArgumentException except = new IllegalArgumentException("anything");
        given(service.getSpaceByName(any(), anyLong(), anyString())).willThrow(except);

        mvc.perform(get(ENDPOINT + "/1/name/test").with(jwt())).andExpect(status().is5xxServerError());
    }

    @Test
    void givenAuthentication_whenGetSpaceByName_thenOk() throws Exception {
        given(service.getSpaceByName(any(), anyLong(), anyString())).willReturn(new Space());

        SpaceReadDTO spaceDTO = new SpaceReadDTO();
        given(converter.convertToDTO(any(Space.class), eq(SpaceReadDTO.class))).willReturn(spaceDTO);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(get(ENDPOINT + "/1/name/test").with(jwt())).andExpect(status().isOk());
    }

    @Test
    void givenNoAuthentication_whenListSpacesByPermission_thenError() throws Exception {
        mvc.perform(get(ENDPOINT + "/1/permissions").param("permission", AuthConfiguration.READ.name())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenUnknownPermission_whenListSpacesByPermission_thenError() throws Exception {
        mvc.perform(get(ENDPOINT + "/1").param("permissions", "something-that-does-not-exist").with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenException_whenListSpacesByPermission_thenError() throws Exception {
        IllegalArgumentException except = new IllegalArgumentException("anything");
        given(service.getSpaces(any(), anyLong(), any())).willThrow(except);

        mvc.perform(get(ENDPOINT + "/1").param("permissions", AuthConfiguration.READ.name()).with(jwt())).andExpect(status().is5xxServerError());
    }

    @Test
    void givenRead_whenListSpacesByPermission_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setName("test");
        orga.setId(1L);

        Space spc = new Space();
        spc.setName("test-space");
        spc.setId(1L);
        spc.setOrganizationId(1L);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga.getName()))});
        authModel.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_%s", orga.getName(), spc.getName(),
                RoleHelper.SpaceScopeRole.USER.name()).toLowerCase(Locale.getDefault()))});
        given(service.getSpaces(any(), anyLong(), any())).willReturn(List.of(spc));

        SpaceReadDTO spaceDTO = modelMapper.map(spc, SpaceReadDTO.class);
        given(converter.convertToDTO(any(Space.class), eq(SpaceReadDTO.class))).willReturn(spaceDTO);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(get(ENDPOINT + "/1").param("permissions", AuthConfiguration.READ.name()).with(jwt())).andExpect(status().isOk()).andExpect(jsonPath("$",
                hasSize(1)));
    }

    @Test
    void givenWrite_whenListSpacesByPermission_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setName("test");
        orga.setId(1L);

        Space spc = new Space();
        spc.setName("test-space");
        spc.setId(1L);
        spc.setOrganizationId(1L);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga.getName()))});
        authModel.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_%s", orga.getName(), spc.getName(), "supplier"))});
        given(service.getSpaces(any(), anyLong(), any())).willReturn(List.of(spc));

        SpaceReadDTO spaceDTO = modelMapper.map(spc, SpaceReadDTO.class);
        given(converter.convertToDTO(any(Space.class), eq(SpaceReadDTO.class))).willReturn(spaceDTO);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(get(ENDPOINT + "/1").param("permissions", AuthConfiguration.WRITE.name()).with(jwt())).andExpect(status().isOk()).andExpect(jsonPath("$",
                hasSize(1)));
    }

    @Test
    void givenDelete_whenListSpacesByPermission_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setName("test");
        orga.setId(1L);

        Space spc = new Space();
        spc.setName("test-space");
        spc.setId(1L);
        spc.setOrganizationId(1L);

        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_access", orga.getName()))});
        authModel.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_%s", orga.getName(), spc.getName(), "trustee"))});
        given(service.getSpaces(any(), anyLong(), any())).willReturn(List.of(spc));

        SpaceReadDTO spaceDTO = modelMapper.map(spc, SpaceReadDTO.class);
        given(converter.convertToDTO(any(Space.class), eq(SpaceReadDTO.class))).willReturn(spaceDTO);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(get(ENDPOINT + "/1").param("permissions", AuthConfiguration.DELETE.name()).with(jwt())).andExpect(status().isOk()).andExpect(jsonPath("$"
                , hasSize(1)));
    }

    @Test
    void givenNoAuthentication_whenMarkForDeletion_thenError() throws Exception {
        mvc.perform(get(ENDPOINT + "/1/1/setDeletionState").param("willBeDeleted", "true")).andExpect(status().is4xxClientError());
    }

    @Test
    void givenOrgaException_whenMarkForDeletion_thenError() throws Exception {
        given(service.setDeletionState(any(), anyLong(), anyLong(), anyBoolean())).willThrow(new OrganizationmanagerException(GET_SINGLE_SPACE_NOT_FOUND));

        mvc.perform(put(ENDPOINT + "/1/1/setDeletionState").with(jwt()).param("willBeDeleted", "true")).andExpect(status().is4xxClientError());
    }

    @Test
    void givenException_whenMarkForDeletion_thenError() throws Exception {
        given(service.setDeletionState(any(), anyLong(), anyLong(), anyBoolean())).willThrow(new OrganizationmanagerException(UNKNOWN_ERROR));

        mvc.perform(put(ENDPOINT + "/1/1/setDeletionState").with(jwt()).param("willBeDeleted", "true")).andExpect(status().is5xxServerError());
    }

    @Test
    void givenAuthentication_whenMarkForDeletion_thenOk() throws Exception {
        Space space = new Space();
        space.setName("test-space");
        space.setId(1L);
        space.setOrganizationId(1L);

        Space modifiedSpace = new Space();
        space.setName("test-space");
        space.setId(1L);
        space.setOrganizationId(1L);
        space.setState(State.CLOSED);
        space.setModified(ZonedDateTime.now());

        given(service.getSpaceById(any(), anyLong(), anyLong())).willReturn(space);
        given(orgaManagerService.updateSpace(any(), anyLong(), any())).willReturn(modifiedSpace);

        SpaceReadDTO spaceDTO = modelMapper.map(modifiedSpace, SpaceReadDTO.class);
        given(converter.convertToDTO(any(Space.class), eq(SpaceReadDTO.class))).willReturn(spaceDTO);
        given(orgaManagerService.getUserName(anyString())).willReturn("some user");

        mvc.perform(put(ENDPOINT + "/1/1/setDeletionState").with(jwt()).param("willBeDeleted", "true")).andExpect(status().isOk());
    }

    @Test
    void givenOrgaException_whenDeleteSpaceById() throws Exception {
        given(orgaManagerService.deleteSpace(any(), anyLong(), anyLong())).willThrow(new OrganizationmanagerException(FORBIDDEN));

        mvc.perform(delete(ENDPOINT + "/1/1").with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenAuthentication_whenDeleteSpaceById() throws Exception {
        given(orgaManagerService.deleteSpace(any(), anyLong(), anyLong())).willReturn(true);

        mvc.perform(delete(ENDPOINT + "/1/1").with(jwt())).andExpect(status().is2xxSuccessful());
    }

}
