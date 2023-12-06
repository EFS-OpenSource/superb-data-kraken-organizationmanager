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

import com.efs.sdk.common.domain.dto.OrganizationReadDTO;
import com.efs.sdk.organizationmanager.commons.OrganizationmanagerException;
import com.efs.sdk.organizationmanager.core.auth.model.UserDTO;
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.helper.AuthHelper;
import com.efs.sdk.organizationmanager.helper.EntityConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping(value = OrgaOwnerController.ENDPOINT)
@RestController
@Tag(name = OrgaOwnerController.ENDPOINT)
public class OrgaOwnerController {
    /**
     * constant containing the orga owner-endpoint
     */
    static final String ENDPOINT = "/api/v1.0/organization/{orgaId}/owners";
    private final OwnerService ownerService;
    private final AuthHelper authHelper;
    private final EntityConverter converter;


    public OrgaOwnerController(AuthHelper authHelper, OwnerService ownerService, EntityConverter converter) {
        this.authHelper = authHelper;
        this.ownerService = ownerService;
        this.converter = converter;
    }

    @Operation(summary = "Gets owners in Organization", description = "Lists all owners in `Organization`")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully listed all owners in the `Organization`.")
    @ApiResponse(responseCode = "403", description = "User does not have permission to list owners", content = @Content(schema = @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "Organization or User was not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<List<UserDTO>> listOwners(@Parameter(hidden = true) JwtAuthenticationToken token, @PathVariable @Parameter(description =
            "The id of the `Organization`.") long orgaId) throws OrganizationmanagerException {
        return ResponseEntity.ok(ownerService.listOwners(authHelper.getAuthenticationModel(token), orgaId));
    }

    @Operation(summary = "Assigns Ownership to a Specific Organization", description = """
            This endpoint assigns a new set of owners to the specified `Organization`. Owners can be set using either their user IDs or email addresses, based on the specified `type` parameter.
                       
            Supported `type` values:
             - `email`: The list of owners contains email addresses.
             - `userId`: The list of owners contains user IDs.
                       
            *Note*: The operation will not proceed if any provided user IDs or email addresses are invalid or do not exist.
            
            **CAUTION** overwrites the list of owners - the provided list should be comprehensive and complete.
            """)
    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Owners successfully set")
    @ApiResponse(responseCode = "403", description = "Insufficient permission to set owners.", content = @Content(schema = @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "Either `Organization` or `User` not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<OrganizationReadDTO> setOwners(
            @Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "The id of the `Organization`.") long orgaId,
            @RequestParam(required = false, defaultValue = "userId") String type,
            @RequestBody List<String> owners) throws OrganizationmanagerException {
        Organization organization;
        if (type.equalsIgnoreCase("userId")) {
            organization = ownerService.setOwners(authHelper.getAuthenticationModel(token), orgaId, owners);
        } else if (type.equalsIgnoreCase("email")) {
            organization = ownerService.setOwnersByEmail(authHelper.getAuthenticationModel(token), orgaId, owners);
        } else {
            throw new OrganizationmanagerException(OrganizationmanagerException.ORGANIZATIONMANAGER_ERROR.BAD_REQUEST_PARAM_VALUE, String.format("-- type " +
                    "'%s' does not exist", type));
        }
        OrganizationReadDTO orgaDTO = converter.convertToDTO(organization, OrganizationReadDTO.class);
        return ResponseEntity.ok(orgaDTO);
    }


    @Operation(summary = "Adds owner in Organization by username", description = "Adds owner in `Organization` by username")
    @PutMapping(path = "name/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully added owner by username.")
    @ApiResponse(responseCode = "403", description = "User does not have permission to add owner.", content = @Content(schema = @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "Organization or User was not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<OrganizationReadDTO> addOwnerByName(@Parameter(hidden = true) JwtAuthenticationToken token, @PathVariable @Parameter(description =
            "The id of the `Organization`.") long orgaId, @Parameter(description = "The name of the `User`.") @PathVariable("name") String name) throws OrganizationmanagerException {
        Organization organization = ownerService.addOwnerByName(authHelper.getAuthenticationModel(token), orgaId, name);
        OrganizationReadDTO orgaDTO = converter.convertToDTO(organization, OrganizationReadDTO.class);
        return ResponseEntity.ok(orgaDTO);
    }

    @Operation(summary = "Adds owner in Organization by email", description = "Adds owner in `Organization` by email")
    @PutMapping(path = "email/{email}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully added owner by email.")
    @ApiResponse(responseCode = "403", description = "User does not have permission to add owner.", content = @Content(schema = @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "Organization was not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<OrganizationReadDTO> addOwnerByEmail(@Parameter(hidden = true) JwtAuthenticationToken token, @PathVariable @Parameter(description =
            "The id of the `Organization`.") long orgaId, @Parameter(description = "The email of the `User`.") @PathVariable("email") String email) throws OrganizationmanagerException {
        Organization organization = ownerService.addOwnerByEmail(authHelper.getAuthenticationModel(token), orgaId, email);
        OrganizationReadDTO orgaDTO = converter.convertToDTO(organization, OrganizationReadDTO.class);
        return ResponseEntity.ok(orgaDTO);
    }

}
