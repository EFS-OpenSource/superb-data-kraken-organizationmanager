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
package com.efs.sdk.organizationmanager.core;

import com.efs.sdk.common.domain.dto.OrganizationCreateDTO;
import com.efs.sdk.logging.AuditLogger;
import com.efs.sdk.organizationmanager.commons.OrganizationmanagerException;
import com.efs.sdk.organizationmanager.core.auth.AuthService;
import com.efs.sdk.organizationmanager.core.auth.RoleService;
import com.efs.sdk.organizationmanager.core.auth.UserService;
import com.efs.sdk.organizationmanager.core.auth.model.OrganizationUserDTO;
import com.efs.sdk.organizationmanager.core.auth.model.RoleDTO;
import com.efs.sdk.organizationmanager.core.auth.model.SpaceUserDTO;
import com.efs.sdk.organizationmanager.core.auth.model.UserDTO;
import com.efs.sdk.organizationmanager.core.clients.AbstractServiceRestClient;
import com.efs.sdk.organizationmanager.core.events.EventPublisher;
import com.efs.sdk.organizationmanager.core.organization.OrganizationService;
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.core.space.SpaceService;
import com.efs.sdk.organizationmanager.core.space.model.Space;
import com.efs.sdk.organizationmanager.core.userrequest.UserRequestService;
import com.efs.sdk.organizationmanager.core.userrequest.model.OrganizationUserRequest;
import com.efs.sdk.organizationmanager.core.userrequest.model.SpaceUserRequest;
import com.efs.sdk.organizationmanager.core.userrequest.model.UserRequestState;
import com.efs.sdk.organizationmanager.helper.AuthConfiguration;
import com.efs.sdk.organizationmanager.helper.AuthenticationModel;
import com.efs.sdk.organizationmanager.helper.EntityConverter;
import com.efs.sdk.organizationmanager.helper.RoleHelper;
import com.efs.sdk.storage.SpaceBase;
import com.efs.sdk.storage.SpaceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static com.efs.sdk.organizationmanager.commons.OrganizationmanagerException.ORGANIZATIONMANAGER_ERROR.*;
import static com.efs.sdk.organizationmanager.helper.Utils.*;
import static java.lang.String.format;

@Service
public class OrganizationManagerService {

    private static final Logger LOG = LoggerFactory.getLogger(OrganizationManagerService.class);

    private final OrganizationService orgaService;
    private final SpaceService spaceService;
    private final RoleService roleService;
    private final AuthService authService;
    private final UserService userService;
    private final UserRequestService userRequestService;
    private final RoleHelper roleHelper;
    private final List<AbstractServiceRestClient> serviceRestClients;
    private final EntityConverter converter;
    private final EventPublisher eventPublisher;
    @Value("${organizationmanager.kafka.topic.space-deleted:space-deleted}")
    private String deletedTopic;
    private boolean kafkaEnabled;

    public OrganizationManagerService(OrganizationService orgaService, SpaceService spaceService, RoleService roleService, AuthService authService,
                                      UserService userService, UserRequestService userRequestService, RoleHelper roleHelper, List<AbstractServiceRestClient> serviceRestClients,
                                      EntityConverter converter, EventPublisher eventPublisher) {
        this.orgaService = orgaService;
        this.spaceService = spaceService;
        this.roleService = roleService;
        this.authService = authService;
        this.userService = userService;
        this.userRequestService = userRequestService;
        this.roleHelper = roleHelper;
        this.serviceRestClients = serviceRestClients;
        this.converter = converter;
        this.eventPublisher = eventPublisher;
    }

    @Autowired
    public void setKafkaEnabled(@Value("${organizationmanager.kafka.enabled:true}") boolean kafkaEnabled) {
        this.kafkaEnabled = kafkaEnabled;
    }

    // ******************************************************
    //             Organization Management
    // ******************************************************

    /**
     * Creates a new organization by
     * - storing the organization in the database
     * - creating keycloak roles for the organization
     * - creating organization contexts for every service client in parallel.
     * <p>
     * If any of the tasks fail, the operation is rolled back by deleting the organization contexts and the persisted object.
     * <p>
     * If all tasks complete successfully, the method returns the persisted Organization object.
     *
     * @param orgDTO the DTO representing the organization to be created
     * @return the newly created organization entity
     * @throws OrganizationmanagerException thrown on error
     */
    public Organization createOrganization(OrganizationCreateDTO orgDTO) throws OrganizationmanagerException {
        Organization org = converter.convertToEntity(orgDTO, Organization.class);

        // 1. set owner
        org.setOwners(List.of(getSubject()));

        // 2. create organization entity
        Organization orgPersisted = orgaService.createOrganizationEntity(org);

        // 3. create organization contexts
        // for every service client a task is scheduled and run in parallel
        // if any of the tasks fail, the operation is rolled back (best effort)
        ExecutorService executorService = Executors.newFixedThreadPool(serviceRestClients.size());
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executorService);
        List<Callable<Void>> tasks = new ArrayList<>();

