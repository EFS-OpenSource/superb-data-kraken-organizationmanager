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
import com.efs.sdk.organizationmanager.core.userrequest.model.OrganizationUserRequest;
import com.efs.sdk.organizationmanager.core.userrequest.model.UserRequestState;
import com.efs.sdk.organizationmanager.core.userrequest.model.dto.OrganizationUserRequestDTO;
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
import static com.efs.sdk.organizationmanager.core.userrequest.OrgaUserRequestController.ENDPOINT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrgaUserRequestController.class)
@ActiveProfiles("test")
class OrgaUserRequestControllerTest {
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
    void givenNoAuthentication_whenGetOrganizationRequests_thenError() throws Exception {
        mvc.perform(get(getEndpoint(1L))).andExpect(status().is4xxClientError());
    }

    @Test
    void givenState_whenGetOrganizationRequests_thenOk() throws Exception {
        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setState(UserRequestState.OPEN);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(UUID.randomUUID().toString());
        List<OrganizationUserRequest> requests = List.of(orgaRequest);
        given(orgaManagerService.listOrganizationRequests(any(), anyLong(), any())).willReturn(requests);

        mvc.perform(get(getEndpoint(1L)).param("state", UserRequestState.OPEN.name()).with(jwt())).andExpect(status().isOk());
    }

    @Test
    void givenNoState_whenGetOrganizationRequests_thenOk() throws Exception {
        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setState(UserRequestState.OPEN);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(UUID.randomUUID().toString());
        List<OrganizationUserRequest> requests = List.of(orgaRequest);
        given(orgaManagerService.listOrganizationRequests(any(), anyLong(), any(UserRequestState.class))).willReturn(requests);

        mvc.perform(get(getEndpoint(1L)).with(jwt())).andExpect(status().isOk());
    }

    @Test
    void givenOrganizationmanagerException_whenGetOrganizationRequests_thenClientError() throws Exception {
        given(orgaManagerService.listOrganizationRequests(any(), anyLong())).willThrow(new OrganizationmanagerException(FORBIDDEN));
        mvc.perform(get(getEndpoint(1L)).with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenException_whenGetOrganizationRequests_thenServerError() throws Exception {
        given(orgaManagerService.listOrganizationRequests(any(), anyLong())).willThrow(new IllegalArgumentException("something went wrong"));
        mvc.perform(get(getEndpoint(1L)).with(jwt())).andExpect(status().is5xxServerError());
    }

    @Test
    void givenNoAuthentication_whenCreateOrganizationRequest_thenError() throws Exception {
        mvc.perform(post(getEndpoint(1L))).andExpect(status().is4xxClientError());
    }

    @Test
    void givenOrganizationmanagerException_whenCreateOrganizationRequest_thenClientError() throws Exception {
        given(orgaManagerService.createOrganizationRequest(any(), anyLong(), any())).willThrow(new OrganizationmanagerException(FORBIDDEN));
        mvc.perform(post(getEndpoint(1L)).with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenNoAuthentication_whenAcceptOrganizationRequest_thenError() throws Exception {
        mvc.perform(put(getEndpoint(1L) + "/1/accept")).andExpect(status().is4xxClientError());
    }

    @Test
    void givenAuthentication_whenAcceptOrganizationRequest_thenOk() throws Exception {
        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setState(UserRequestState.OPEN);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(UUID.randomUUID().toString());

        OrganizationUserRequestDTO dto = new OrganizationUserRequestDTO();
        dto.setId(orgaRequest.getId());
        dto.setState(orgaRequest.getState());
        dto.setRole(orgaRequest.getRole());
        dto.setUserId(orgaRequest.getUserId());
        given(orgaManagerService.acceptOrganizationRequest(any(), anyLong(), anyLong())).willReturn(orgaRequest);
        given(converter.convertToDTO(any(OrganizationUserRequest.class))).willReturn(dto);

        MvcResult result = mvc.perform(put(getEndpoint(1L) + "/1/accept").with(jwt())).andReturn();
        MockHttpServletResponse response = result.getResponse();
        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        JSONAssert.assertEquals(objectMapper.writeValueAsString(orgaRequest), response.getContentAsString(), false);
    }

    @Test
    void givenOrganizationmanagerException_whenAcceptOrganizationRequest_thenClientError() throws Exception {
        given(orgaManagerService.acceptOrganizationRequest(any(), anyLong(), anyLong())).willThrow(new OrganizationmanagerException(FORBIDDEN));
        mvc.perform(put(getEndpoint(1L) + "/1/accept").with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenException_whenAcceptOrganizationRequest_thenServerError() throws Exception {
        given(orgaManagerService.acceptOrganizationRequest(any(), anyLong(), anyLong())).willThrow(new IllegalArgumentException("something went wrong"));
        mvc.perform(put(getEndpoint(1L) + "/1/accept").with(jwt())).andExpect(status().is5xxServerError());
    }

    @Test
    void givenNoAuthentication_whenDeclineOrganizationRequest_thenError() throws Exception {
        mvc.perform(put(getEndpoint(1L) + "/1/decline")).andExpect(status().is4xxClientError());
    }

    @Test
    void givenAuthentication_whenDeclineOrganizationRequest_thenOk() throws Exception {
        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setState(UserRequestState.OPEN);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(UUID.randomUUID().toString());

        OrganizationUserRequestDTO dto = new OrganizationUserRequestDTO();
        dto.setId(orgaRequest.getId());
        dto.setState(orgaRequest.getState());
        dto.setRole(orgaRequest.getRole());
        dto.setUserId(orgaRequest.getUserId());
        given(orgaManagerService.declineOrganizationRequest(any(), anyLong(), anyLong())).willReturn(orgaRequest);
        given(converter.convertToDTO(any(OrganizationUserRequest.class))).willReturn(dto);

        MvcResult result = mvc.perform(put(getEndpoint(1L) + "/1/decline").with(jwt())).andReturn();
        MockHttpServletResponse response = result.getResponse();
        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        JSONAssert.assertEquals(objectMapper.writeValueAsString(orgaRequest), response.getContentAsString(), false);
    }

    @Test
    void givenOrganizationmanagerException_whenDeclineOrganizationRequest_thenClientError() throws Exception {
        given(orgaManagerService.declineOrganizationRequest(any(), anyLong(), anyLong())).willThrow(new OrganizationmanagerException(FORBIDDEN));
        mvc.perform(put(getEndpoint(1L) + "/1/decline").with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenException_whenDeclineOrganizationRequest_thenServerError() throws Exception {
        given(orgaManagerService.declineOrganizationRequest(any(), anyLong(), anyLong())).willThrow(new IllegalArgumentException("something went wrong"));
        mvc.perform(put(getEndpoint(1L) + "/1/decline").with(jwt())).andExpect(status().is5xxServerError());
    }

    private static String getEndpoint(Long orgaId) {
        return ENDPOINT.replace("{orgaId}", String.valueOf(orgaId));
    }
}
