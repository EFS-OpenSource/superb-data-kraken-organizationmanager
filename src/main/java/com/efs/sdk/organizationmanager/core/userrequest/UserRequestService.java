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
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static com.efs.sdk.organizationmanager.commons.OrganizationmanagerException.ORGANIZATIONMANAGER_ERROR.CONFLICTING_IDS_PROVIDED;
import static com.efs.sdk.organizationmanager.commons.OrganizationmanagerException.ORGANIZATIONMANAGER_ERROR.UNABLE_FIND_USERREQUEST;

/**
 * Service for managing Orga-/Space-UserRequests
 */
@Service
public class UserRequestService {

    private final OrganizationUserRequestRepository orgaRequestRepo;
    private final SpaceUserRequestRepository spaceRequestRepo;

    public UserRequestService(OrganizationUserRequestRepository orgaRequestRepo, SpaceUserRequestRepository spaceRequestRepo) {
        this.orgaRequestRepo = orgaRequestRepo;
        this.spaceRequestRepo = spaceRequestRepo;
    }

    /**
     * List all userrequests to the given organization
     *
     * @param orga the organization
     * @return all organization-userrequests
     */
    public List<OrganizationUserRequest> listUserRequests(Organization orga) {
        return orgaRequestRepo.findByOrgaId(orga.getId());
    }

    /**
     * List all userrequests to the given organization matching the given state
     *
     * @param orga  the organization
     * @param state the UserRequestState
     * @return all organization-userrequests
     */
    public List<OrganizationUserRequest> listUserRequests(Organization orga, UserRequestState state) {
        return orgaRequestRepo.findByOrgaIdAndState(orga.getId(), state);
    }

    /**
     * Creates an userrequest to the given organization
     *
     * @param item the OrganizationUserRequest
     * @return the created OrganizationUserRequest
     */
    public OrganizationUserRequest createUserRequest(OrganizationUserRequest item) {
        item.setCreated(ZonedDateTime.now());
        item.setState(UserRequestState.OPEN);
        return orgaRequestRepo.saveAndFlush(item);
    }

    /**
     * Accept organization-userrequest
     *
     * @param orga the organization
     * @param id   the id of the userrequest
     * @return the updated userrequest
     * @throws OrganizationmanagerException thrown on errors
     */
    public OrganizationUserRequest acceptUserRequest(Organization orga, long id) throws OrganizationmanagerException {
        return updateUserRequestState(orga, id, UserRequestState.ACCEPTED);
    }

    /**
     * Decline organization-userrequest
     *
     * @param orga the organization
     * @param id   the id of the userrequest
     * @return the updated userrequest
     * @throws OrganizationmanagerException thrown on errors
     */
    public OrganizationUserRequest declineUserRequest(Organization orga, long id) throws OrganizationmanagerException {
        return updateUserRequestState(orga, id, UserRequestState.DECLINED);
    }

    /**
     * Update state of organization-userrequest
     *
     * @param orga  the organization
     * @param id    the id of the userrequest
     * @param state the requested UserRequestState
     * @return the updated userrequest
     * @throws OrganizationmanagerException thrown on errors
     */
    private OrganizationUserRequest updateUserRequestState(Organization orga, long id, UserRequestState state) throws OrganizationmanagerException {
        Optional<OrganizationUserRequest> userRequestOpt = orgaRequestRepo.findById(id);
        if (userRequestOpt.isEmpty()) {
            throw new OrganizationmanagerException(UNABLE_FIND_USERREQUEST);
        }
        OrganizationUserRequest userRequest = userRequestOpt.get();
        if (userRequest.getOrgaId() != orga.getId()) {
            throw new OrganizationmanagerException(CONFLICTING_IDS_PROVIDED);
        }
        userRequest.setModified(ZonedDateTime.now());
        userRequest.setState(state);
        return orgaRequestRepo.saveAndFlush(userRequest);
    }

    /**
     * List all userrequests to the given space
     *
     * @param orga  the organization
     * @param space the space
     * @return all organization-userrequests
     */
    public List<SpaceUserRequest> listUserRequests(Organization orga, Space space) {
        return spaceRequestRepo.findByOrgaIdAndSpaceId(orga.getId(), space.getId());
    }

    /**
     * List all userrequests to the given space matching the given state
     *
     * @param orga  the organization
     * @param space the space
     * @param state the UserRequestState
     * @return all organization-userrequests
     */
    public List<SpaceUserRequest> listUserRequests(Organization orga, Space space, UserRequestState state) {
        return spaceRequestRepo.findByOrgaIdAndSpaceIdAndState(orga.getId(), space.getId(), state);
    }

    /**
     * Creates an userrequest to the given space
     *
     * @param item the SpaceUserRequest
     * @return the created SpaceUserRequest
     */
    public SpaceUserRequest createUserRequest(SpaceUserRequest item) {
        item.setCreated(ZonedDateTime.now());
        item.setState(UserRequestState.OPEN);
        return spaceRequestRepo.saveAndFlush(item);
    }

    /**
     * Accept space-userrequest
     *
     * @param orga  the organization
     * @param space the space
     * @param id    the id of the userrequest
     * @return the updated userrequest
     * @throws OrganizationmanagerException thrown on errors
     */
    public SpaceUserRequest acceptUserRequest(Organization orga, Space space, long id) throws OrganizationmanagerException {
        return updateUserRequestState(orga, space, id, UserRequestState.ACCEPTED);
    }

    /**
     * Decline space-userrequest
     *
     * @param orga  the organization
     * @param space the space
     * @param id    the id of the userrequest
     * @return the updated userrequest
     * @throws OrganizationmanagerException thrown on errors
     */
    public SpaceUserRequest declineUserRequest(Organization orga, Space space, long id) throws OrganizationmanagerException {
        return updateUserRequestState(orga, space, id, UserRequestState.DECLINED);
    }

    /**
     * Update state of space-userrequest
     *
     * @param orga  the organization
     * @param space the space
     * @param id    the id of the userrequest
     * @param state the requested UserRequestState
     * @return the updated userrequest
     * @throws OrganizationmanagerException thrown on errors
     */
    private SpaceUserRequest updateUserRequestState(Organization orga, Space space, long id, UserRequestState state) throws OrganizationmanagerException {
        Optional<SpaceUserRequest> userRequestOpt = spaceRequestRepo.findById(id);
        if (userRequestOpt.isEmpty()) {
            throw new OrganizationmanagerException(UNABLE_FIND_USERREQUEST);
        }
        SpaceUserRequest userRequest = userRequestOpt.get();
        if (userRequest.getOrgaId() != orga.getId() || userRequest.getSpaceId() != space.getId()) {
            throw new OrganizationmanagerException(CONFLICTING_IDS_PROVIDED);
        }
        userRequest.setModified(ZonedDateTime.now());
        userRequest.setState(state);
        return spaceRequestRepo.saveAndFlush(userRequest);
    }
}
