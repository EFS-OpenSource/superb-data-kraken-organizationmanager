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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

import static com.efs.sdk.common.domain.model.Confidentiality.PUBLIC;
import static com.efs.sdk.organizationmanager.commons.OrganizationmanagerException.ORGANIZATIONMANAGER_ERROR.*;
import static com.efs.sdk.organizationmanager.core.organization.model.Organization.REGEX_NAME;
import static com.efs.sdk.organizationmanager.helper.AuthConfiguration.GET;
import static com.efs.sdk.organizationmanager.helper.Utils.getSubject;
import static com.efs.sdk.organizationmanager.helper.Utils.isAdminOrOwner;

/**
 * Service for managing organizations
 *
 * @author e:fs TechHub GmbH
 */
@Service
public class OrganizationService {

    public static final String PROP_ORG_DELETED = "orgaDeleted";
    public static final String PROP_ORG_CREATED = "orgaCreated";
    private static final Logger LOG = LoggerFactory.getLogger(OrganizationService.class);
    private final PropertyChangeSupport pcs;
    private final OrganizationRepository repo;

    public OrganizationService(OrganizationRepository repo) {
        this.repo = repo;
        this.pcs = new PropertyChangeSupport(this);
    }

    /**
     * Creates an organization
     * <p>
     * That means it writes all the relevant information to the database, creates keycloak-roles,
     * opensearch- and storage-structure
     *
     * @param item the organization to create
     * @return the created organization
     * @throws OrganizationmanagerException error in storage-organization
     */
    public Organization createOrganizationEntity(Organization item) throws OrganizationmanagerException {
        LOG.info("Creating organization entity '{}'...", item.getName());

        if (item.getId() > 0) {
            LOG.info("Save organization - Id is not empty!");
            throw new OrganizationmanagerException(SAVE_PROVIDE_ID);
        }

        nameValidation(item);

        item.setCreated(ZonedDateTime.now());

        Organization persisted = repo.saveAndFlush(item);
        pcs.firePropertyChange(PROP_ORG_CREATED, null, persisted);

        LOG.info("Creating organization entity '{}'...successful", item.getName());
        return persisted;
    }

    public boolean deleteOrganizationEntity(Organization org) {
        repo.delete(org);
        pcs.firePropertyChange(PROP_ORG_DELETED, org, null);
        return true;
    }

    /**
     * Validates the name - not empty, non-existent, matches Pattern
     *
     * @param item the organization
     * @throws OrganizationmanagerException validation-violation
     */
    void nameValidation(Organization item) throws OrganizationmanagerException {
        // name must not be empty
        if (item.getName() == null || item.getName().isEmpty()) {
            LOG.info("Save organization - Required info not provided!");
            throw new OrganizationmanagerException(SAVE_REQUIRED_INFO_MISSING);
        }
        // name must fit certain requirements
        if (!item.getName().matches(REGEX_NAME)) {
            throw new OrganizationmanagerException(INVALID_NAME, REGEX_NAME);
        }
        // name must not be available yet
        Optional<Organization> orgaOpt = repo.findByName(item.getName());
        if (orgaOpt.isPresent() && orgaOpt.get().getId() != item.getId()) {
            throw new OrganizationmanagerException(SAVE_ORGANIZATION_NAME_FOUND);
        }
    }

    /**
     * Updates an organization
     *
     * @param item      the new organization
     * @param authModel AuthenticationModel
     * @return updated organization
     * @throws OrganizationmanagerException error in storage-organization
     */
    public Organization updateOrganizationEntity(Organization item, AuthenticationModel authModel) throws OrganizationmanagerException {
        LOG.info("Updating organization '{}'...", item.getName());

        nameValidation(item);

        Organization persisted = getOrganization(item.getId(), authModel);
        // updates only possible if user has admin-access -> no public access enabled
        if (!isAdminOrOwner(authModel, persisted)) {
            LOG.info("User is not allowed to update organization {}", item.getName());
            throw new OrganizationmanagerException(NO_ACCESS_TO_ORGANIZATION);
        }
        if (!persisted.getName().equals(item.getName())) {
            throw new OrganizationmanagerException(RENAMING_OBJECT_FORBIDDEN);
        }

        item.setCreated(persisted.getCreated());
        item.setModified(ZonedDateTime.now());

        LOG.info("Updating organization '{}'...successful", item.getName());
        return repo.saveAndFlush(item);
    }

