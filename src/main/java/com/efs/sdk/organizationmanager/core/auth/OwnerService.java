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

import com.efs.sdk.logging.AuditLogger;
import com.efs.sdk.organizationmanager.commons.OrganizationmanagerException;
import com.efs.sdk.organizationmanager.core.auth.model.OwnerDTO;
import com.efs.sdk.organizationmanager.core.auth.model.UserDTO;
import com.efs.sdk.organizationmanager.core.organization.OrganizationService;
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.core.space.SpaceService;
import com.efs.sdk.organizationmanager.core.space.model.Space;
import com.efs.sdk.organizationmanager.helper.AuthenticationModel;
import com.efs.sdk.organizationmanager.helper.RoleHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.efs.sdk.organizationmanager.commons.OrganizationmanagerException.ORGANIZATIONMANAGER_ERROR.FORBIDDEN;
import static com.efs.sdk.organizationmanager.commons.OrganizationmanagerException.ORGANIZATIONMANAGER_ERROR.UNABLE_GET_USER;
import static com.efs.sdk.organizationmanager.helper.Utils.isOwner;
import static java.lang.String.format;

@Service
public class OwnerService {

    private static final Logger LOG = LoggerFactory.getLogger(OwnerService.class);
    private final OrganizationService orgaService;
    private final SpaceService spaceService;
    private final UserService userService;


    public OwnerService(OrganizationService orgaService, SpaceService spaceService, UserService userService) {
        this.orgaService = orgaService;
        this.spaceService = spaceService;
        this.userService = userService;
    }


    /**
     * List owners in the given organization
     *
     * @param authModel the authenticationmodel
     * @param orgaId    the organization-id
     * @return the owners
     * @throws OrganizationmanagerException thrown on errors
     */
    public List<OwnerDTO> listOwners(AuthenticationModel authModel, long orgaId) throws OrganizationmanagerException {
        Organization orga = orgaService.getOrganization(orgaId, authModel);
        return getOwnerDTOs(orga.getOwners());
    }

    /**
     * Sets owners in the given organization
     *
     * @param authModel the authenticationmodel
     * @param orgaId    the organization-id
     * @param owners    new list of owners
     * @return true
     * @throws OrganizationmanagerException thrown on errors
     */
    public Organization setOwners(AuthenticationModel authModel, long orgaId, List<String> owners) throws OrganizationmanagerException {
        Organization orga = orgaService.getOrganization(orgaId, authModel);
        if (!isAuthorizedToSetOrganizationOwners(authModel, orga)) {
            AuditLogger.error(LOG, "is not authorized to set owners for organization id {}",
                    authModel.getToken(), orgaId);
            throw new OrganizationmanagerException(FORBIDDEN);
        }
        for (String o : owners) {
            if (!userService.userExists(o)) {
                throw new OrganizationmanagerException(UNABLE_GET_USER, format(" with userId %s", o));
            }
        }
        orga.setOwners(owners);
        orgaService.updateOrganizationEntity(orga, authModel);
        AuditLogger.info(LOG, "successfully updated owners for organization id {}, owners: {}",
                authModel.getToken(), orgaId, owners);
        return orga;
    }

    public Organization setOwnersByEmail(AuthenticationModel authModel, long orgaId, List<String> mails) throws OrganizationmanagerException{
        // check if the caller has rights to set owners
        Organization orga = orgaService.getOrganization(orgaId, authModel);
        if (!isAuthorizedToSetOrganizationOwners(authModel, orga)) {
            AuditLogger.error(LOG, "is not authorized to set owners for organization id {}",
                    authModel.getToken(), orgaId);
            throw new OrganizationmanagerException(FORBIDDEN);
        }

        // get user ids
        ArrayList<String> owners = new ArrayList<>();
        for (var mail : mails) {
            owners.add(userService.getUserByEmail(mail).getId());
        }

        orga.setOwners(owners);
        orgaService.updateOrganizationEntity(orga, authModel);
        AuditLogger.info(LOG, "successfully updated owners for organization id {}, owners: {}",
                authModel.getToken(), orgaId, owners);
        return orga;
    }

