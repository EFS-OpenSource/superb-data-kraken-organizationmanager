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

import com.efs.sdk.logging.AuditLogger;
import com.efs.sdk.organizationmanager.commons.OrganizationmanagerException;
import com.efs.sdk.organizationmanager.core.organization.OrganizationService;
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.core.space.model.Space;
import com.efs.sdk.organizationmanager.helper.AuthConfiguration;
import com.efs.sdk.organizationmanager.helper.AuthEntityOrganization;
import com.efs.sdk.organizationmanager.helper.AuthenticationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.efs.sdk.common.domain.model.Confidentiality.PUBLIC;
import static com.efs.sdk.common.domain.model.State.CLOSED;
import static com.efs.sdk.common.domain.model.State.DELETION;
import static com.efs.sdk.organizationmanager.commons.OrganizationmanagerException.ORGANIZATIONMANAGER_ERROR.*;
import static com.efs.sdk.organizationmanager.core.space.model.Space.REGEX_NAME;
import static com.efs.sdk.organizationmanager.helper.AuthConfiguration.GET;
import static com.efs.sdk.organizationmanager.helper.AuthConfiguration.READ;
import static com.efs.sdk.organizationmanager.helper.Utils.isAdminOrOwner;
import static com.efs.sdk.organizationmanager.helper.Utils.isOwner;

/**
 * Service for managing spaces
 *
 * @author e:fs TechHub GmbH
 */
