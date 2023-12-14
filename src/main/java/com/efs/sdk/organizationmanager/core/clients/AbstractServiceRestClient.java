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
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.core.space.model.Space;
import com.efs.sdk.organizationmanager.helper.EntityConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

public abstract class AbstractServiceRestClient {

    protected final RestTemplate restTemplate;
    protected final EntityConverter converter;
    protected final String serviceEndpoint;
    protected final AuthService authService;

    AbstractServiceRestClient(RestTemplate restTemplate, EntityConverter converter, String serviceEndpoint, AuthService authService) {
        this.restTemplate = restTemplate;
        this.serviceEndpoint = serviceEndpoint;
        this.converter = converter;
        this.authService = authService;
    }

    /******************************************************************************************************************/
    /********************************  Organization Context                            ********************************/
    /******************************************************************************************************************/

    protected abstract boolean hasOrganizationContext(Organization org);

    public void createOrganizationContext(Organization org) throws OrganizationmanagerException {
        if (hasOrganizationContext(org)) {
            createOrganizationContextImpl(org);
        }
    }

    public abstract void createOrganizationContextImpl(Organization org) throws OrganizationmanagerException;

    public void updateOrganizationContext(Organization org) throws OrganizationmanagerException {
        if (hasOrganizationContext(org)) {
            updateOrganizationContextImpl(org);
        }
    }

    public abstract void updateOrganizationContextImpl(Organization org) throws OrganizationmanagerException;

    public void deleteOrganizationContext(Organization org) throws OrganizationmanagerException {
        if (hasOrganizationContext(org)) {
            deleteOrganizationContextImpl(org);
        }
    }

    public abstract void deleteOrganizationContextImpl(Organization org) throws OrganizationmanagerException;

    /******************************************************************************************************************/
    /********************************  Space Context                                   ********************************/
    /******************************************************************************************************************/

    protected boolean hasSpaceContext(Space spc) {
        return !Collections.disjoint(spc.getCapabilities(), getCapabilitiesManagedByService());
    }

    public void createSpaceContext(Organization org, Space spc) throws OrganizationmanagerException {
        if (hasSpaceContext(spc)) {
            createSpaceContextImpl(org, spc);
        }
    }

    public abstract void createSpaceContextImpl(Organization org, Space spc) throws OrganizationmanagerException;

    public void updateSpaceContext(Organization org, Space original, Space update) throws OrganizationmanagerException {
        if (hasSpaceContext(original) || hasSpaceContext(update)) {
            updateSpaceContextImpl(org, original, update);
        }
    }

    protected abstract void updateSpaceContextImpl(Organization org, Space original, Space update) throws OrganizationmanagerException;

    public void deleteSpaceContext(Organization org, Space spc) throws OrganizationmanagerException {
        if (hasSpaceContext(spc)) {
            deleteSpaceContextImpl(org, spc);
        }
    }

    public abstract void deleteSpaceContextImpl(Organization org, Space spc) throws OrganizationmanagerException;


    /**
     * Returns a list of Capability objects representing the capabilities supported by the service.
     *
     * @return a list of Capabilitiys
     */
    public abstract List<Capability> getCapabilitiesManagedByService();


    protected String getAccessToken() throws OrganizationmanagerException {
        return authService.getSAaccessToken();
    }

}