    /**
     * Adds owner by Name in the given organization
     *
     * @param authModel the authenticationmodel
     * @param orgaId    the organization-id
     * @param name      name of new owner
     * @return organization
     * @throws OrganizationmanagerException thrown on errors
     */
    public Organization addOwnerByName(AuthenticationModel authModel, long orgaId, String name) throws OrganizationmanagerException {
        Organization orga = orgaService.getOrganization(orgaId, authModel);
        if (!isAuthorizedToSetOrganizationOwners(authModel, orga)) {
            AuditLogger.error(LOG, "is not authorized to add owners for organization id {}",
                    authModel.getToken(), orgaId);
            throw new OrganizationmanagerException(FORBIDDEN);
        }
        UserDTO user = userService.getUserByName(name);
        String userID = user.getId();
        if (!orga.getOwners().contains(userID)) {
            orga.addOwner(userID);
            orgaService.updateOrganizationEntity(orga, authModel);
            AuditLogger.info(LOG, "successfully added owner {} for organization id {}",
                    authModel.getToken(), name, orgaId);
        }
        return orga;
    }

    /**
     * Adds owner by Email in the given organization
     *
     * @param authModel the authenticationmodel
     * @param orgaId    the organization-id
     * @param email     email of new owner
     * @return organization
     * @throws OrganizationmanagerException thrown on errors
     */
    public Organization addOwnerByEmail(AuthenticationModel authModel, long orgaId, String email) throws OrganizationmanagerException {
        Organization orga = orgaService.getOrganization(orgaId, authModel);
        if (!isAuthorizedToSetOrganizationOwners(authModel, orga)) {
            AuditLogger.error(LOG, "is not authorized to add owner for organization id {}",
                    authModel.getToken(), orgaId);
            throw new OrganizationmanagerException(FORBIDDEN);
        }
        UserDTO user = userService.getUserByEmail(email);
        String userID = user.getId();
        if (!orga.getOwners().contains(userID)) {
            orga.addOwner(userID);
            orgaService.updateOrganizationEntity(orga, authModel);
            AuditLogger.info(LOG, "successfully added owner {} for organization id {}",
                    authModel.getToken(), userID, orgaId);
        }
        return orga;
    }

    /**
     * List owners in the given space
     *
     * @param authModel the authenticationmodel
     * @param orgaId    the organization-id
     * @param spaceId   the space-id
     * @return the owners
     * @throws OrganizationmanagerException thrown on errors
     */
    public List<OwnerDTO> listOwners(AuthenticationModel authModel, long orgaId, long spaceId) throws OrganizationmanagerException {
        Space space = spaceService.getSpaceById(authModel, orgaId, spaceId);
        return getOwnerDTOs(space.getOwners());
    }

    /**
     * Sets owners in the given space
     *
     * @param authModel the authenticationmodel
     * @param orgaId    the organization-id
     * @param spaceId   the space-id
     * @return true
     * @throws OrganizationmanagerException thrown on errors
     */
    public Space setOwners(AuthenticationModel authModel, long orgaId, long spaceId, List<String> owners) throws OrganizationmanagerException {
        Space space = spaceService.getSpaceById(authModel, orgaId, spaceId);
        if (!isAuthorizedToSetSpaceOwners(authModel, space)) {
            AuditLogger.error(LOG, "is not authorized to set owners for space id {} in organization id {}",
                    authModel.getToken(), spaceId, orgaId);
            throw new OrganizationmanagerException(FORBIDDEN);
        }
        // check, if each of the owners exists
        for (String o : owners) {
            if (!userService.userExists(o)) {
                throw new OrganizationmanagerException(UNABLE_GET_USER, format(" with userId %s", o));
            }
        }
        space.setOwners(owners);
        Organization orga = orgaService.getOrganization(orgaId, authModel);
        spaceService.updateSpaceEntity(authModel, orga, space);
        // assign each of the owners to all of the space-roles
        for (var owner : owners) {
            userService.assignRoles(orga, space, Arrays.stream(RoleHelper.SpaceScopeRole.values()).toList(), owner);
        }
        AuditLogger.info(LOG, "successfully added owners for space id {} in organization id {}, owners: {}",
                authModel.getToken(), owners, orgaId, spaceId);
        return space;
    }

