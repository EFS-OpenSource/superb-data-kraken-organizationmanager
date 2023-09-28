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
import com.efs.sdk.organizationmanager.core.auth.model.RoleDTO;
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.core.space.model.Space;
import com.efs.sdk.organizationmanager.helper.RoleHelper;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static com.efs.sdk.organizationmanager.commons.OrganizationmanagerException.ORGANIZATIONMANAGER_ERROR.*;
import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
public class RoleService {

    private static final Logger LOG = LoggerFactory.getLogger(RoleService.class);

    private final RoleHelper roleHelper;

    private final String roleEndpoint;

    private final RestTemplate restTemplate;

    /**
     * Constructor.
     *
     * @param restTemplate  The rest-template
     * @param roleHelper    The RoleHelper
     * @param realmEndpoint The Realm Endpoint
     */
    public RoleService(RestTemplate restTemplate, RoleHelper roleHelper,
            @Value("${organizationmanager.auth.realm-endpoint}") String realmEndpoint) {

        this.restTemplate = restTemplate;
        this.roleHelper = roleHelper;
        this.roleEndpoint = format("%s/roles", realmEndpoint);
    }

    /**
     * Create organization-roles ("org_&lt;organization.name&gt;_access" & "org_&lt;organization.name&gt;_admin")
     *
     * @param accessToken  The Access Token
     * @param organization The Organization
     */

    public boolean createRoles(String accessToken, Organization organization) throws OrganizationmanagerException {
        List<String> roles = roleHelper.getRoles(organization);
        for (String role : roles) {
            createRole(accessToken, role);
        }
        return true;
    }

    /**
     * Create space-roles ("&lt;organization.name&gt;_&lt;space.name&gt;_user", "&lt;organization.name&gt;_&lt;space.name&gt;_supplier" & "&lt;organization
     * .name&gt;_&lt;space.name&gt;_trustee")
     *
     * @param accessToken The Access Token
     * @param space       The Space
     */
    public boolean createRoles(String accessToken, Organization orga, Space space) throws OrganizationmanagerException {
        List<String> roles = roleHelper.getRoles(orga, space);
        for (String role : roles) {
            createRole(accessToken, role);
        }
        return true;
    }

    /**
     * Delete organization-roles ("org_&lt;organization.name&gt;_access" & "org_&lt;organization.name&gt;_admin")
     *
     * @param accessToken  The Access Token
     * @param organization The Organization
     */
    public boolean deleteRoles(String accessToken, Organization organization) throws OrganizationmanagerException {
        List<String> roles = roleHelper.getRoles(organization);
        for (String role : roles) {
            deleteRole(accessToken, role);
        }
        return true;
    }

    /**
     * Delete space-roles ("&lt;organization.name&gt;_&lt;space.name&gt;_user", "&lt;organization.name&gt;_&lt;space.name&gt;_supplier" & "&lt;organization
     * .name&gt;_&lt;space.name&gt;_trustee")
     *
     * @param accessToken The Access Token
     * @param space       The Space
     */
    public boolean deleteRoles(String accessToken, Organization orga, Space space) throws OrganizationmanagerException {
        List<String> roles = roleHelper.getRoles(orga, space);
        for (String role : roles) {
            deleteRole(accessToken, role);
        }
        return true;
    }


    private void createRole(String accessToken, String roleName) throws OrganizationmanagerException {
        try {
            if (roleExists(accessToken, roleName)) {
                LOG.warn("Role '{}' already exists - nothing to do!", roleName);
                return;
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            JSONObject jsonObject = new JSONObject();
            jsonObject.appendField("name", roleName);

            HttpEntity<String> request = new HttpEntity<>(jsonObject.toString(), headers);

            restTemplate.postForEntity(roleEndpoint, request, Void.class).getBody();
        } catch (RestClientException e) {
            LOG.error(e.getMessage(), e);
            throw new OrganizationmanagerException(UNABLE_CREATE_ROLE, roleName);
        }
    }

    private void deleteRole(String accessToken, String roleName) throws OrganizationmanagerException {
        try {
            if (!roleExists(accessToken, roleName)) {
                LOG.warn("Role '{}' does not exist - nothing to do!", roleName);
                return;
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<String> request = new HttpEntity<>(null, headers);
            restTemplate.exchange(format("%s/%s", roleEndpoint, roleName), HttpMethod.DELETE, request, Void.class);
        } catch (RestClientException e) {
            LOG.error(e.getMessage(), e);
            throw new OrganizationmanagerException(UNABLE_DELETE_ROLE, roleName);
        }
    }

    private boolean roleExists(String accessToken, String roleName) throws OrganizationmanagerException {
        return Arrays.stream(getRoles(accessToken)).map(RoleDTO::getName).anyMatch(name -> name.equalsIgnoreCase(roleName));
    }

    public RoleDTO[] getRoles(String accessToken) throws OrganizationmanagerException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<String> request = new HttpEntity<>(null, headers);
            return restTemplate.exchange(roleEndpoint, HttpMethod.GET, request, RoleDTO[].class).getBody();
        } catch (RestClientException e) {
            LOG.error(e.getMessage(), e);
            throw new OrganizationmanagerException(UNABLE_GET_ROLE);
        }
    }
}
