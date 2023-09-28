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
import com.efs.sdk.organizationmanager.core.auth.model.OrganizationUserDTO;
import com.efs.sdk.organizationmanager.core.auth.model.RoleDTO;
import com.efs.sdk.organizationmanager.core.auth.model.SpaceUserDTO;
import com.efs.sdk.organizationmanager.core.auth.model.UserDTO;
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.core.space.model.Space;
import com.efs.sdk.organizationmanager.helper.RoleHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.efs.sdk.organizationmanager.commons.OrganizationmanagerException.ORGANIZATIONMANAGER_ERROR.*;
import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_JSON;


/**
 * Service for handling usermanagement
 */
@Service
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private final RestTemplate restTemplate;
    private final AuthService authService;
    private final RoleHelper roleHelper;
    private final RoleService roleService;
    private final String realmEndpoint;

    public UserService(RestTemplate restTemplate, AuthService authService, RoleHelper roleHelper, RoleService roleService, @Value("${organizationmanager.auth" +
            ".realm-endpoint}") String realmEndpoint) {
        this.restTemplate = restTemplate;
        this.authService = authService;
        this.roleHelper = roleHelper;
        this.roleService = roleService;
        this.realmEndpoint = realmEndpoint;
    }

    /**
     * Get Users with permissions in organization
     *
     * @param orga the organization
     * @return users with permissions in organization
     * @throws OrganizationmanagerException thrown on errors
     */
    public Set<OrganizationUserDTO> getUsers(Organization orga) throws OrganizationmanagerException {
        String accessToken = authService.getSAaccessToken();

        // get users by organization-scope
        Map<RoleHelper.OrganizationScopeRole, List<OrganizationUserDTO>> usersByScope = new EnumMap<>(RoleHelper.OrganizationScopeRole.class);
        ParameterizedTypeReference<List<OrganizationUserDTO>> responseType = new ParameterizedTypeReference<>() {
        };
        for (RoleHelper.OrganizationScopeRole roleScope : RoleHelper.OrganizationScopeRole.values()) {
            String roleName = roleHelper.buildOrganizationRole(orga, roleScope);
            usersByScope.put(roleScope, getUsers(accessToken, roleName, responseType));
        }

        // reorganize users so that they have their permissions
        Set<OrganizationUserDTO> orgaUsers = usersByScope.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        orgaUsers.forEach(orgaUser -> usersByScope.entrySet().stream().filter(userRoles -> userRoles.getValue().contains(orgaUser)).forEach(userRoles -> orgaUser.addPermission(userRoles.getKey())));
        return orgaUsers;
    }

    /**
     * Gets the user with the given id
     *
     * @param userId the user's id
     * @return the UserDTO
     * @throws OrganizationmanagerException thrown on errors
     */
    public UserDTO getUser(String userId) throws OrganizationmanagerException {
        String accessToken = authService.getSAaccessToken();
        return getUser(accessToken, userId);
    }

    /**
     * Gets the user with the given name
     *
     * @param name the user's name
     * @return the UserDTO
     * @throws OrganizationmanagerException thrown on errors
     */
    public UserDTO getUserByName(String name) throws OrganizationmanagerException {
        String accessToken = authService.getSAaccessToken();
        String url = format("%s/users?username=%s&exact=true", realmEndpoint, name);
        UserDTO[] userDTOS = getUsersFromQueryURL(accessToken, url);
        return getSingleUserFromUsers(userDTOS);
    }

    /**
     * Gets the user with the given name
     *
     * @param email the user's email
     * @return the UserDTO
     * @throws OrganizationmanagerException thrown on errors
     */
    public UserDTO getUserByEmail(String email) throws OrganizationmanagerException {
        String accessToken = authService.getSAaccessToken();
        String url = format("%s/users?email=%s&exact=true", realmEndpoint, email);
        UserDTO[] userDTOS = getUsersFromQueryURL(accessToken, url);
        return getSingleUserFromUsers(userDTOS);
    }

    /**
     * Get Users with permissions in space
     *
     * @param orga  the organization
     * @param space the space
     * @return users with permissions in space
     * @throws OrganizationmanagerException thrown on errors
     */
    public Set<SpaceUserDTO> getUsers(Organization orga, Space space) throws OrganizationmanagerException {
        String accessToken = authService.getSAaccessToken();

        // get users by space-scope
        Map<RoleHelper.SpaceScopeRole, List<SpaceUserDTO>> usersByScope = new EnumMap<>(RoleHelper.SpaceScopeRole.class);
        ParameterizedTypeReference<List<SpaceUserDTO>> responseType = new ParameterizedTypeReference<>() {
        };
        for (RoleHelper.SpaceScopeRole roleScope : RoleHelper.SpaceScopeRole.values()) {
            String roleName = roleHelper.buildSpaceRole(orga, space, roleScope);
            usersByScope.put(roleScope, getUsers(accessToken, roleName, responseType));
        }

        // reorganize users so that they have their permissions
        Set<SpaceUserDTO> spaceUsers = usersByScope.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        spaceUsers.forEach(spaceUser -> usersByScope.entrySet().stream().filter(userRoles -> userRoles.getValue().contains(spaceUser)).forEach(userRoles -> spaceUser.addPermission(userRoles.getKey())));
        return spaceUsers;
    }

    /**
     * Get users with given role
     *
     * @param accessToken  the access-token
     * @param roleName     the role-name
     * @param responseType the responsetype
     * @return list of users within role
     * @throws OrganizationmanagerException thrown on errors
     */
    private <T extends UserDTO> List<T> getUsers(String accessToken, String roleName, ParameterizedTypeReference<List<T>> responseType) throws OrganizationmanagerException {
        try {

            HttpHeaders headers = getHttpHeaders(accessToken);

            String url = getUserRoleEndpoint(roleName);
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), responseType).getBody();

        } catch (RestClientException e) {
            LOG.error(e.getMessage(), e);
            throw new OrganizationmanagerException(UNABLE_GET_USERS, roleName);
        }
    }


    /**
     * Get single user with given role
     *
     * @param accessToken the access-token
     * @param userId      the user-id
     * @return the user
     */
    private UserDTO getUser(String accessToken, String userId) throws OrganizationmanagerException {
        try {
            HttpHeaders headers = getHttpHeaders(accessToken);
            String url = getUserEndpoint(userId);
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), UserDTO.class).getBody();
        } catch (RestClientException e) {
            LOG.warn(e.getMessage(), e);
            throw new OrganizationmanagerException(UNABLE_GET_USER, userId);
        }
    }

    public UserDTO getUserView(String userId) throws OrganizationmanagerException {
        String accessToken = authService.getSAaccessToken();
        return getUserView(accessToken, userId);
    }

    /**
     * Gets user by user-id - will return a dummy-user ("unknown user") if user is not found
     *
     * @param accessToken the access-token
     * @param userId      the user-id
     * @return the user or a dummy
     */
    private UserDTO getUserView(String accessToken, String userId) {
        try {
            HttpHeaders headers = getHttpHeaders(accessToken);
            String url = getUserEndpoint(userId);
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), UserDTO.class).getBody();
        } catch (RestClientException e) {
            LOG.debug(e.getMessage());
            return dummyUser(userId);
        }
    }


    /**
     * Checks if user exists
     *
     * @param userId the user-id
     * @return whether user exists
     */
    public boolean userExists(String userId) throws OrganizationmanagerException {
        try {
            String accessToken = authService.getSAaccessToken();
            HttpHeaders headers = getHttpHeaders(accessToken);

            String url = getUserEndpoint(userId);
            restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), UserDTO.class).getBody();
            return true;
        } catch (RestClientException e) {
            return false;
        }
    }

    /**
     * Get single users from user array
     *
     * @param userDTOS the list of userDTOs
     * @return user
     */
    private UserDTO getSingleUserFromUsers(UserDTO[] userDTOS) throws OrganizationmanagerException {

        if (userDTOS == null || userDTOS.length != 1) {
            throw new OrganizationmanagerException(UNABLE_GET_USER);
        }

        return userDTOS[0];

    }

    /**
     * Get users with given url
     *
     * @param accessToken the access-token
     * @param url         the url
     * @return users within query
     */
    private UserDTO[] getUsersFromQueryURL(String accessToken, String url) throws RestClientException {

        HttpHeaders headers = getHttpHeaders(accessToken);

        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), UserDTO[].class).getBody();

    }

    private UserDTO dummyUser(String userId) {
        UserDTO dummy = new UserDTO();
        dummy.setFirstName("unknown");
        dummy.setLastName("user");
        dummy.setId(userId);
        return dummy;
    }

    private HttpHeaders getHttpHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    /**
     * Assign user to organization-role
     *
     * @param orga      the organization
     * @param roleScope the role
     * @param user      the user
     * @throws OrganizationmanagerException thrown on errors
     */
    public void assignUserToRole(Organization orga, RoleHelper.OrganizationScopeRole roleScope, UserDTO user) throws OrganizationmanagerException {
        String accessToken = authService.getSAaccessToken();
        String roleName = roleHelper.buildOrganizationRole(orga, roleScope);
        assignRole(accessToken, roleName, user);
    }

    /**
     * Assign user to space-role
     *
     * @param orga      the organization
     * @param space     the space
     * @param roleScope the role
     * @param user      the user
     * @throws OrganizationmanagerException thrown on errors
     */
    public void assignUserToRole(Organization orga, Space space, RoleHelper.SpaceScopeRole roleScope, UserDTO user) throws OrganizationmanagerException {
        String accessToken = authService.getSAaccessToken();
        String roleName = roleHelper.buildSpaceRole(orga, space, roleScope);
        assignRole(accessToken, roleName, user);
    }

    /**
     * Assign user to role
     *
     * @param accessToken the access-token
     * @param roleName    the role
     * @param user        the user
     * @throws OrganizationmanagerException thrown on errors
     */
    private void assignRole(String accessToken, String roleName, UserDTO user) throws OrganizationmanagerException {
        try {

            HttpHeaders headers = getHttpHeaders(accessToken);

            RoleDTO[] roles = roleService.getRoles(accessToken);
            Optional<RoleDTO> role = Stream.of(roles).filter(r -> r.getName().equalsIgnoreCase(roleName)).findFirst();
            if (role.isPresent()) {
                String url = getUserAssignRoleEndpoint(user.getId());
                restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(List.of(role.get()), headers), UserDTO.class);
                return;
            }

            throw new OrganizationmanagerException(UNABLE_GET_ROLE, roleName);
        } catch (RestClientException e) {
            LOG.error(e.getMessage(), e);
            throw new OrganizationmanagerException(UNABLE_ASSIGN_ROLE, roleName);
        }
    }

    /**
     * Withdraw user from role
     *
     * @param orga      the organization
     * @param roleScope the role
     * @param user      the user
     * @throws OrganizationmanagerException thrown on errors
     */
    public void withdrawUserFromRole(Organization orga, RoleHelper.OrganizationScopeRole roleScope, UserDTO user) throws OrganizationmanagerException {
        String accessToken = authService.getSAaccessToken();
        String roleName = roleHelper.buildOrganizationRole(orga, roleScope);
        withdrawRole(accessToken, roleName, user);
    }

    /**
     * Withdraw user from role
     *
     * @param orga      the organization
     * @param space     the space
     * @param roleScope the role
     * @param user      the user
     * @throws OrganizationmanagerException thrown on errors
     */
    private void withdrawUserFromRole(Organization orga, Space space, RoleHelper.SpaceScopeRole roleScope, UserDTO user) throws OrganizationmanagerException {
        String accessToken = authService.getSAaccessToken();
        String roleName = roleHelper.buildSpaceRole(orga, space, roleScope);
        withdrawRole(accessToken, roleName, user);
    }

    /**
     * Withdraw user from role
     *
     * @param accessToken the access-token
     * @param roleName    the role
     * @param user        the user
     * @throws OrganizationmanagerException thrown on errors
     */
    private void withdrawRole(String accessToken, String roleName, UserDTO user) throws OrganizationmanagerException {
        try {

            HttpHeaders headers = getHttpHeaders(accessToken);

            RoleDTO[] roles = roleService.getRoles(accessToken);
            Optional<RoleDTO> role = Stream.of(roles).filter(r -> r.getName().equalsIgnoreCase(roleName)).findFirst();
            if (role.isPresent()) {
                String url = getUserAssignRoleEndpoint(user.getId());
                restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(List.of(role.get()), headers), UserDTO.class);
                return;
            }

            throw new OrganizationmanagerException(UNABLE_GET_ROLE, roleName);
        } catch (RestClientException e) {
            LOG.error(e.getMessage(), e);
            throw new OrganizationmanagerException(UNABLE_WITHDRAW_ROLE, roleName);
        }
    }

    private String getUserAssignRoleEndpoint(String userId) {
        return format("%s/users/%s/role-mappings/realm", realmEndpoint, userId);
    }

    private String getUserRoleEndpoint(String roleName) {
        return format("%s/roles/%s/users", realmEndpoint, roleName);
    }

    private String getUserEndpoint(String userId) {
        return format("%s/users/%s", realmEndpoint, userId);
    }

    public void setUserRoles(Organization orga, List<RoleHelper.OrganizationScopeRole> assignRoles, String userId) throws OrganizationmanagerException {
        UserDTO user = getUser(userId);
        withdrawAllRoles(orga, user);
        assignRoles(orga, assignRoles, user);
    }

    public void assignRoles(Organization orga, List<RoleHelper.OrganizationScopeRole> assignRoles, UserDTO user) throws OrganizationmanagerException {
        for (var role : assignRoles) {
            assignUserToRole(orga, role, user);
        }
    }

    private void withdrawAllRoles(Organization orga, UserDTO user) throws OrganizationmanagerException {
        for (var role : RoleHelper.OrganizationScopeRole.values()) {
            withdrawUserFromRole(orga, role, user);
        }
    }

    public void setUserRoles(Organization orga, Space space, List<RoleHelper.SpaceScopeRole> assignRoles, String userId) throws OrganizationmanagerException {
        UserDTO user = getUser(userId);
        withdrawAllRoles(orga, space, user);
        assignRoles(orga, space, assignRoles, user);
    }

    public void assignRoles(Organization orga, Space space, List<RoleHelper.SpaceScopeRole> assignRoles, String userId) throws OrganizationmanagerException {
        UserDTO user = getUser(userId);
        for (var role : assignRoles) {
            assignUserToRole(orga, space, role, user);
        }
    }

    private void assignRoles(Organization orga, Space space, List<RoleHelper.SpaceScopeRole> assignRoles, UserDTO user) throws OrganizationmanagerException {
        for (var role : assignRoles) {
            assignUserToRole(orga, space, role, user);
        }
    }

    private void withdrawAllRoles(Organization orga, Space space, UserDTO user) throws OrganizationmanagerException {
        for (var role : RoleHelper.SpaceScopeRole.values()) {
            withdrawUserFromRole(orga, space, role, user);
        }
    }
}
