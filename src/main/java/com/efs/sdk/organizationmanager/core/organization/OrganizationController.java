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

import com.efs.sdk.common.domain.dto.OrganizationCreateDTO;
import com.efs.sdk.common.domain.dto.OrganizationReadDTO;
import com.efs.sdk.common.domain.dto.OrganizationUpdateDTO;
import com.efs.sdk.logging.AuditLogger;
import com.efs.sdk.organizationmanager.commons.OrganizationmanagerException;
import com.efs.sdk.organizationmanager.core.OrganizationManagerService;
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.helper.AuthConfiguration;
import com.efs.sdk.organizationmanager.helper.AuthHelper;
import com.efs.sdk.organizationmanager.helper.AuthenticationModel;
import com.efs.sdk.organizationmanager.helper.EntityConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.List;

import static com.efs.sdk.organizationmanager.helper.AuthConfiguration.GET;

@RequestMapping(value = OrganizationController.ENDPOINT)
@RestController
@Tag(name = OrganizationController.ENDPOINT)
public class OrganizationController {
    /**
     * constant containing the controller-endpoint
     */
    static final String ENDPOINT = "/api/v1.0/organization";
    private static final Logger LOG = LoggerFactory.getLogger(OrganizationController.class);

    private final AuthHelper authHelper;
    private final OrganizationService service;
    private final OrganizationManagerService orgaManagerService;
    private final EntityConverter converter;

    public OrganizationController(AuthHelper authHelper, OrganizationService service, EntityConverter converter,
            OrganizationManagerService orgaManagerService) {
        this.authHelper = authHelper;
        this.service = service;
        this.converter = converter;
        this.orgaManagerService = orgaManagerService;
    }

