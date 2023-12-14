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

import com.efs.sdk.common.domain.dto.OrganizationContextDTO;
import com.efs.sdk.common.domain.dto.SpaceContextDTO;
import com.efs.sdk.common.domain.model.Capability;
import com.efs.sdk.organizationmanager.commons.OrganizationmanagerException;
import com.efs.sdk.organizationmanager.core.auth.AuthService;
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.core.space.model.Space;
import com.efs.sdk.organizationmanager.helper.EntityConverter;
import com.efs.sdk.storage.OrganizationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import static com.efs.sdk.common.domain.model.Capability.ANALYSIS;
import static com.efs.sdk.common.domain.model.Capability.METADATA;
import static com.efs.sdk.organizationmanager.commons.OrganizationmanagerException.ORGANIZATIONMANAGER_ERROR.METADATA_SERVICE_ERROR;
import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
// optionally disables the component (used for local development)
// defaults to Component being injected if property is missing or set to false
@ConditionalOnProperty(
        value = "sdk.services.metadata.connection-disabled",
        havingValue = "false",
        matchIfMissing = true)
public class MetadataServiceRestClient extends AbstractServiceRestClient {
    private static final Logger LOG = LoggerFactory.getLogger(MetadataServiceRestClient.class);

    public MetadataServiceRestClient(
            RestTemplate restTemplate,
            @Value("${sdk.services.metadata.context-endpoint}") String serviceEndpoint,
            EntityConverter converter,
            AuthService authService) {

        super(restTemplate, converter, serviceEndpoint, authService);
    }

    @Override
    public void createOrganizationContextImpl(Organization org) throws OrganizationmanagerException {
        LOG.debug("trying to create metadata Organization context...");
        OrganizationContextDTO organizationDTO = converter.convertToDTO(org, OrganizationContextDTO.class);

        URI uri = URI.create(serviceEndpoint + "/organization/").normalize();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.setBearerAuth(getAccessToken());
        HttpEntity<Object> requestEntity = new HttpEntity<>(organizationDTO, headers);

        try {
            restTemplate.postForEntity(uri, requestEntity, OrganizationDTO.class);
        } catch (RestClientException e) {
            LOG.debug(e.getMessage());
            throw new OrganizationmanagerException(METADATA_SERVICE_ERROR);
        }
    }

    @Override
    public void updateOrganizationContextImpl(Organization org) throws OrganizationmanagerException {
        LOG.debug("trying to update metadata Organization context...");
        OrganizationContextDTO orgDTO = converter.convertToDTO(org, OrganizationContextDTO.class);

        URI uri = URI.create(serviceEndpoint + "/organization/" + orgDTO.getName()).normalize();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.setBearerAuth(getAccessToken());
        HttpEntity<Object> requestEntity = new HttpEntity<>(orgDTO, headers);

        try {
            restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, OrganizationContextDTO.class);
        } catch (RestClientException e) {
            LOG.debug(e.getMessage());
            throw new OrganizationmanagerException(METADATA_SERVICE_ERROR);
        }
    }

    @Override
    public void createSpaceContextImpl(Organization org, Space spc) throws OrganizationmanagerException {
        LOG.debug("trying to create metadata Space context...");
        OrganizationContextDTO orgDTO = converter.convertToDTO(org, OrganizationContextDTO.class);
        SpaceContextDTO spcDTO = converter.convertToDTO(spc, SpaceContextDTO.class);
        spcDTO.setOrganization(orgDTO);

        URI uri = URI.create(format("%s/organization/%s/space/", serviceEndpoint, org.getName())).normalize();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.setBearerAuth(getAccessToken());
        HttpEntity<Object> requestEntity = new HttpEntity<>(spcDTO, headers);

        try {
            restTemplate.postForEntity(uri, requestEntity, SpaceContextDTO.class);
        } catch (RestClientException e) {
            LOG.debug(e.getMessage());
            throw new OrganizationmanagerException(METADATA_SERVICE_ERROR);
        }
    }

    @Override
    protected void updateSpaceContextImpl(Organization org, Space original, Space update) throws OrganizationmanagerException {
        LOG.debug("trying to update metadata Space context...");
        OrganizationContextDTO orgDTO = converter.convertToDTO(org, OrganizationContextDTO.class);
        SpaceContextDTO spcDTO = converter.convertToDTO(update, SpaceContextDTO.class);
        spcDTO.setOrganization(orgDTO);

        URI uri = URI.create(format("%s/organization/%s/space/%s", serviceEndpoint, org.getName(), update.getName())).normalize();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.setBearerAuth(getAccessToken());
        HttpEntity<Object> requestEntity = new HttpEntity<>(spcDTO, headers);

        try {
            restTemplate.exchange(uri, HttpMethod.PUT, requestEntity, SpaceContextDTO.class);
        } catch (RestClientException e) {
            LOG.debug(e.getMessage());
            throw new OrganizationmanagerException(METADATA_SERVICE_ERROR);
        }
    }

    @Override
    public void deleteOrganizationContextImpl(Organization org) throws OrganizationmanagerException {
        LOG.debug("trying to delete metadata Organization context...");
        URI uri = URI.create(format("%s/organization/%s", serviceEndpoint, org.getName())).normalize();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(uri, HttpMethod.DELETE, requestEntity, Void.class);
        } catch (RestClientException e) {
            LOG.debug(e.getMessage());
            throw new OrganizationmanagerException(METADATA_SERVICE_ERROR);
        }
    }

    @Override
    public void deleteSpaceContextImpl(Organization org, Space spc) throws OrganizationmanagerException {
        LOG.debug("trying to delete metadata Space context...");
        URI uri = URI.create(format("%s/organization/%s/space/%s", serviceEndpoint, org.getName(), spc.getName())).normalize();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(uri, HttpMethod.DELETE, requestEntity, Void.class);
        } catch (RestClientException e) {
            LOG.debug(e.getMessage());
            throw new OrganizationmanagerException(METADATA_SERVICE_ERROR);
        }
    }

    @Override
    public List<Capability> getCapabilitiesManagedByService() {
        return List.of(METADATA, ANALYSIS);
    }

    /***
     * Metadata service needs to be called regardless of the space capabilities.
     * The Metadata service knows what to do with the capabilities
     */
    @Override
    protected boolean hasOrganizationContext(Organization org) {
        // Metadata service should always be called for opensearch roles, rolesmappings, tenants
        return true;
    }

    /***
     * Metadata service needs to be called regardless of the space capabilities.
     * The Metadata service then knows what to do with the capabilities
     */
    @Override
    protected boolean hasSpaceContext(Space spc) {
        // Metadata service should always be called for opensearch roles, rolesmappings, tenants
        return true;
    }

}