    /**
     * Gets all organizations with access
     *
     * @param authModel            AuthenticationModel
     * @param allowedOrganizations Organizations the user has access to
     * @param authConfig           the permissions, the user should have
     * @return all organizations with access
     */
    public List<Organization> getAllOrganizations(AuthenticationModel authModel, String[] allowedOrganizations, AuthConfiguration authConfig) {
        LOG.info("Retrieve all organizations the user has access to");

        Set<Organization> orgas = new HashSet<>();
        // if user is superuser -> has access to all organizations
        if (GET.equals(authConfig) && authModel.isSuperuser()) {
            return repo.findAll();
        }

        AuthEntityOrganization[] orgaRoles = authModel.getOrganizations();

        // only add public organizations, if the user has org_all_public
        if (authModel.isOrgaPublicAccess()) {
            orgas.addAll(repo.findByConfidentiality(PUBLIC));
        }
        // user without organization-rights
        if (orgaRoles == null || orgaRoles.length < 1) {
            return orgas.stream().toList();
        }
        // find all organizations, the user has direct access to (ignore whether access or admin!)
        orgas.addAll(repo.findByNameIn(Arrays.stream(orgaRoles).map(AuthEntityOrganization::getOrganization).toList()));
        // find all organizations, the user is owner of
        orgas.addAll(repo.findByOwners(getSubject()));

        // if allowedOrganizations is null or empty, no permission-query-param was set and all
        // accessible organizations will be returned
        if (allowedOrganizations != null && allowedOrganizations.length > 0) {
            return orgas.stream().filter(o -> Stream.of(allowedOrganizations).anyMatch(allowedOrga -> allowedOrga.equalsIgnoreCase(o.getName()))).toList();
        }

        return orgas.stream().toList();
    }

    /**
     * Retrieves an organization by its ID.
     * <p>
     * This method searches for an organization based on a provided ID. It uses an authentication model to verify
     * the caller's rights to access the information. If the organization is found and the user has the necessary
     * access rights, it returns the organization. If the organization is not found or the user lacks access rights,
     * it throws an OrganizationmanagerException.
     *
     * @param id        The unique identifier for the organization.
     * @param authModel An instance of AuthenticationModel used to verify access rights.
     * @return Organization object representing the retrieved organization.
     * @throws OrganizationmanagerException If the organization with the specified ID is not found, or if the user does
     *                                      not have the rights to access the organization. This exception is also thrown
     *                                      if the organization is private and the user lacks the required access.
     */
    public Organization getOrganization(long id, AuthenticationModel authModel) throws OrganizationmanagerException {
        LOG.info("Retrieve an organization by id");

        Optional<Organization> orgaOpt = repo.findById(id);
        if (orgaOpt.isEmpty()) {
            throw new OrganizationmanagerException(GET_SINGLE_NOT_FOUND);
        }
        Organization organization = orgaOpt.get();
        // check if the user has access to the organization
        if (!canAccessOrganization(organization, authModel)) {
            throw new OrganizationmanagerException(NO_ACCESS_TO_ORGANIZATION);
        }
        return organization;
    }

    /**
     * Gets an organization by name
     *
     * @param name      the organization-name
     * @param authModel AuthenticationModel
     * @return the organization
     * @throws OrganizationmanagerException organization not found or invalid rights
     */
    public Organization getOrganizationByName(String name, AuthenticationModel authModel) throws OrganizationmanagerException {
        LOG.info("Retrieve an organization by name");

        Optional<Organization> orgaOpt = repo.findByName(name);
        if (orgaOpt.isEmpty()) {
            throw new OrganizationmanagerException(GET_SINGLE_NOT_FOUND);
        }
        Organization organization = orgaOpt.get();
        // check if user has access to organization or organization is public
        if (!canAccessOrganization(organization, authModel)) {
            throw new OrganizationmanagerException(NO_ACCESS_TO_ORGANIZATION);
        }
        return organization;
    }

    /**
     * Checks if user has access to organization by explicit role or via public
     * access
     *
     * @param orga      the organization
     * @param authModel AuthenticationModel
     * @return access
     */
    private boolean canAccessOrganization(Organization orga, AuthenticationModel authModel) {
        boolean publicAccess = PUBLIC.equals(orga.getConfidentiality()) && authModel.isOrgaPublicAccess();
        return publicAccess || organizationAccess(orga, authModel);
    }

    /**
     * Checks if user has access to organization by explicit role
     *
     * @param orga      the organization
     * @param authModel AuthenticationModel
     * @return access
     */
    private boolean organizationAccess(Organization orga, AuthenticationModel authModel) {
        // short-cut -> user can do anything if global superuser or owner of the organization
        if (isAdminOrOwner(authModel, orga)) {
            return true;
        }
        AuthEntityOrganization[] organizationAccess = authModel.getOrganizations();
        return organizationAccess != null && Stream.of(organizationAccess).anyMatch(orgaRole -> orgaRole.getOrganization().equalsIgnoreCase(orga.getName()));
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        pcs.addPropertyChangeListener(pcl);
    }
}
