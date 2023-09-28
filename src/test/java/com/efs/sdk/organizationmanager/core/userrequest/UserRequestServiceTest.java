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
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.core.space.model.Space;
import com.efs.sdk.organizationmanager.core.userrequest.model.OrganizationUserRequest;
import com.efs.sdk.organizationmanager.core.userrequest.model.SpaceUserRequest;
import com.efs.sdk.organizationmanager.core.userrequest.model.UserRequestState;
import com.efs.sdk.organizationmanager.helper.RoleHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.efs.sdk.common.domain.model.Confidentiality.INTERNAL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

class UserRequestServiceTest {

    private UserRequestService service;
    @MockBean
    private OrganizationUserRequestRepository orgaRequestRepo;
    @MockBean
    private SpaceUserRequestRepository spaceRequestRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        this.orgaRequestRepo = Mockito.mock(OrganizationUserRequestRepository.class);
        this.spaceRequestRepo = Mockito.mock(SpaceUserRequestRepository.class);
        this.service = new UserRequestService(orgaRequestRepo, spaceRequestRepo);
    }

    @Test
    void givenOrga_whenListOrganizationRequests_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");
        orga.setConfidentiality(INTERNAL);

        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setState(UserRequestState.OPEN);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(UUID.randomUUID().toString());
        orgaRequest.setOrgaId(orga.getId());
        List<OrganizationUserRequest> requests = List.of(orgaRequest);
        given(orgaRequestRepo.findByOrgaId(anyLong())).willReturn(requests);

        JSONAssert.assertEquals(objectMapper.writeValueAsString(requests), objectMapper.writeValueAsString(service.listUserRequests(orga)), false);
    }

    @Test
    void givenState_whenListOrganizationRequests_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");
        orga.setConfidentiality(INTERNAL);

        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setState(UserRequestState.OPEN);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(UUID.randomUUID().toString());
        orgaRequest.setOrgaId(orga.getId());
        List<OrganizationUserRequest> requests = List.of(orgaRequest);
        given(orgaRequestRepo.findByOrgaIdAndState(anyLong(), any())).willReturn(requests);

        JSONAssert.assertEquals(objectMapper.writeValueAsString(requests), objectMapper.writeValueAsString(service.listUserRequests(orga,
                UserRequestState.OPEN)), false);
    }

    @Test
    void givenOrgaRequest_whenCreateUserRequest_thenCreatedAndOpen() {
        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(UUID.randomUUID().toString());
        orgaRequest.setOrgaId(1L);

        given(orgaRequestRepo.saveAndFlush(any())).willReturn(orgaRequest);
        OrganizationUserRequest stored = service.createUserRequest(orgaRequest);
        assertNotNull(stored);
        assertNotNull(stored.getCreated());
        assertNull(stored.getModified());
        assertNotNull(stored.getState());
    }

    @Test
    void givenOrgaRequestFound_whenAcceptUserRequest_thenModified() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");
        orga.setConfidentiality(INTERNAL);

        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setState(UserRequestState.OPEN);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(UUID.randomUUID().toString());
        orgaRequest.setOrgaId(orga.getId());

        given(orgaRequestRepo.findById(anyLong())).willReturn(Optional.of(orgaRequest));
        given(orgaRequestRepo.saveAndFlush(any())).willReturn(orgaRequest);

        OrganizationUserRequest stored = service.acceptUserRequest(orga, orgaRequest.getId());
        assertNotNull(stored);
        assertNotNull(stored.getModified());
        assertEquals(UserRequestState.ACCEPTED, stored.getState());
    }

    @Test
    void givenOrgaRequestNotFound_whenAcceptUserRequest_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");
        orga.setConfidentiality(INTERNAL);

        given(orgaRequestRepo.findById(anyLong())).willReturn(Optional.empty());
        assertThrows(OrganizationmanagerException.class, () -> service.acceptUserRequest(orga, 1L));
    }

    @Test
    void givenMissmatchingOrgaId_whenAcceptUserRequest_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");
        orga.setConfidentiality(INTERNAL);

        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setState(UserRequestState.OPEN);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(UUID.randomUUID().toString());
        orgaRequest.setOrgaId(2L);

        given(orgaRequestRepo.findById(anyLong())).willReturn(Optional.of(orgaRequest));
        assertThrows(OrganizationmanagerException.class, () -> service.acceptUserRequest(orga, orgaRequest.getId()));
    }

    @Test
    void givenOrgaRequestFound_whenDeclineUserRequest_thenModified() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");
        orga.setConfidentiality(INTERNAL);

        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setState(UserRequestState.OPEN);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(UUID.randomUUID().toString());
        orgaRequest.setOrgaId(orga.getId());

        given(orgaRequestRepo.findById(anyLong())).willReturn(Optional.of(orgaRequest));
        given(orgaRequestRepo.saveAndFlush(any())).willReturn(orgaRequest);

        OrganizationUserRequest stored = service.declineUserRequest(orga, orgaRequest.getId());
        assertNotNull(stored);
        assertNotNull(stored.getModified());
        assertEquals(UserRequestState.DECLINED, stored.getState());
    }

    @Test
    void givenOrgaRequestNotFound_whenDeclineUserRequest_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");
        orga.setConfidentiality(INTERNAL);

        given(orgaRequestRepo.findById(anyLong())).willReturn(Optional.empty());
        assertThrows(OrganizationmanagerException.class, () -> service.declineUserRequest(orga, 1L));
    }

    @Test
    void givenMissmatchingOrgaId_whenDeclineUserRequest_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");
        orga.setConfidentiality(INTERNAL);

        OrganizationUserRequest orgaRequest = new OrganizationUserRequest();
        orgaRequest.setId(1L);
        orgaRequest.setState(UserRequestState.OPEN);
        orgaRequest.setRole(RoleHelper.OrganizationScopeRole.ACCESS);
        orgaRequest.setUserId(UUID.randomUUID().toString());
        orgaRequest.setOrgaId(2L);

        given(orgaRequestRepo.findById(anyLong())).willReturn(Optional.of(orgaRequest));
        assertThrows(OrganizationmanagerException.class, () -> service.declineUserRequest(orga, orgaRequest.getId()));
    }

    @Test
    void givenSpace_whenListSpaceRequests_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");
        orga.setConfidentiality(INTERNAL);

        Space space = new Space();
        space.setId(1L);

        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(UUID.randomUUID().toString());
        spaceRequest.setOrgaId(orga.getId());
        List<SpaceUserRequest> requests = List.of(spaceRequest);
        given(spaceRequestRepo.findByOrgaIdAndSpaceId(anyLong(), anyLong())).willReturn(requests);

        JSONAssert.assertEquals(objectMapper.writeValueAsString(requests), objectMapper.writeValueAsString(service.listUserRequests(orga, space)), false);
    }

    @Test
    void givenState_whenListSpaceRequests_thenOk() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");
        orga.setConfidentiality(INTERNAL);

        Space space = new Space();
        space.setId(1L);

        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(UUID.randomUUID().toString());
        spaceRequest.setOrgaId(orga.getId());
        List<SpaceUserRequest> requests = List.of(spaceRequest);
        given(spaceRequestRepo.findByOrgaIdAndSpaceIdAndState(anyLong(), anyLong(), any())).willReturn(requests);

        JSONAssert.assertEquals(objectMapper.writeValueAsString(requests), objectMapper.writeValueAsString(service.listUserRequests(orga, space,
                UserRequestState.OPEN)), false);
    }

    @Test
    void givenSpaceRequest_whenCreateUserRequest_thenCreatedAndOpen() {
        Organization orga = new Organization();
        orga.setId(1L);

        Space space = new Space();
        space.setId(1L);

        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(UUID.randomUUID().toString());
        spaceRequest.setOrgaId(orga.getId());
        spaceRequest.setSpaceId(space.getId());

        given(spaceRequestRepo.saveAndFlush(any())).willReturn(spaceRequest);
        SpaceUserRequest stored = service.createUserRequest(spaceRequest);
        assertNotNull(stored);
        assertNotNull(stored.getCreated());
        assertNull(stored.getModified());
        assertNotNull(stored.getState());
    }

    @Test
    void givenSpaceRequestFound_whenAcceptUserRequest_thenModified() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");
        orga.setConfidentiality(INTERNAL);

        Space space = new Space();
        space.setId(1L);

        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(UUID.randomUUID().toString());
        spaceRequest.setOrgaId(orga.getId());
        spaceRequest.setSpaceId(space.getId());

        given(spaceRequestRepo.findById(anyLong())).willReturn(Optional.of(spaceRequest));
        given(spaceRequestRepo.saveAndFlush(any())).willReturn(spaceRequest);

        SpaceUserRequest stored = service.acceptUserRequest(orga, space, spaceRequest.getId());
        assertNotNull(stored);
        assertNotNull(stored.getModified());
        assertEquals(UserRequestState.ACCEPTED, stored.getState());
    }

    @Test
    void givenSpaceRequestNotFound_whenAcceptUserRequest_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);

        Space space = new Space();
        space.setId(1L);

        given(spaceRequestRepo.findById(anyLong())).willReturn(Optional.empty());
        assertThrows(OrganizationmanagerException.class, () -> service.acceptUserRequest(orga, space, 1L));
    }

    @Test
    void givenMissmatchingOrgaId_whenAcceptSpaceUserRequest_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);

        Space space = new Space();
        space.setId(1L);

        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(UUID.randomUUID().toString());
        spaceRequest.setOrgaId(2L);
        spaceRequest.setSpaceId(space.getId());

        given(spaceRequestRepo.findById(anyLong())).willReturn(Optional.of(spaceRequest));
        assertThrows(OrganizationmanagerException.class, () -> service.acceptUserRequest(orga, space, spaceRequest.getId()));
    }

    @Test
    void givenMissmatchingSpaceId_whenAcceptSpaceUserRequest_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);

        Space space = new Space();
        space.setId(1L);

        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(UUID.randomUUID().toString());
        spaceRequest.setOrgaId(orga.getId());
        spaceRequest.setSpaceId(2L);

        given(spaceRequestRepo.findById(anyLong())).willReturn(Optional.of(spaceRequest));
        assertThrows(OrganizationmanagerException.class, () -> service.acceptUserRequest(orga, space, spaceRequest.getId()));
    }

    @Test
    void givenSpaceRequestFound_whenDeclineUserRequest_thenModified() throws Exception {
        Organization orga = new Organization();
        orga.setId(1L);

        Space space = new Space();
        space.setId(1L);

        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(UUID.randomUUID().toString());
        spaceRequest.setOrgaId(orga.getId());
        spaceRequest.setSpaceId(space.getId());

        given(spaceRequestRepo.findById(anyLong())).willReturn(Optional.of(spaceRequest));
        given(spaceRequestRepo.saveAndFlush(any())).willReturn(spaceRequest);

        SpaceUserRequest stored = service.declineUserRequest(orga, space, spaceRequest.getId());
        assertNotNull(stored);
        assertNotNull(stored.getModified());
        assertEquals(UserRequestState.DECLINED, stored.getState());
    }

    @Test
    void givenSpaceRequestNotFound_whenDeclineUserRequest_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);

        Space space = new Space();
        space.setId(1L);

        given(spaceRequestRepo.findById(anyLong())).willReturn(Optional.empty());
        assertThrows(OrganizationmanagerException.class, () -> service.declineUserRequest(orga, space, 1L));
    }

    @Test
    void givenMissmatchingOrgaId_whenDeclineSpaceUserRequest_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);

        Space space = new Space();
        space.setId(1L);

        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(UUID.randomUUID().toString());
        spaceRequest.setOrgaId(2L);
        spaceRequest.setSpaceId(space.getId());

        given(spaceRequestRepo.findById(anyLong())).willReturn(Optional.of(spaceRequest));
        assertThrows(OrganizationmanagerException.class, () -> service.declineUserRequest(orga, space, spaceRequest.getId()));
    }

    @Test
    void givenMissmatchingSpaceId_whenDeclineUserRequest_thenError() {
        Organization orga = new Organization();
        orga.setId(1L);

        Space space = new Space();
        space.setId(1L);

        SpaceUserRequest spaceRequest = new SpaceUserRequest();
        spaceRequest.setId(1L);
        spaceRequest.setState(UserRequestState.OPEN);
        spaceRequest.setRole(RoleHelper.SpaceScopeRole.USER);
        spaceRequest.setUserId(UUID.randomUUID().toString());
        spaceRequest.setOrgaId(orga.getId());
        spaceRequest.setSpaceId(2L);

        given(spaceRequestRepo.findById(anyLong())).willReturn(Optional.of(spaceRequest));
        assertThrows(OrganizationmanagerException.class, () -> service.declineUserRequest(orga, space, spaceRequest.getId()));
    }
}