        var securityContext = SecurityContextHolder.getContext();
        try {
            for (var client : serviceRestClients) {
                Callable<Void> task = () -> {
                    SecurityContextHolder.setContext(securityContext); // need to set this for the token to be available in new thread
                    client.createOrganizationContext(orgPersisted);
                    return null;
                };
                tasks.add(task);
                completionService.submit(task);
            }
            for (var ignored : tasks) {
                completionService.take().get();
            }
        } catch (InterruptedException e) {
            LOG.error("completion service interrupted.");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // rollback: delete context and persisted object
            deleteOrganizationContextsBestEffort(orgPersisted);
            orgaService.deleteOrganizationEntity(orgPersisted);
            throw new OrganizationmanagerException(DOWNSTREAM_ERROR, e.getMessage());
        } finally {
            executorService.shutdown();
        }

        return orgPersisted;
    }


    /**
     * Updates an organization by updating the organization entity in the database and updating organization contexts
     * for every service client in parallel. The update operation is considered best effort. If any of the tasks fail,
     * the state may be inconsistent.
     * <p>
     * The updated organization entity is returned if the the operation was successful.
     *
     * @param update    the organization to be updated
     * @param authModel the authentication model used for authorization
     * @return the updated organization entity
     * @throws OrganizationmanagerException thrown if an error occurs
     */
    public Organization updateOrganization(Organization update, AuthenticationModel authModel) throws OrganizationmanagerException {
        // 1. update organization entity
        // ensure owners are not updated
        Organization original = getOrgaAdminOrOwner(authModel, update.getId());
        update.setOwners(original.getOwners());
        // update entity
        Organization orgPersisted = orgaService.updateOrganizationEntity(update, authModel);

        // 2. create organization contexts
        // for every service client a task is scheduled and run in parallel
        // the update operation is considered best effort. If any fail, the state will can be in an inconsistent state
        ExecutorService executorService = Executors.newFixedThreadPool(serviceRestClients.size());
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executorService);
        List<Callable<Void>> tasks = new ArrayList<>();

        var securityContext = SecurityContextHolder.getContext();
        try {
            for (var client : serviceRestClients) {
                Callable<Void> task = () -> {
                    SecurityContextHolder.setContext(securityContext);
                    client.updateOrganizationContext(orgPersisted);
                    return null;
                };
                tasks.add(task);
                completionService.submit(task);
            }
            for (var ignored : tasks) {
                completionService.take().get();
            }
        } catch (InterruptedException e) {
            LOG.error("completion service interrupted.");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // simply return the error
            throw new OrganizationmanagerException(DOWNSTREAM_ERROR, e.getMessage());
        } finally {
            executorService.shutdown();
        }

        return orgPersisted;

    }

    /**
     * Deletes an organization by deleting the organization contexts and the organization entity from the database.
     *
     * @param orgaName  the name of the organization to be deleted
     * @param authModel the authentication model used for authorization
     * @throws OrganizationmanagerException if there is a downstream error
     */
    public void deleteOrganization(String orgaName, AuthenticationModel authModel) throws OrganizationmanagerException {
        if (!authModel.isSuperuser()) {
            AuditLogger.error(LOG, "missing permission to delete organization {}", authModel.getToken(), orgaName);
            throw new OrganizationmanagerException(FORBIDDEN, "can only be deleted by superuser");
        }
        Organization org = orgaService.getOrganizationByName(orgaName, authModel);

        List<Space> spaces = spaceService.getSpaces(authModel, org.getId(), AuthConfiguration.GET);
        for (var space : spaces) {
            deleteSpace(authModel, org.getId(), space.getId());
        }

        deleteOrganizationContextsBestEffort(org);

        orgaService.deleteOrganizationEntity(org);
        AuditLogger.error(LOG, "organization {} and all included spaces successfully deleted",
                authModel.getToken(), org.getId());
    }

    /**
     * Deletes the organization context for the specified organization on all registered service REST clients,
     * making a best effort to complete the deletion on all clients. The method creates a new thread pool with a
     * fixed number of threads equal to the number of service REST clients, and submits a deletion task to each
     * thread. The security context of the current thread is set on each task to ensure that the appropriate
     * security token is available for the deletion request. The method waits for all tasks to complete before
     * shutting down the thread pool.
     *
     * @param org The organization for which to delete the context on all service REST clients.
     */
    private void deleteOrganizationContextsBestEffort(Organization org) {
        ExecutorService executorService = Executors.newFixedThreadPool(serviceRestClients.size());
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executorService);
        List<Callable<Void>> tasks = new ArrayList<>();
        var securityContext = SecurityContextHolder.getContext();
        try {
            for (var client : serviceRestClients) {
                Callable<Void> task = () -> {
                    SecurityContextHolder.setContext(securityContext);
                    client.deleteOrganizationContextImpl(org);
                    return null;
                };
                tasks.add(task);
                completionService.submit(task);
            }
            for (var ignored : tasks) {
                completionService.take().get();
            }
        } catch (InterruptedException e) {
            LOG.error("completion service interrupted.");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // do nothing
        } finally {
            executorService.shutdown();
        }
    }

    // ******************************************************
    //             Space Management
    // ******************************************************

    /**
     * Create space including dedicated roles
     *
     * @param authModel the authenticationmodel
     * @param orgaId    the organization-id
     * @param space     the space
     * @return the created space
     * @throws OrganizationmanagerException thrown on errors
     */
    public Space createSpace(AuthenticationModel authModel, long orgaId, Space space) throws OrganizationmanagerException {
        // 1. check rights of caller to create space
        Organization orgaAdmin = getOrgaAdminOrOwner(authModel, orgaId);

        // 2. set current owner
        space.setOwners(List.of(getSubject()));

        // 3. create space entity
        Space spaceCreated = spaceService.createSpaceEntity(orgaAdmin, space);

        // 4. create organization contexts
        // for every service client a task is scheduled and run in parallel
        // if any of the tasks fail, the operation is rolled back (best effort)
        ExecutorService executorService = Executors.newFixedThreadPool(serviceRestClients.size());
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executorService);
        List<Callable<Void>> tasks = new ArrayList<>();

        var securityContext = SecurityContextHolder.getContext();
        try {
            for (var client : serviceRestClients) {
                Callable<Void> task = () -> {
                    SecurityContextHolder.setContext(securityContext);
                    client.createSpaceContext(orgaAdmin, spaceCreated);
                    return null;
                };
                tasks.add(task);
                completionService.submit(task);
            }
            for (var ignored : tasks) {
                completionService.take().get();
            }
        } catch (Exception e) {
            // rollback: delete context and persisted object
            deleteSpaceContextsBestEffort(orgaAdmin, spaceCreated);
            spaceService.deleteSpaceEntity(spaceCreated);
            throw new OrganizationmanagerException(DOWNSTREAM_ERROR, e.getMessage());
        } finally {
            executorService.shutdown();
        }

        // 5. assign all space-roles to owner
        String ownerId = getSubject();
        userService.setUserRoles(orgaAdmin, spaceCreated, Arrays.stream(RoleHelper.SpaceScopeRole.values()).toList(), ownerId);
        AuditLogger.info(LOG, "successfullly created space {} in organization {}", authModel.getToken(),
                spaceCreated.getId(), orgaId);
        return spaceCreated;
    }

    /**
     * Updates a space (skip renaming the role as renaming a space would return a
     * * RENAMING_OBJECT_FORBIDDEN-Exception)
     *
     * @param authModel then authenticationmodel
     * @param orgaId    the organization-id
     * @param update    the space
     * @return the updated space
     * @throws OrganizationmanagerException thrown on errors
     */
    public Space updateSpace(AuthenticationModel authModel, long orgaId, Space update) throws OrganizationmanagerException {
        // 1. check rights of caller to create space
        // check if user has permissions to organization
        Organization organization = orgaService.getOrganization(orgaId, authModel);

        // 2. update space entity
        // ensure owners are not updated
        Space original = spaceService.getSpaceById(authModel, orgaId, update.getId());
        update.setOwners(original.getOwners());
        // update entity
        Space updated = spaceService.updateSpaceEntity(authModel, organization, update);

        // 3. create organization contexts
        // for every service client a task is scheduled and run in parallel
        // if any of the tasks fail, the operation is rolled back (best effort)
        ExecutorService executorService = Executors.newFixedThreadPool(serviceRestClients.size());
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executorService);
        List<Callable<Void>> tasks = new ArrayList<>();

        var securityContext = SecurityContextHolder.getContext();
        try {
            for (var client : serviceRestClients) {
                Callable<Void> task = () -> {
                    SecurityContextHolder.setContext(securityContext);
                    client.updateSpaceContext(organization, updated);
                    return null;
                };
                tasks.add(task);
                completionService.submit(task);
            }
            for (var ignored : tasks) {
                completionService.take().get();
            }
        } catch (InterruptedException e) {
            LOG.error("completion service interrupted.");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // simply return the error
            throw new OrganizationmanagerException(DOWNSTREAM_ERROR, e.getMessage());
        } finally {
            executorService.shutdown();
        }
        AuditLogger.info(LOG, "successfullly updated space {} in organization {}, update {}", authModel.getToken(),
                update.getId(), orgaId, update);
        return updated;
    }

    /**
     * Delete a space including dedicated roles
     *
     * @param authModel the authenticationmodel
     * @param orgaId    the organization-id
     * @param spaceId   the space-id
     * @return success
     * @throws OrganizationmanagerException thrown on errors
     */
    public boolean deleteSpace(AuthenticationModel authModel, long orgaId, long spaceId) throws OrganizationmanagerException {
        if (!authModel.isSuperuser()) {
            AuditLogger.error(LOG, "has no permission to delete space {} in organization {}",
                    authModel.getToken(), spaceId, orgaId);
            throw new OrganizationmanagerException(FORBIDDEN, "can only be deleted by superuser");
        }
        // check if user has admin-access to organization
        Organization orga = orgaService.getOrganization(orgaId, authModel);
        Space space = spaceService.getSpaceById(authModel, orgaId, spaceId);

        deleteSpaceContextsBestEffort(orga, space);

        if (kafkaEnabled && orga != null) {
            try {
                // atm only workflowmanager is listening
                sendEvent(orga, space, deletedTopic);
            } catch (IOException e) {
                LOG.debug(e.getMessage());
            }
        }

        spaceService.deleteSpaceEntity(space);
        AuditLogger.info(LOG, "successfully deleted space {} in organization {}",
                authModel.getToken(), spaceId, orgaId);
        return true;
    }

    /**
     * Deletes the space context for the specified space and organization on all registered service REST clients,
     * making a best effort to complete the deletion on all clients. The method creates a new thread pool with a
     * fixed number of threads equal to the number of service REST clients, and submits a deletion task to each
     * thread. The security context of the current thread is set on each task to ensure that the appropriate
     * security token is available for the deletion request. The method waits for all tasks to complete before
     * shutting down the thread pool.
     *
     * @param org The organization to which the space belongs.
     * @param spc The space for which to delete the context on all service REST clients.
     */
    private void deleteSpaceContextsBestEffort(Organization org, Space spc) {
        ExecutorService executorService = Executors.newFixedThreadPool(serviceRestClients.size());
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executorService);
        List<Callable<Void>> tasks = new ArrayList<>();
        var securityContext = SecurityContextHolder.getContext();
        try {
            for (var client : serviceRestClients) {
                Callable<Void> task = () -> {
                    SecurityContextHolder.setContext(securityContext);
                    client.deleteSpaceContext(org, spc);
                    return null;
                };
                tasks.add(task);
                completionService.submit(task);
            }
            for (var ignored : tasks) {
                completionService.take().get();
            }
        } catch (InterruptedException e) {
            LOG.error("completion service interrupted.");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // do nothing
        } finally {
            executorService.shutdown();
        }
    }

    // ******************************************************
    //                 USER-MANAGEMENT
    // ******************************************************

    /**
     * List users with permissions in the given organization
     *
     * @param authModel the authenticationmodel
     * @param orgaId    the organization-id
     * @return the users
     * @throws OrganizationmanagerException thrown on errors
     */
    public Set<OrganizationUserDTO> listUsers(AuthenticationModel authModel, long orgaId) throws OrganizationmanagerException {
        Organization orga = getOrgaAdminOrOwner(authModel, orgaId);
        return userService.getUsers(orga);
    }

    /**
     * List users with permissions in the given space
     *
     * @param authModel the authenticationmodel
     * @param orgaId    the organization-id
     * @param spaceId   the space-id
     * @return the users
     * @throws OrganizationmanagerException thrown on errors
     */
    public Set<SpaceUserDTO> listUsers(AuthenticationModel authModel, long orgaId, long spaceId) throws OrganizationmanagerException {
        Organization orga = orgaService.getOrganization(orgaId, authModel);
        Space space = spaceService.getSpaceById(authModel, orgaId, spaceId);
        // orga-admins, orga-owner and space-owner may list users and their permissions in space
        if (!isAdminOrOwner(authModel, orga, space)) {
            throw new OrganizationmanagerException(FORBIDDEN);
        }
        return userService.getUsers(orga, space);
    }

    /**
     * Assignes the given organization-scoped roles to the user (queried by user-id)
     *
     * @param authenticationModel The AuthenticationModel
     * @param orgaId              the organization-id
     * @param userId              the user-id
     * @param roleScopes          the OrganizationScopeRoles
     * @return The UserDTO
     * @throws OrganizationmanagerException thrown on errors
     */
    public UserDTO setRoles(AuthenticationModel authenticationModel, long orgaId, String userId, List<RoleHelper.OrganizationScopeRole> roleScopes) throws OrganizationmanagerException {
        UserDTO user = userService.getUser(userId);
        return setRoles(authenticationModel, orgaId, user, roleScopes);
    }

    /**
     * Assignes the given organization-scoped roles to the user (queried by username)
     *
     * @param authenticationModel The AuthenticationModel
     * @param orgaId              the organization-id
     * @param name                the username
     * @param roleScopes          the OrganizationScopeRoles
     * @return The UserDTO
     * @throws OrganizationmanagerException thrown on errors
     */
    public UserDTO setRolesByName(AuthenticationModel authenticationModel, long orgaId, String name, List<RoleHelper.OrganizationScopeRole> roleScopes) throws OrganizationmanagerException {
        UserDTO user = userService.getUserByName(name);
        return setRoles(authenticationModel, orgaId, user, roleScopes);
    }

    /**
     * Assignes the given organization-scoped roles to the user (queried by email)
     *
     * @param authenticationModel The AuthenticationModel
     * @param orgaId              the organization-id
     * @param email               the email
     * @param roleScopes          the OrganizationScopeRoles
     * @return The UserDTO
     * @throws OrganizationmanagerException thrown on errors
     */
    public UserDTO setRolesByEmail(AuthenticationModel authenticationModel, long orgaId, String email, List<RoleHelper.OrganizationScopeRole> roleScopes) throws OrganizationmanagerException {
        UserDTO user = userService.getUserByEmail(email);
        return setRoles(authenticationModel, orgaId, user, roleScopes);
    }

    private UserDTO setRoles(AuthenticationModel authModel, long orgaId, UserDTO user, List<RoleHelper.OrganizationScopeRole> roleScopes) throws OrganizationmanagerException {
        Organization orga = getOrgaAdminOrOwner(authModel, orgaId);
        userService.setUserRoles(orga, roleScopes, user.getId());
        AuditLogger.info(LOG, "set roles for user {} in organization {} - roleScopes {}", authModel.getToken(),
                user.getId(), orgaId, roleScopes);
        return user;
    }

    /**
     * Assignes the given space-scoped roles to the user (queried by user-id)
     *
     * @param authenticationModel The AuthenticationModel
     * @param orgaId              the organization-id
     * @param userId              the user-id
     * @param roleScopes          the SpaceScopeRoles
     * @return The UserDTO
     * @throws OrganizationmanagerException thrown on errors
     */
    public UserDTO setRoles(AuthenticationModel authenticationModel, long orgaId, long spaceId, String userId, List<RoleHelper.SpaceScopeRole> roleScopes) throws OrganizationmanagerException {
        UserDTO user = userService.getUser(userId);
        return setRoles(authenticationModel, orgaId, spaceId, user, roleScopes);
    }

    /**
     * Assignes the given space-scoped roles to the user (queried by username)
     *
     * @param authenticationModel The AuthenticationModel
     * @param orgaId              the organization-id
     * @param name                the username
     * @param roleScopes          the SpaceScopeRoles
     * @return The UserDTO
     * @throws OrganizationmanagerException thrown on errors
     */
    public UserDTO setRolesByName(AuthenticationModel authenticationModel, long orgaId, long spaceId, String name,
                                  List<RoleHelper.SpaceScopeRole> roleScopes) throws OrganizationmanagerException {
        UserDTO user = userService.getUserByName(name);
        return setRoles(authenticationModel, orgaId, spaceId, user, roleScopes);
    }

    /**
     * Assignes the given space-scoped roles to the user (queried by email)
     *
     * @param authenticationModel The AuthenticationModel
     * @param orgaId              the organization-id
     * @param email               the email
     * @param roleScopes          the SpaceScopeRoles
     * @return The UserDTO
     * @throws OrganizationmanagerException thrown on errors
     */
    public UserDTO setRolesByEmail(AuthenticationModel authenticationModel, long orgaId, long spaceId, String email,
                                   List<RoleHelper.SpaceScopeRole> roleScopes) throws OrganizationmanagerException {
        UserDTO user = userService.getUserByEmail(email);
        return setRoles(authenticationModel, orgaId, spaceId, user, roleScopes);
    }

    private UserDTO setRoles(AuthenticationModel authModel, long orgaId, long spaceId, UserDTO user, List<RoleHelper.SpaceScopeRole> roleScopes) throws OrganizationmanagerException {
        Organization orga = orgaService.getOrganization(orgaId, authModel);
        Space space = spaceService.getSpaceById(authModel, orgaId, spaceId);
        // orga-admins, orga-owner and space-owner may edit user-permissions in space
        if (!isAdminOrOwner(authModel, orga, space)) {
            AuditLogger.error(LOG, "insufficient permission to set roles in organization {} and space {}!",
                    authModel.getToken(), orgaId, spaceId);
            throw new OrganizationmanagerException(FORBIDDEN);
        }
        userService.setUserRoles(orga, space, roleScopes, user.getId());
        AuditLogger.info(LOG, "set roles for user {} in organization {} for space {} - roleScopes {}",
                authModel.getToken(), user.getId(), orgaId, spaceId, roleScopes);
        return user;
    }

    /**
     * Assign an organization-role to the given user
     *
     * @param authModel the AuthenticationModel
     * @param orgaId    the organization-id
     * @param userId    the user-id
     * @param roleScope the OrganizationScopeRole
     * @throws OrganizationmanagerException thrown on errors
     */
    private void assignRole(AuthenticationModel authModel, long orgaId, String userId, RoleHelper.OrganizationScopeRole roleScope) throws OrganizationmanagerException {
        Organization orga = getOrgaAdminOrOwner(authModel, orgaId);
        UserDTO user = userService.getUser(userId);
        userService.assignUserToRole(orga, roleScope, user);
        AuditLogger.info(LOG, "successfully assigned roleScope {} to user {} in organization {}",
                authModel.getToken(), roleScope, userId, orgaId);
        LOG.info("{} assigned {}-permission for orga {} to user {}", getSubject(), roleScope.name(), orgaId, userId);
    }


    /**
     * Assign a space-role to the given user
     *
     * @param authModel the authenticationmodel
     * @param orgaId    the organization-id
     * @param spaceId   the space-id
     * @param userId    the user-id
     * @param roleScope the SpaceScopeRole
     * @throws OrganizationmanagerException thrown on errors
     */
    private void assignRole(AuthenticationModel authModel, long orgaId, long spaceId, String userId, RoleHelper.SpaceScopeRole roleScope) throws OrganizationmanagerException {
        Organization orga = orgaService.getOrganization(orgaId, authModel);
        Space space = spaceService.getSpaceById(authModel, orgaId, spaceId);
        UserDTO user = userService.getUser(userId);
        userService.assignUserToRole(orga, space, roleScope, user);
        AuditLogger.info(LOG, "successfully assigned role {} to user {} for organization {} and space {}",
                authModel.getToken(), roleScope, userId, orgaId, spaceId);
        LOG.info("{} assigned {}-permission for space {} in orga {} to user {}", getSubject(), roleScope.name(), spaceId, orgaId, userId);
    }

    // ******************************************************
    //              ORGA-USERREQUEST-MANAGEMENT
    // ******************************************************

    /**
     * List all userrequests to the given organization
     *
     * @param authModel the AuthenticationModel
     * @param orgaId    the organization-id
     * @return all userrequests for organization
     * @throws OrganizationmanagerException thrown on errors
     */
    public List<OrganizationUserRequest> listOrganizationRequests(AuthenticationModel authModel, long orgaId) throws OrganizationmanagerException {
        Organization orga = getOrgaAdmin(authModel, orgaId);
        return userRequestService.listUserRequests(orga);
    }

    /**
     * List all userrequests to the given organization by state
     *
     * @param authModel the AuthenticationModel
     * @param orgaId    the organization-id
     * @param state     the UserRequestState
     * @return all userrequests for organization
     * @throws OrganizationmanagerException thrown on errors
     */
    public List<OrganizationUserRequest> listOrganizationRequests(AuthenticationModel authModel, long orgaId, UserRequestState state) throws OrganizationmanagerException {
        Organization orga = getOrgaAdmin(authModel, orgaId);
        return userRequestService.listUserRequests(orga, state);
    }

    /**
     * Create an userrequest to the given organization
     *
     * @param authModel   the authenticationmodel
     * @param orgaId      the organization-id
     * @param userRequest the OrganizationUserRequest
     * @return the created OrganizationUserRequest
     * @throws OrganizationmanagerException thrown on errors
     */
    public OrganizationUserRequest createOrganizationRequest(AuthenticationModel authModel, long orgaId, OrganizationUserRequest userRequest) throws OrganizationmanagerException {
        // temporarily set superuser-rights so checking for orga-existence is possible
        AuthenticationModel tempAuthModel = new AuthenticationModel();
        tempAuthModel.setSuperuser(true);
        Organization orga = orgaService.getOrganization(orgaId, tempAuthModel);
        if (orga == null) {
            throw new OrganizationmanagerException(GET_SINGLE_NOT_FOUND);
        }
        String roleName = roleHelper.buildOrganizationRole(orga, userRequest.getRole());
        checkRoleExists(roleName);
        return userRequestService.createUserRequest(userRequest);
    }

    /**
     * Accept organization-userrequest
     *
     * @param authModel the AuthenticationModel
     * @param orgaId    the organization-id
     * @param id        the id of the userrequest
     * @return the updated userrequest
     * @throws OrganizationmanagerException thrown on errors
     */
    public OrganizationUserRequest acceptOrganizationRequest(AuthenticationModel authModel, long orgaId, long id) throws OrganizationmanagerException {
        Organization orga = getOrgaAdmin(authModel, orgaId);
        OrganizationUserRequest organizationUserRequest = userRequestService.acceptUserRequest(orga, id);
        assignRole(authModel, orgaId, organizationUserRequest.getUserId(), organizationUserRequest.getRole());
        AuditLogger.info(LOG, "accepted a request to organization {} for request id {} and user id {}",
                authModel.getToken(), orgaId, id, organizationUserRequest.getUserId());
        return organizationUserRequest;
    }

    /**
     * Decline organization-userrequest
     *
     * @param authModel the authenticationmodel
     * @param orgaId    the organization-id
     * @param id        the id of the userrequest
     * @return the updated userrequest
     * @throws OrganizationmanagerException thrown on errors
     */
    public OrganizationUserRequest declineOrganizationRequest(AuthenticationModel authModel, long orgaId, long id) throws OrganizationmanagerException {
        Organization orga = getOrgaAdmin(authModel, orgaId);
        return userRequestService.declineUserRequest(orga, id);
    }

    // ******************************************************
    //              SPACE-USERREQUEST-MANAGEMENT
    // ******************************************************

    /**
     * List all userrequests to the given space
     *
     * @param authModel the AuthenticationModel
     * @param orgaId    the organization-id
     * @param spaceId   the space-id
     * @return all userrequests for organization
     * @throws OrganizationmanagerException thrown on errors
     */
    public List<SpaceUserRequest> listSpaceRequests(AuthenticationModel authModel, long orgaId, long spaceId) throws OrganizationmanagerException {
        Organization orga = orgaService.getOrganization(orgaId, authModel);
        Space space = spaceService.getSpaceById(authModel, orgaId, spaceId);
        // orga-admins, orga-owner and space-owner may list userrequests and their permissions in space
        if (!isAdminOrOwner(authModel, orga, space)) {
            throw new OrganizationmanagerException(FORBIDDEN);
        }
        return userRequestService.listUserRequests(orga, space);
    }

    /**
     * List all userrequests to the given space matching the given state
     *
     * @param authModel the AuthenticationModel
     * @param orgaId    the organization-id
     * @param spaceId   the space-id
     * @param state     the UserRequestState
     * @return all userrequests for organization
     * @throws OrganizationmanagerException thrown on errors
     */
    public List<SpaceUserRequest> listSpaceRequests(AuthenticationModel authModel, long orgaId, long spaceId, UserRequestState state) throws OrganizationmanagerException {
        Organization orga = getOrgaAdmin(authModel, orgaId);
        Space space = spaceService.getSpaceById(authModel, orgaId, spaceId);
        return userRequestService.listUserRequests(orga, space, state);
    }

    /**
     * Create an userrequest to the given space
     *
     * @param authModel   the authenticationmodel
     * @param orgaId      the organization-id
     * @param spaceId     the space-id
     * @param userRequest the SpaceUserRequest
     * @return the created SpaceUserRequest
     * @throws OrganizationmanagerException thrown on errors
     */
    public SpaceUserRequest createSpaceRequest(AuthenticationModel authModel, long orgaId, long spaceId, SpaceUserRequest userRequest) throws OrganizationmanagerException {
        // temporarily set superuser-rights so checking for orga-existence is possible
        AuthenticationModel tempAuthModel = new AuthenticationModel();
        tempAuthModel.setSuperuser(true);
        Organization orga = orgaService.getOrganization(orgaId, tempAuthModel);
        if (orga == null) {
            throw new OrganizationmanagerException(GET_SINGLE_NOT_FOUND);
        }
        Space space = spaceService.getSpaceById(tempAuthModel, orgaId, spaceId);
        if (space == null) {
            throw new OrganizationmanagerException(GET_SINGLE_NOT_FOUND);
        }
        String roleName = roleHelper.buildSpaceRole(orga, space, userRequest.getRole());
        checkRoleExists(roleName);
        return userRequestService.createUserRequest(userRequest);
    }

    private void checkRoleExists(String roleName) throws OrganizationmanagerException {
        String accessToken = authService.getSAaccessToken();
        RoleDTO[] roles = roleService.getRoles(accessToken);
        Optional<RoleDTO> role = Stream.of(roles).filter(r -> r.getName().equalsIgnoreCase(roleName)).findFirst();
        if (role.isPresent()) {
            throw new OrganizationmanagerException(UNABLE_GET_ROLE, roleName);
        }

    }

    /**
     * Accept space-userrequest
     *
     * @param authModel the AuthenticationModel
     * @param orgaId    the organization-id
     * @param spaceId   the space-id
     * @param id        the id of the userrequest
     * @return the updated userrequest
     * @throws OrganizationmanagerException thrown on errors
     */
    public SpaceUserRequest acceptSpaceRequest(AuthenticationModel authModel, long orgaId, long spaceId, long id) throws OrganizationmanagerException {
        Organization orga = orgaService.getOrganization(orgaId, authModel);
        Space space = spaceService.getSpaceById(authModel, orgaId, spaceId);
        // orga-admins, orga-owner and space-owner may accept userrequests
        if (!isAdminOrOwner(authModel, orga, space)) {
            AuditLogger.error(LOG, "is not allowed to accept space requests for organization {} and space {}",
                    authModel.getToken(), orgaId, spaceId);
            throw new OrganizationmanagerException(FORBIDDEN);
        }
        SpaceUserRequest spaceUserRequest = userRequestService.acceptUserRequest(orga, space, id);
        AuditLogger.info(LOG, "successfully accepted request for space request id {} in organization {} and " +
                "space {}", authModel.getToken(), id, orgaId, spaceId);
        assignRole(authModel, orgaId, spaceId, spaceUserRequest.getUserId(), spaceUserRequest.getRole());
        return spaceUserRequest;
    }

    /**
     * Decline space-userrequest
     *
     * @param authModel the authenticationmodel
     * @param orgaId    the organization-id
     * @param spaceId   the space-id
     * @param id        the id of the userrequest
     * @return the updated userrequest
     * @throws OrganizationmanagerException thrown on errors
     */
    public SpaceUserRequest declineSpaceRequest(AuthenticationModel authModel, long orgaId, long spaceId, long id) throws OrganizationmanagerException {
        Organization orga = getOrgaAdmin(authModel, orgaId);
        Space space = spaceService.getSpaceById(authModel, orgaId, spaceId);
        AuditLogger.info(LOG, "declined request for space request id {} in organization {} and " +
                "space {}", authModel.getToken(), id, orgaId, spaceId);
        return userRequestService.declineUserRequest(orga, space, id);
    }

    private Organization getOrgaAdmin(AuthenticationModel authModel, long orgaId) throws OrganizationmanagerException {
        Organization orga = orgaService.getOrganization(orgaId, authModel);
        if (!isAdmin(authModel, orga)) {
            AuditLogger.error(LOG, "has no admin rights for organization {}", authModel.getToken(), orgaId);
            throw new OrganizationmanagerException(FORBIDDEN);
        }
        return orga;
    }

    /**
     * Checks if current user is superuser, orga-admin or orga-owner
     *
     * @param authModel The authenticationmodel
     * @param orgaId    the organization-id
     * @return the organization if user is superuser, orga-admin or -owner
     * @throws OrganizationmanagerException thrown in case of an error (e.g. permission denied)
     */
    private Organization getOrgaAdminOrOwner(AuthenticationModel authModel, long orgaId) throws OrganizationmanagerException {
        Organization orga = orgaService.getOrganization(orgaId, authModel);
        if (!isAdminOrOwner(authModel, orga)) {
            AuditLogger.error(LOG, "is neither admin nor owner of organization {}!", authModel.getToken(), orgaId);
            throw new OrganizationmanagerException(FORBIDDEN);
        }
        return orga;
    }

    public String getUserName(String userId) throws OrganizationmanagerException {
        UserDTO user = userService.getUserView(userId);
        return format("%s %s", user.getFirstName(), user.getLastName());
    }

    private void sendEvent(Organization organization, Space space, String topic) throws IOException {
        SpaceBase serial = converter.convertToSerializable(space);
        serial.setOrganizationName(organization.getName());
        SpaceDTO dto = SpaceDTO.newBuilder().setSpace(serial).build();
        ByteBuffer buffer = dto.toByteBuffer();
        eventPublisher.sendMessage(buffer, topic);
    }

    public List<String> getSpaceNamesWithOrganizationPrefix(AuthenticationModel authenticationModel, AuthConfiguration authConfiguration) throws RestClientException, OrganizationmanagerException {
        String[] allowedOrganizations = authenticationModel.getOrganizationsByPermission(authConfiguration);
        List<Organization> orgas = orgaService.getAllOrganizations(authenticationModel, allowedOrganizations, authConfiguration);
        List<String> spaceNames = new ArrayList<>();
        for (Organization orga : orgas) {
            Long orgId = orga.getId();
            String organizationName = orga.getName();
            List<Space> spaces = spaceService.getSpaces(authenticationModel, orgId, authConfiguration);
            spaceNames.addAll(spaces.stream().map(
                    space -> organizationName + "_" + space.getName()).toList());
        }
        return spaceNames;
    }
}
