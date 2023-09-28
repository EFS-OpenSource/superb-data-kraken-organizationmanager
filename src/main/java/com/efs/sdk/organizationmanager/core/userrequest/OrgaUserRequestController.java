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
package com.efs.sdk.organizationmanager.core.userrequest;

import com.efs.sdk.organizationmanager.commons.OrganizationmanagerException;
import com.efs.sdk.organizationmanager.core.OrganizationManagerService;
import com.efs.sdk.organizationmanager.core.userrequest.model.OrganizationUserRequest;
import com.efs.sdk.organizationmanager.core.userrequest.model.UserRequestState;
import com.efs.sdk.organizationmanager.core.userrequest.model.dto.OrganizationUserRequestCreateDTO;
import com.efs.sdk.organizationmanager.core.userrequest.model.dto.OrganizationUserRequestDTO;
import com.efs.sdk.organizationmanager.helper.AuthHelper;
import com.efs.sdk.organizationmanager.helper.EntityConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RequestMapping(value = OrgaUserRequestController.ENDPOINT)
@RestController
@Tag(name = OrgaUserRequestController.ENDPOINT)
public class OrgaUserRequestController {

    /**
     * constant containing the userrequest-endpoint
     */
    static final String ENDPOINT = "/api/v1.0/organization/{orgaId}/userrequests";
    private final AuthHelper authHelper;
    private final OrganizationManagerService orgaManagerService;
    private final EntityConverter entityConverter;

    public OrgaUserRequestController(OrganizationManagerService orgaManagerService, AuthHelper authHelper, EntityConverter entityConverter) {
        this.orgaManagerService = orgaManagerService;
        this.authHelper = authHelper;
        this.entityConverter = entityConverter;
    }

    @Operation(summary = "Accept UserRequest for Organization", description = "Accept the given `UserRequest` for `Organization`")
    @PutMapping(path = "/{id}/accept", produces = APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully accepted `UserRequest` for `Organization`.")
    @ApiResponse(responseCode = "403", description = "User does not have permission to accept the `UserRequest`.", content = @Content(schema =
    @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization`, `UserRequest` or `User` not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<OrganizationUserRequestDTO> acceptOrganizationRequest(@Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "The id of the `Organization`.") long orgaId, @PathVariable @Parameter(description = "The id of the " +
            "`UserRequest`.") long id) throws OrganizationmanagerException {
        OrganizationUserRequest organizationUserRequest = orgaManagerService.acceptOrganizationRequest(authHelper.getAuthenticationModel(token), orgaId, id);
        return ResponseEntity.ok(entityConverter.convertToDTO(organizationUserRequest));
    }

    @Operation(summary = "Create UserRequest for Organization", description = "Request access to `Organization`")
    @PostMapping(produces = APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully created `UserRequest` for `Organization`.")
    @ApiResponse(responseCode = "404", description = "`Organization` not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<OrganizationUserRequestDTO> createOrganizationRequest(@Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "The id of the `Organization`.") long orgaId, @RequestBody OrganizationUserRequestCreateDTO dto) throws OrganizationmanagerException {
        OrganizationUserRequest organizationUserRequest = orgaManagerService.createOrganizationRequest(authHelper.getAuthenticationModel(token), orgaId,
                entityConverter.convertToEntity(dto));
        return ResponseEntity.ok(entityConverter.convertToDTO(organizationUserRequest));
    }

    @Operation(summary = "Decline UserRequest for Organization", description = "Decline the given `UserRequest` for `Organization`")
    @PutMapping(path = "/{id}/decline", produces = APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully declined `UserRequest` for `Organization`.")
    @ApiResponse(responseCode = "403", description = "User does not have permission to accept the `UserRequest`.", content = @Content(schema =
    @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization`, `UserRequest` or `User` not found.", content =
    @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<OrganizationUserRequestDTO> declineOrganizationRequest(@Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "The id of the `Organization`.") long orgaId, @PathVariable @Parameter(description = "The id of the " +
            "`UserRequest`.") long id) throws OrganizationmanagerException {
        OrganizationUserRequest organizationUserRequest = orgaManagerService.declineOrganizationRequest(authHelper.getAuthenticationModel(token), orgaId, id);
        return ResponseEntity.ok(entityConverter.convertToDTO(organizationUserRequest));
    }

    @Operation(summary = "Gets UserRequests for Organization", description = "Lists `UserRequest`s for `Organization`")
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully listed all `UserRequest` for `Organization`.")
    @ApiResponse(responseCode = "404", description = "`Organization` not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<List<OrganizationUserRequestDTO>> listOrganizationRequests(@Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "The id of the `Organization`.") long orgaId, @Parameter(description = "Filter `UserRequest`s by a certain" +
            " state - optional", schema = @Schema(type = "string", allowableValues = {"OPEN", "ACCEPTED", "DECLINED"})) @RequestParam(required = false) UserRequestState state) throws OrganizationmanagerException {
        List<OrganizationUserRequest> organizationUserRequests;
        if (state == null) {
            organizationUserRequests = orgaManagerService.listOrganizationRequests(authHelper.getAuthenticationModel(token), orgaId);
        } else {
            organizationUserRequests = orgaManagerService.listOrganizationRequests(authHelper.getAuthenticationModel(token), orgaId, state);
        }
        return ResponseEntity.ok(organizationUserRequests.stream().map(entityConverter::convertToDTO).toList());
    }
}
