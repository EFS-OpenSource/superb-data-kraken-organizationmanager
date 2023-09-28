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
import com.efs.sdk.organizationmanager.core.auth.model.OwnerDTO;
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.helper.AuthHelper;
import com.efs.sdk.organizationmanager.helper.EntityConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;
import java.util.List;

import static com.efs.sdk.organizationmanager.commons.OrganizationmanagerException.ORGANIZATIONMANAGER_ERROR.UNABLE_GET_ROLE;
import static com.efs.sdk.organizationmanager.core.auth.OrgaOwnerController.ENDPOINT;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrgaOwnerController.class)
@ActiveProfiles("test")
class OrgaOwnerControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String content = objectMapper.writeValueAsString(Collections.singletonList("id"));
    @Autowired
    private MockMvc mvc;
    @MockBean
    private OwnerService ownerService;
    /* required for tests to run */
    @MockBean
    private AuthHelper authHelper;
    /* required for tests to run */
    @MockBean
    private JwtDecoder jwtDecoder;
    /* required for tests to run */
    @MockBean
    private EntityConverter converter;

    public OrgaOwnerControllerTest() throws JsonProcessingException {
    }

    private static String getEndpoint(Long orgaId) {
        return ENDPOINT.replace("{orgaId}", String.valueOf(orgaId));
    }

    private static List<OwnerDTO> ownerDTOList() {
        OwnerDTO ownerDTO = new OwnerDTO("id", "firstname", "lastname");
        return Collections.singletonList(ownerDTO);
    }

    @Test
    void givenNoAuthentication_whenGetOwners_thenError() throws Exception {
        mvc.perform(get(getEndpoint(1L))).andExpect(status().is4xxClientError());
    }

    @Test
    void givenAuthentication_whenGetOwners_thenOk() throws Exception {
        mvc.perform(get(getEndpoint(1L)).with(jwt())).andExpect(status().isOk());
    }

    @Test
    void givenCorrectOwners_whenGetOwners_thenOk() throws Exception {
        given(ownerService.listOwners(any(), anyLong())).willReturn(ownerDTOList());
        MvcResult result = mvc.perform(get(getEndpoint(1L)).with(jwt())).andReturn();
        MockHttpServletResponse response = result.getResponse();
        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        JSONAssert.assertEquals(objectMapper.writeValueAsString(ownerDTOList()), response.getContentAsString(), false);
    }

    @Test
    void givenException_whenGetOwners_then5xxError() throws Exception {
        IllegalArgumentException except = new IllegalArgumentException("anything");
        given(ownerService.listOwners(any(), anyLong())).willThrow(except);

        mvc.perform(get(getEndpoint(1L)).with(jwt())).andExpect(status().is5xxServerError());
    }

    @Test
    void givenOrgamanException_whenGetOwners_then4xxError() throws Exception {
        given(ownerService.listOwners(any(), anyLong())).willThrow(new OrganizationmanagerException(UNABLE_GET_ROLE));
        mvc.perform(get(getEndpoint(1L)).with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenNoAuthentication_whenSetOwners_thenError() throws Exception {
        mvc.perform(put(getEndpoint(1L), Collections.singletonList("id")).contentType(APPLICATION_JSON).content(content)).andExpect(status().is4xxClientError());
    }

    @Test
    void givenAuthentication_whenSetOwners_thenOk() throws Exception {
        Organization organization = new Organization();
        organization.setName("test");
        given(ownerService.setOwners(any(), anyLong(), any())).willReturn(organization);
        mvc.perform(put(getEndpoint(1L), Collections.singletonList("id")).with(jwt()).contentType(APPLICATION_JSON).content(content)).andExpect(status().isOk());
    }

    @Test
    void givenException_whenSetOwners_then5xxError() throws Exception {
        IllegalArgumentException except = new IllegalArgumentException("anything");
        given(ownerService.setOwners(any(), anyLong(), any())).willThrow(except);

        mvc.perform(put(getEndpoint(1L), Collections.singletonList("id")).with(jwt()).contentType(APPLICATION_JSON).content(content)).andExpect(status().is5xxServerError());
    }

    @Test
    void givenOrgamanException_whenSetOwners_then4xxError() throws Exception {
        given(ownerService.setOwners(any(), anyLong(), any())).willThrow(new OrganizationmanagerException(UNABLE_GET_ROLE));
        mvc.perform(put(getEndpoint(1L), Collections.singletonList("id")).with(jwt()).contentType(APPLICATION_JSON).content(content)).andExpect(status().is4xxClientError());
    }


    @Test
    void givenNoAuthentication_whenAddOwnerByName_thenError() throws Exception {
        mvc.perform(put(format("%s/name/test", getEndpoint(1L)))).andExpect(status().is4xxClientError());
    }

    @Test
    void givenAuthentication_whenAddOwnerByName_thenOk() throws Exception {
        Organization organization = new Organization();
        organization.setName("test");
        given(ownerService.addOwnerByName(any(), anyLong(), any())).willReturn(organization);
        mvc.perform(put(format("%s/name/test", getEndpoint(1L))).with(jwt())).andExpect(status().isOk());
    }

    @Test
    void givenException_whenAddOwnerByName_then5xxError() throws Exception {
        IllegalArgumentException except = new IllegalArgumentException("anything");
        given(ownerService.addOwnerByName(any(), anyLong(), any())).willThrow(except);

        mvc.perform(put(format("%s/name/test", getEndpoint(1L))).with(jwt())).andExpect(status().is5xxServerError());
    }

    @Test
    void givenOrgamanException_whenAddOwnerByName_then4xxError() throws Exception {
        given(ownerService.addOwnerByName(any(), anyLong(), any())).willThrow(new OrganizationmanagerException(UNABLE_GET_ROLE));
        mvc.perform(put(format("%s/name/test", getEndpoint(1L))).with(jwt())).andExpect(status().is4xxClientError());
    }

    @Test
    void givenNoAuthentication_whenAddOwnerByEmail_thenError() throws Exception {
        mvc.perform(put(format("%s/email/test", getEndpoint(1L)))).andExpect(status().is4xxClientError());
    }

    @Test
    void givenAuthentication_whenAddOwnerByEmail_thenOk() throws Exception {
        Organization organization = new Organization();
        organization.setName("test");
        given(ownerService.addOwnerByEmail(any(), anyLong(), any())).willReturn(organization);
        mvc.perform(put(format("%s/email/test", getEndpoint(1L))).with(jwt())).andExpect(status().isOk());
    }

    @Test
    void givenException_whenAddOwnerByEmail_then5xxError() throws Exception {
        IllegalArgumentException except = new IllegalArgumentException("anything");
        given(ownerService.addOwnerByEmail(any(), anyLong(), any())).willThrow(except);

        mvc.perform(put(format("%s/email/test", getEndpoint(1L))).with(jwt())).andExpect(status().is5xxServerError());
    }

    @Test
    void givenOrgamanException_whenAddOwnerByEmail_then4xxError() throws Exception {
        given(ownerService.addOwnerByEmail(any(), anyLong(), any())).willThrow(new OrganizationmanagerException(UNABLE_GET_ROLE));
        mvc.perform(put(format("%s/email/test", getEndpoint(1L))).with(jwt())).andExpect(status().is4xxClientError());
    }
}