    @Operation(summary = "Create a new Organization", description = "Create a new `Organization`, also calls dedicated services in order to create " +
            "`Organization`-context. This feature is only applicable for users with the role 'org_create_permission'.")
    @PostMapping(produces = "application/json")
    @PreAuthorize("hasRole('" + AuthHelper.ORG_CREATE_PERMISSION_ROLE + "')")
    @ApiResponse(responseCode = "200", description = "Successfully created `Organization`.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "403", description = "User does not have the required permissions.", content = @Content(schema = @Schema(hidden =
            true)))
    public ResponseEntity<OrganizationReadDTO> createOrganization(@Parameter(hidden = true) JwtAuthenticationToken token,
            @RequestBody OrganizationCreateDTO organization) throws OrganizationmanagerException {
        LOG.debug("creating organization '{}'", organization.getName());
        var orgCreated = orgaManagerService.createOrganization(organization);
        updateOwners(orgCreated);
        OrganizationReadDTO returnDTO = converter.convertToDTO(orgCreated, OrganizationReadDTO.class);
        AuditLogger.info(LOG, "organization {} sucessfully created. Created Values {}", token, returnDTO.getId(),
                orgCreated);
        return ResponseEntity.ok(returnDTO);
    }

    @Operation(summary = "Lists all Organizations", description = """
            Lists all `Organization`s the user has access to. 
                        
            You can also specify `permissions`, then only those `Organization`s are listed that contain a `Space` to which the user has the appropriate permission. This feature can be used, for example, to generate a list of `Organization`s to which the user is allowed to upload data.
            """)
    @GetMapping(produces = "application/json")
    @ApiResponse(responseCode = "200", description = "Successfully listed all `Organization`s the user has access to.")
    public ResponseEntity<List<OrganizationReadDTO>> getAllOrganizations(
            @Parameter(hidden = true) JwtAuthenticationToken token,
            @Parameter(description = "Name of the permissions.", schema = @Schema(type = "string", allowableValues = {"READ", "WRITE", "DELETE"}), in =
                    ParameterIn.QUERY) @RequestParam(required = false) AuthConfiguration permissions) throws OrganizationmanagerException {
        LOG.debug("getting all organizations");
        // persisted in database
        AuthenticationModel authModel = authHelper.getAuthenticationModel(token);
        AuthConfiguration authConfig = permissions == null ? GET : permissions;
        String[] allowedOrganizations = authModel.getOrganizationsByPermission(authConfig);
        List<Organization> items = service.getAllOrganizations(authModel, allowedOrganizations, authConfig);
        for (Organization org : items) {
            updateOwners(org);
        }
        List<OrganizationReadDTO> orgaDTOs = new ArrayList<>();
        for (Organization item : items) {
            OrganizationReadDTO dto = converter.convertToDTO(item, OrganizationReadDTO.class);
            orgaDTOs.add(dto);
        }
        return ResponseEntity.ok(orgaDTOs);
    }

    @Operation(summary = "Gets Organization by id", description = "Gets the given `Organization` if the user has access to.")
    @GetMapping(path = "/{id}", produces = "application/json")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved `Organization`-information.")
    @ApiResponse(responseCode
            = "403", description = "User does not have permissions to the `Organization`.", content = @Content(schema = @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization` was not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<OrganizationReadDTO> getOrganization(@Parameter(hidden = true) JwtAuthenticationToken token, @PathVariable @Parameter(description =
            "The id of the `Organization`.") long id) throws OrganizationmanagerException {
        LOG.debug("get organization {}", id);
        // persisted in database
        Organization item = service.getOrganization(id, authHelper.getAuthenticationModel(token));
        updateOwners(item);
        OrganizationReadDTO orgaDTO = converter.convertToDTO(item, OrganizationReadDTO.class);
        return ResponseEntity.ok(orgaDTO);
    }

    @Operation(summary = "Gets Organization by name", description = "Gets the given `Organization` if the user has access to.")
    @GetMapping(path = "/name/{name}", produces = "application/json")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved `Organization`-information.")
    @ApiResponse(responseCode
            = "403", description = "User does not have permissions to the `Organization`.", content = @Content(schema = @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization` was not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<OrganizationReadDTO> getOrganizationByName(@Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "The name of the `Organization`.") String name) throws OrganizationmanagerException {
        LOG.debug("get organization {}", name);
        // persisted in database
        Organization item = service.getOrganizationByName(name, authHelper.getAuthenticationModel(token));
        updateOwners(item);
        OrganizationReadDTO orgaDTO = converter.convertToDTO(item, OrganizationReadDTO.class);
        return ResponseEntity.ok(orgaDTO);
    }

    @Operation(summary = "Updates an Organization", description = "Updates the given `Organization` if the user has appropriate permissions.")
    @PutMapping(path = "/{id}", produces = "application/json")
    @ApiResponse(responseCode = "200", description = "Successfully updated the `Organization`.")
    @ApiResponse(responseCode = "403",
            description = "User does not have permissions to update the `Organization`.", content = @Content(schema = @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization` was not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<OrganizationReadDTO> updateOrganization(@Parameter(hidden = true) JwtAuthenticationToken token,
            @RequestBody OrganizationUpdateDTO dto, @PathVariable @Parameter(description = "The id of the `Organization`.") long id) throws OrganizationmanagerException {
        LOG.debug("updating organization '{}'", dto.getName());
        Organization item = converter.convertToEntity(dto, Organization.class);
        item.setId(id);

        Organization organization = orgaManagerService.updateOrganization(item, authHelper.getAuthenticationModel(token));
        updateOwners(organization);

        OrganizationReadDTO orgaDTO = converter.convertToDTO(organization, OrganizationReadDTO.class);
        AuditLogger.info(LOG, "organization {} sucessfully updated. Updated Organization Values {}", token, orgaDTO.getId(),
                item);
        return ResponseEntity.ok(orgaDTO);
    }

    @Operation(summary = "Deletes an Organization", description = """
            Deletes the given `Organization` (only Superuser is allowed to do this).
                        
            Also deletes all Spaces within this `Organization`, including `Organization`- and `Space`-context (indices, storage, roles).
                                            
            **CAUTION**: leads to information-loss!!
            """)
    @DeleteMapping(path = "/{orgaName}")
    @PreAuthorize("hasRole('" + AuthHelper.SUPERUSER_ROLE + "')")
    @ApiResponse(responseCode = "200", description = "Successfully deleted the `Organization`.")
    @ApiResponse(responseCode = "403",
            description = "User does not have permissions to delete the `Organization`.", content = @Content(schema = @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization` was not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<Void> deleteOrganization(
            @PathVariable @Parameter(description = "The name of the `Organization`.") String orgaName,
            @Parameter(hidden = true) JwtAuthenticationToken token
    ) throws OrganizationmanagerException {
        LOG.debug("deleting organization '{}'", orgaName);
        this.orgaManagerService.deleteOrganization(orgaName, authHelper.getAuthenticationModel(token));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void updateOwners(Organization org) throws OrganizationmanagerException {
        List<String> userIds = org.getOwners();
        List<String> userNames = new ArrayList<>();
        for (String userId : userIds) {
            userNames.add(orgaManagerService.getUserName(userId));
        }
        org.setOwners(userNames);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(AuthConfiguration.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                setValue(AuthConfiguration.valueOf(text.toUpperCase()));
            }
        });
    }
}
