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
package com.efs.sdk.organizationmanager.core.userrequest;

import com.efs.sdk.organizationmanager.commons.OrganizationmanagerException;
import com.efs.sdk.organizationmanager.core.OrganizationManagerService;
import com.efs.sdk.organizationmanager.core.userrequest.model.SpaceUserRequest;
import com.efs.sdk.organizationmanager.core.userrequest.model.UserRequestState;
import com.efs.sdk.organizationmanager.core.userrequest.model.dto.SpaceUserRequestDTO;
import com.efs.sdk.organizationmanager.helper.AuthHelper;
import com.efs.sdk.organizationmanager.helper.EntityConverter;
import com.efs.sdk.organizationmanager.helper.RoleHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static com.efs.sdk.organizationmanager.commons.OrganizationmanagerException.ORGANIZATIONMANAGER_ERROR.FORBIDDEN;
import static com.efs.sdk.organizationmanager.core.userrequest.SpaceUserRequestController.ENDPOINT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpaceUserRequestController.class)
@ActiveProfiles("test")
class SpaceUserRequestControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private EntityConverter converter;
    @MockBean
    private OrganizationManagerService orgaManagerService;
    /* required for tests to run */
    @MockBean
    private AuthHelper authHelper;

    @Test
    void givenNoAuthentication_whenGetSpaceRequests_thenError() throws Exception {
        mvc.perform(get(getEndpoint(1L, 1L) + "/1/1")).andExpect(status().is4xxClientError());
    }

    @Test
    void givenState_whenGetSpaceRequests_thenOk() throws Exception {
        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(UUID.randomUUID().toString());
        List<SpaceUserRequest> requests = List.of(spaceRequest);
        given(orgaManagerService.listSpaceRequests(any(), anyLong(), anyLong())).willReturn(requests);

        mvc.perform(get(getEndpoint(1L, 1L)).with(jwt())).andExpect(status().isOk());
    }

    @Test
    void givenNoState_whenGetSpaceRequests_thenOk() throws Exception {
        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(UUID.randomUUID().toString());
        List<SpaceUserRequest> requests = List.of(spaceRequest);
        given(orgaManagerService.listSpaceRequests(any(), anyLong(), anyLong(), any(UserRequestState.class))).willReturn(requests);

        mvc.perform(get(getEndpoint(1L, 1L)).with(jwt())).andExpect(status().isOk());
    }

    @Test
    void givenOrganizationmanagerException_whenGetSpaceRequests_thenClientError() throws Exception {
        given(orgaManagerService.listSpaceRequests(any(), anyLong(), anyLong())).willThrow(new OrganizationmanagerException(FORBIDDEN));
        mvc.perform(get(getEndpoint(1L, 1L)).with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenException_whenGetSpaceRequests_thenServerError() throws Exception {
        given(orgaManagerService.listSpaceRequests(any(), anyLong(), anyLong())).willThrow(new IllegalArgumentException("something went wrong"));
        mvc.perform(get(getEndpoint(1L, 1L)).with(jwt())).andExpect(status().is5xxServerError());
    }

    @Test
    void givenNoAuthentication_whenCreateSpaceRequest_thenError() throws Exception {
        mvc.perform(post(getEndpoint(1L, 1L))).andExpect(status().is4xxClientError());
    }

    @Test
    void givenOrganizationmanagerException_whenCreateSpaceRequest_thenClientError() throws Exception {
        given(orgaManagerService.createSpaceRequest(any(), anyLong(), anyLong(), any())).willThrow(new OrganizationmanagerException(FORBIDDEN));
        mvc.perform(post(getEndpoint(1L, 1L)).with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenNoAuthentication_whenAcceptSpaceRequest_thenError() throws Exception {
        mvc.perform(put(getEndpoint(1L, 1L) + "/1/accept")).andExpect(status().is4xxClientError());
    }

    @Test
    void givenAuthentication_whenAcceptSpaceRequest_thenOk() throws Exception {
        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(UUID.randomUUID().toString());

        SpaceUserRequestDTO dto = new SpaceUserRequestDTO();
        dto.setId(spaceRequest.getId());
        dto.setState(spaceRequest.getState());
        dto.setRole(spaceRequest.getRole());
        dto.setUserId(spaceRequest.getUserId());

        given(orgaManagerService.acceptSpaceRequest(any(), anyLong(), anyLong(), anyLong())).willReturn(spaceRequest);
        given(converter.convertToDTO(any(SpaceUserRequest.class))).willReturn(dto);

        MvcResult result = mvc.perform(put(getEndpoint(1L, 1L) + "/1/accept").with(jwt())).andReturn();
        MockHttpServletResponse response = result.getResponse();
        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        JSONAssert.assertEquals(objectMapper.writeValueAsString(spaceRequest), response.getContentAsString(), false);
    }

    @Test
    void givenOrganizationmanagerException_whenAcceptSpaceRequest_thenClientError() throws Exception {
        given(orgaManagerService.acceptSpaceRequest(any(), anyLong(), anyLong(), anyLong())).willThrow(new OrganizationmanagerException(FORBIDDEN));
        mvc.perform(put(getEndpoint(1L, 1L) + "/1/accept").with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenException_whenAcceptSpaceRequest_thenServerError() throws Exception {
        given(orgaManagerService.acceptSpaceRequest(any(), anyLong(), anyLong(), anyLong())).willThrow(new IllegalArgumentException("something went wrong"));
        mvc.perform(put(getEndpoint(1L, 1L) + "/1/accept").with(jwt())).andExpect(status().is5xxServerError());
    }

    @Test
    void givenNoAuthentication_whenDeclineSpaceRequest_thenError() throws Exception {
        mvc.perform(put(getEndpoint(1L, 1L) + "/1/decline")).andExpect(status().is4xxClientError());
    }

    @Test
    void givenAuthentication_whenDeclineSpaceRequest_thenOk() throws Exception {
        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(UUID.randomUUID().toString());

        SpaceUserRequestDTO dto = new SpaceUserRequestDTO();
        dto.setId(spaceRequest.getId());
        dto.setState(spaceRequest.getState());
        dto.setRole(spaceRequest.getRole());
        dto.setUserId(spaceRequest.getUserId());

        given(orgaManagerService.declineSpaceRequest(any(), anyLong(), anyLong(), anyLong())).willReturn(spaceRequest);
        given(converter.convertToDTO(any(SpaceUserRequest.class))).willReturn(dto);

        MvcResult result = mvc.perform(put(getEndpoint(1L, 1L) + "/1/decline").with(jwt())).andReturn();
        MockHttpServletResponse response = result.getResponse();
        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        JSONAssert.assertEquals(objectMapper.writeValueAsString(spaceRequest), response.getContentAsString(), false);
    }

    @Test
    void givenOrganizationmanagerException_whenDeclineSpaceRequest_thenClientError() throws Exception {
        given(orgaManagerService.declineSpaceRequest(any(), anyLong(), anyLong(), anyLong())).willThrow(new OrganizationmanagerException(FORBIDDEN));
        mvc.perform(put(getEndpoint(1L, 1L) + "/1/decline").with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenException_whenDeclineSpaceRequest_thenServerError() throws Exception {
        given(orgaManagerService.declineSpaceRequest(any(), anyLong(), anyLong(), anyLong())).willThrow(new IllegalArgumentException("something went wrong"));
        mvc.perform(put(getEndpoint(1L, 1L) + "/1/decline").with(jwt())).andExpect(status().is5xxServerError());
    }

    private static String getEndpoint(Long orgaId, Long spaceId) {
        return ENDPOINT.replace("{orgaId}", String.valueOf(orgaId)).replace("{spaceId}", String.valueOf(spaceId));
    }

}
