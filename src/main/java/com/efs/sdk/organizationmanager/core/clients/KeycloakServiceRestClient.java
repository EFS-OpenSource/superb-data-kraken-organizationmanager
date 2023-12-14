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
package com.efs.sdk.organizationmanager.core.clients;

import com.efs.sdk.common.domain.model.Capability;
import com.efs.sdk.organizationmanager.commons.OrganizationmanagerException;
import com.efs.sdk.organizationmanager.core.auth.AuthService;
import com.efs.sdk.organizationmanager.core.auth.RoleService;
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.core.space.model.Space;
import com.efs.sdk.organizationmanager.helper.EntityConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * Rest Client class responsible for managing Keycloak context for organizations / spaces
 * This atm means creating keycloak roles.
 */
@Component
public class KeycloakServiceRestClient extends AbstractServiceRestClient {

    private final RoleService roleService;

    public KeycloakServiceRestClient(RestTemplate restTemplate, EntityConverter converter, @Value("") String serviceEndpoint /* not needed */,
            RoleService roleService, AuthService authService) {
        super(restTemplate, converter, serviceEndpoint, authService);
        this.roleService = roleService;
    }

    @Override
    protected boolean hasOrganizationContext(Organization org) {
        return true;
    }

    @Override
    public void createOrganizationContextImpl(Organization org) throws OrganizationmanagerException {
        roleService.createRoles(authService.getSAaccessToken(), org);
    }

    @Override
    public void updateOrganizationContextImpl(Organization org) throws OrganizationmanagerException {
        // creates roles if they do not already exist
        // this can happen when the confidentiality of the organization changes for the first time from PUBLIC to INTERNAL/PRIVATE
        roleService.createRoles(authService.getSAaccessToken(), org);
    }

    @Override
    public void deleteOrganizationContextImpl(Organization org) throws OrganizationmanagerException {
        roleService.deleteRoles(authService.getSAaccessToken(), org);
    }

    @Override
    public void createSpaceContextImpl(Organization org, Space spc) throws OrganizationmanagerException {
        roleService.createRoles(authService.getSAaccessToken(), org, spc);
    }

    @Override
    protected void updateSpaceContextImpl(Organization org, Space original, Space update) throws OrganizationmanagerException {
        // creates roles if they do not already exist
        // this can happen when the confidentiality of the organization changes for the first time
        roleService.createRoles(authService.getSAaccessToken(), org, update);
    }

    @Override
    public void deleteSpaceContextImpl(Organization org, Space spc) throws OrganizationmanagerException {
        roleService.deleteRoles(authService.getSAaccessToken(), org, spc);
    }

    @Override
    protected boolean hasSpaceContext(Space space) {
        return true;
    }

    @Override
    public List<Capability> getCapabilitiesManagedByService() {
        return Collections.emptyList();
    }
}