@Service
public class SpaceService implements PropertyChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(SpaceService.class);

    private final SpaceRepository repo;
    private final OrganizationService orgaService;

    public SpaceService(SpaceRepository repo, OrganizationService orgaService) {
        this.repo = repo;
        this.orgaService = orgaService;
        this.orgaService.addPropertyChangeListener(this);
    }

    /**
     * Delete Space from DB based. Comparing to another delete space function in that class (which will be called
     * on create-failed-event), this one function will be executed from endpoint.
     *
     * @param space the space
     * @return deleted space instance
     */
    public boolean deleteSpaceEntity(Space space) {
        repo.delete(space);
        return true;
    }

    public Space getSpaceByName(AuthenticationModel authModel, long orgaId, String spaceName) throws OrganizationmanagerException {
        LOG.info("Retrieve space with id {}", orgaId);
        Optional<Space> spaceOpt = repo.findByOrganizationIdAndName(orgaId, spaceName);

        return getSpace(orgaId, spaceOpt, authModel);
    }

    /**
     * Gets a space
     *
     * @param authModel AuthenticationModel
     * @param orgaId    the organization-id
     * @param spaceOpt  the space
     * @return the space
     * @throws OrganizationmanagerException space not found or invalid rights
     */
    private Space getSpace(long orgaId, Optional<Space> spaceOpt, AuthenticationModel authModel) throws OrganizationmanagerException {

        if (spaceOpt.isEmpty()) {
            throw new OrganizationmanagerException(GET_SINGLE_SPACE_NOT_FOUND);
        }
        Space space = spaceOpt.get();
        if (authModel.isSuperuser()) {
            return space;
        }

        // to check if one has access to the organization
        Organization orga = orgaService.getOrganization(orgaId, authModel);
        if (orga == null) {
            LOG.debug("got organization-rights");
            throw new OrganizationmanagerException(GET_SINGLE_NOT_FOUND);
        }

        if (!(isAdminOrOwner(authModel, orga, space) || canAccessSpace(space, authModel))) {
            throw new OrganizationmanagerException(NO_ACCESS_TO_SPACE);
        }
        return space;
    }

    /**
     * Gets a space
     *
     * @param authModel AuthenticationModel
     * @param orgaId    the organization-id
     * @param spaceId   the space
     * @return the space
     * @throws OrganizationmanagerException space not found or invalid rights
     */
    private Space getSpace(long orgaId, long spaceId, AuthenticationModel authModel, AuthConfiguration permission) throws OrganizationmanagerException {

        Optional<Space> spaceOpt = repo.findByOrganizationIdAndId(orgaId, spaceId);
        if (spaceOpt.isEmpty()) {
            throw new OrganizationmanagerException(GET_SINGLE_SPACE_NOT_FOUND);
        }
        Space space = spaceOpt.get();

        // to check if one has access to the organization
        Organization orga = orgaService.getOrganization(orgaId, authModel);
        if (orga == null) {
            throw new OrganizationmanagerException(GET_SINGLE_NOT_FOUND);
        }

        // check if user has the required permissions

        if (!(isAdminOrOwner(authModel, orga, space) || canAccessSpace(space, authModel))) {
            throw new OrganizationmanagerException(NO_ACCESS_TO_SPACE);
        }
        return space;
    }

    /**
     * Checks if user has access to given space
     *
     * @param space     the space
     * @param authModel the AuthenticationModel
     * @return access
     */
    private boolean canAccessSpace(Space space, AuthenticationModel authModel) {
        // if space is marked for deletion
        if (space.getState() != null && space.getState().equals(DELETION)) {
            return false;
        }

        boolean publicAccess = PUBLIC.equals(space.getConfidentiality()) && authModel.isSpacePublicAccess();
        return publicAccess || spaceAccess(space, authModel.getSpacesByPermission(READ)) || spaceAccess(space, authModel.getSpacesByPermission(GET));
    }

    /**
     * Checks if user has access to space by explicit role
     *
     * @param space       the space
     * @param spaceAccess the spaces where access is given
     * @return access
     */
    private boolean spaceAccess(Space space, String[] spaceAccess) {
        return spaceAccess != null && Stream.of(spaceAccess).anyMatch(access -> access.equalsIgnoreCase(space.getName()));
    }

    /**
     * Gets all spaces the user has the given permissions to within the given
     * organization
     *
     * @param authModel  AuthenticationModel
     * @param orgaId     the organization-id
     * @param authConfig the permissions, the user should have
     * @return all spaces with access
     */
    public List<Space> getSpaces(AuthenticationModel authModel, long orgaId, AuthConfiguration authConfig) throws OrganizationmanagerException {
        LOG.info("Retrieve all spaces the user has access to in organization with id {}", orgaId);
        // fast-lane for superuser
        if (GET.equals(authConfig) && authModel.isSuperuser()) {
            return repo.findByOrganizationId(orgaId);
        }
        // to check if one has access to the organization
        Organization orga = orgaService.getOrganization(orgaId, authModel);
        if (orga == null) {
            throw new OrganizationmanagerException(GET_SINGLE_NOT_FOUND);
        }
        // fast-lane for organization-admin - if admin than return all spaces
        if (GET.equals(authConfig) && authModel.getOrganizations() != null && Arrays.stream(authModel.getOrganizations()).anyMatch(orgaRole -> orgaRole.getOrganization().equals(orga.getName()) && AuthEntityOrganization.ADMIN_ROLE.equals(orgaRole.getRole()))) {
            return repo.findByOrganizationId(orgaId);
        }

        List<String> spaceNames = new ArrayList<>();
        if (authModel.getSpacesByPermission(authConfig) != null) {
            spaceNames.addAll(Stream.of(authModel.getSpacesByPermission(authConfig)).toList());
        }

        Set<Space> spaces = new HashSet<>(repo.findByOrganizationIdAndNameIn(orgaId, spaceNames));
        if ((READ.equals(authConfig) || GET.equals(authConfig)) && authModel.isSpacePublicAccess()) {
            spaces.addAll(repo.findByOrganizationIdAndConfidentiality(orgaId, PUBLIC));
        }

        // if not orga admin - hide spaces marked for deletion that aren't owned by current user
        if (!(authModel.isSuperuser() || authModel.isAdmin(orga.getName()))) {
            spaces =
                    spaces.stream().filter(space -> space.getState() == null || !space.getState().equals(DELETION) || isOwner(space)).collect(Collectors.toSet());
        }
        return spaces.stream().toList();
    }

    /**
     * Triggers SpaceService Methods based on changes in the OrganizationService.
     * Based on the Spring Observer Pattern
     * <a href="https://www.baeldung.com/java-observer-pattern">...</a>
     *
     * @param evt emited from the OrganizationService
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (OrganizationService.PROP_ORG_DELETED.equals(evt.getPropertyName())) {
            Object oldValue = evt.getOldValue();
            Organization orga = (Organization) oldValue;
            deleteSpaces(orga.getId());
        }
    }

    /**
     * Delete all Space of an organization from DB based on the orgaId
     * <br>
     * <b>CAUTION:</b> This function should only be called from flows, that check the permissions beforehand!!
     *
     * @param orgaId The organization whose spaces should be deleted
     * @return whether the spaces have been deleted
     */
    private boolean deleteSpaces(Long orgaId) {
        List<Space> spaces = repo.findByOrganizationId(orgaId);
        repo.deleteAll(spaces);
        return true;
    }

    /**
     * Sets Space from DB based
     *
     * @param authModel     authModel the AuthenticationModel
     * @param orgaId        the organization-id
     * @param spaceId       the space-id
     * @param willBeDeleted if true, the state will be set to be deleted; otherwise will be set to closed
     * @return updated space instance
     */
    public Space setDeletionState(AuthenticationModel authModel, long orgaId, long spaceId, boolean willBeDeleted) throws OrganizationmanagerException {
        Organization orga = orgaService.getOrganization(orgaId, authModel);

        Space space = getSpaceById(authModel, orgaId, spaceId);
        if (!isAdminOrOwner(authModel, orga, space)) {
            AuditLogger.error(LOG, "has no permission to change deletion state in space {} of organization {}",
                    authModel.getToken(), spaceId, orgaId);
            LOG.info("can be marked for deletion only by organization administrator");
            throw new OrganizationmanagerException(FORBIDDEN);
        }

        if (willBeDeleted) {
            space.setState(DELETION);
        } else {
            space.setState(CLOSED);
        }

        AuditLogger.info(LOG, "successfully set deletion state to {} for space {} in organization {}",
                authModel.getToken(), willBeDeleted, spaceId, orgaId);
        return updateSpaceEntity(authModel, orga, space);
    }

    public Space getSpaceById(AuthenticationModel authModel, long orgaId, long spaceId) throws OrganizationmanagerException {
        LOG.info("Retrieve space with id {}", orgaId);
        Optional<Space> spaceOpt = repo.findByOrganizationIdAndId(orgaId, spaceId);

        return getSpace(orgaId, spaceOpt, authModel);
    }

    /**
     * Updates a space entity in the database
     * <p>
     *
     * @param orga the organization
     * @param item the new space
     * @return updated space
     * @throws OrganizationmanagerException error in storage-organization
     */
    public Space updateSpaceEntity(AuthenticationModel authModel, Organization orga, Space item) throws OrganizationmanagerException {
        LOG.info("update Space {}", item.getName());

        // check if space exists within given organization
        Optional<Space> spaceOpt = repo.findByOrganizationIdAndId(orga.getId(), item.getId());
        if (spaceOpt.isEmpty()) {
            throw new OrganizationmanagerException(GET_SINGLE_SPACE_NOT_FOUND);
        }
        // validation
        nameValidation(item, orga.getId());

        Space persisted = spaceOpt.get();
        if (!isAdminOrOwner(authModel, orga, item)) {
            throw new OrganizationmanagerException(FORBIDDEN);
        }
        if (!persisted.getName().equals(item.getName())) {
            throw new OrganizationmanagerException(RENAMING_OBJECT_FORBIDDEN);
        }

        item.setCreated(persisted.getCreated());
        item.setModified(ZonedDateTime.now());

        return repo.saveAndFlush(item);
    }

    /**
     * Validates the name - not empty, non-existent within organization, matches
     * Pattern
     *
     * @param item the Space
     * @throws OrganizationmanagerException validation-violation
     */
    void nameValidation(Space item, Long orgaId) throws OrganizationmanagerException {
        // name must not be empty
        if (item.getName() == null || item.getName().isEmpty()) {
            throw new OrganizationmanagerException(SAVE_REQUIRED_INFO_MISSING);
        }
        // name must fit certain requirements
        if (!item.getName().matches(REGEX_NAME)) {
            throw new OrganizationmanagerException(INVALID_NAME, REGEX_NAME);
        }
        // name must not be available within the given organization yet
        Optional<Space> spaceOpt = repo.findByOrganizationIdAndName(orgaId, item.getName());
        if (spaceOpt.isPresent() && spaceOpt.get().getId() != item.getId()) {
            throw new OrganizationmanagerException(SAVE_SPACE_NAME_FOUND);
        }
    }

    /**
     * Creates a new space
     * <p>
     * That means it writes all the relevant information to the database and sends
     * an event on the "space-created" topic. This event will be consumed by the
     * Storage-manager that will then create the actual Azure Storage Container, the
     * OpenSearch Index and the role in keycloak
     *
     * @param org the organization the space should be attached to
     * @param spc the space to be created
     * @return the created space
     * @throws OrganizationmanagerException error in storage-organization
     */
    public Space createSpaceEntity(Organization org, Space spc) throws OrganizationmanagerException {
        LOG.info("create Space {} in organization {}", spc.getName(), org.getName());

        if (spc.getId() > 0) {
            throw new OrganizationmanagerException(SAVE_PROVIDE_ID);
        }

        // validation
        nameValidation(spc, org.getId());

        spc.setCreated(ZonedDateTime.now());
        spc.setOrganizationId(org.getId());
        return repo.saveAndFlush(spc);
    }

}