    /**
     * Sets owners in the specified space using their email addresses.
     * <p>
     * This method assigns ownership of a space within an organization to a list of users identified by their email addresses.
     * It first validates the caller's authorization to set owners, converts the emails to user IDs, and updates the space ownership.
     * Each new owner is then assigned all roles associated with the space.
     * </p>
     *
     * @param authModel the authentication model containing user credentials and context
     * @param orgaId    the unique identifier of the organization
     * @param spaceId   the unique identifier of the space within the organization
     * @param mails     a list of email addresses corresponding to the users to be set as owners
     * @return the updated Space object with the new set of owners
     * @throws OrganizationmanagerException if the user is not authorized or if any other error occurs
     */
    public Space setOwnersByEmail(AuthenticationModel authModel, long orgaId, long spaceId, List<String> mails) throws OrganizationmanagerException {
        // check if the caller has rights to set owners
        Space space = spaceService.getSpaceById(authModel, orgaId, spaceId);
        if (!isAuthorizedToSetSpaceOwners(authModel, space)) {
            AuditLogger.error(LOG, "is not authorized to set owners for space id {} in organization id {}",
                    authModel.getToken(), spaceId, orgaId);
            throw new OrganizationmanagerException(FORBIDDEN);
        }

        // get user ids
        ArrayList<String> owners = new ArrayList<>();
        for (var mail : mails) {
            owners.add(userService.getUserByEmail(mail).getId());
        }

        space.setOwners(owners);
        Organization orga = orgaService.getOrganization(orgaId, authModel);
        spaceService.updateSpaceEntity(authModel, orga, space);

        // assign each of the owners to all of the space-roles
        for (var owner : owners) {
            userService.assignRoles(orga, space, Arrays.stream(RoleHelper.SpaceScopeRole.values()).toList(), owner);
        }
        AuditLogger.info(LOG, "successfully added owners for space id {} in organization id {}, owners: {}",
                authModel.getToken(), owners, orgaId, spaceId);
        return space;
    }

    /**
     * Adds owner by Name in the given organization
     *
     * @param authModel the authenticationmodel
     * @param orgaId    the organization-id
     * @param name      name of new owner
     * @return organization
     * @throws OrganizationmanagerException thrown on errors
     */
    public Space addOwnerByName(AuthenticationModel authModel, long orgaId, long spaceId, String name) throws OrganizationmanagerException {
        UserDTO user = userService.getUserByName(name);

        return addOwner(authModel, orgaId, spaceId, user);
    }

    /**
     * Adds owner by Email in the given organization
     *
     * @param authModel the authenticationmodel
     * @param orgaId    the organization-id
     * @param email     email of new owner
     * @return organization
     * @throws OrganizationmanagerException thrown on errors
     */
    public Space addOwnerByEmail(AuthenticationModel authModel, long orgaId, long spaceId, String email) throws OrganizationmanagerException {
        UserDTO user = userService.getUserByEmail(email);

        return addOwner(authModel, orgaId, spaceId, user);
    }

    private Space addOwner(AuthenticationModel authModel, long orgaId, long spaceId, UserDTO user) throws OrganizationmanagerException {
        Space space = spaceService.getSpaceById(authModel, orgaId, spaceId);
        if (!isAuthorizedToSetSpaceOwners(authModel, space)) {
            AuditLogger.error(LOG, "is not authorized to set owners for space id {} in organization id {}",
                    authModel.getToken(), spaceId, orgaId);
            throw new OrganizationmanagerException(FORBIDDEN);
        }

        String userID = user.getId();
        if (!space.getOwners().contains(userID)) {
            space.addOwner(userID);
            Organization orga = orgaService.getOrganization(orgaId, authModel);
            spaceService.updateSpaceEntity(authModel, orga, space);
            // assign all space-roles to owner
            userService.assignRoles(orga, space, Arrays.stream(RoleHelper.SpaceScopeRole.values()).toList(), userID);
            AuditLogger.info(LOG, "successfully added owner {} for space id {} in organization id {}",
                    authModel.getToken(), userID, spaceId, orgaId);
        }
        return space;
    }


    private boolean isAuthorizedToSetOrganizationOwners(AuthenticationModel authModel, Organization organization) {
        return authModel.isSuperuser() || isOwner(organization);
    }

    private boolean isAuthorizedToSetSpaceOwners(AuthenticationModel authModel, Space space) {
        return authModel.isSuperuser() || isOwner(space);
    }

    private List<OwnerDTO> getOwnerDTOs(List<String> userIds) throws OrganizationmanagerException {
        List<OwnerDTO> ownerDTOs = new ArrayList<>();
        for (String userId : userIds) {
            UserDTO user = userService.getUserView(userId);
            OwnerDTO ownerDTO = new OwnerDTO(userId, user.getFirstName(), user.getLastName());
            ownerDTOs.add(ownerDTO);
        }
        return ownerDTOs;
    }

}
