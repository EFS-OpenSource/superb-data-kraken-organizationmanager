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
import com.efs.sdk.organizationmanager.core.userrequest.model.SpaceUserRequest;
import com.efs.sdk.organizationmanager.core.userrequest.model.UserRequestState;
import com.efs.sdk.organizationmanager.core.userrequest.model.dto.SpaceUserRequestCreateDTO;
import com.efs.sdk.organizationmanager.core.userrequest.model.dto.SpaceUserRequestDTO;
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

@RequestMapping(value = SpaceUserRequestController.ENDPOINT)
@RestController
@Tag(name = SpaceUserRequestController.ENDPOINT)
public class SpaceUserRequestController {

    /**
     * constant containing the userrequest-endpoint
     */
    static final String ENDPOINT = "/api/v1.0/organization/{orgaId}/space/{spaceId}/userrequests";
    private final AuthHelper authHelper;
    private final OrganizationManagerService orgaManagerService;
    private final EntityConverter entityConverter;

    public SpaceUserRequestController(OrganizationManagerService orgaManagerService, AuthHelper authHelper, EntityConverter entityConverter) {
        this.orgaManagerService = orgaManagerService;
        this.authHelper = authHelper;
        this.entityConverter = entityConverter;
    }

    @Operation(summary = "Accept UserRequest for Space", description = "Accept the given `UserRequest` for `Space`")
    @PutMapping(path = "/{id}/accept", produces = APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully accepted `UserRequest` for `Space`.")
    @ApiResponse(responseCode = "403", description = "User does not have permission to accept the `UserRequest`.", content = @Content(schema =
    @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization`, `Space`, `UserRequest` or `User` not found.", content = @Content(schema =
    @Schema(hidden = true)))
    public ResponseEntity<SpaceUserRequestDTO> acceptSpaceRequest(@Parameter(hidden = true) JwtAuthenticationToken token, @PathVariable @Parameter(description =
            "The id of the `Organization`.") long orgaId,
            @PathVariable @Parameter(description = "The id of the `Space`.") long spaceId, @PathVariable @Parameter(description = "The id of the " +
            "`UserRequest`.") long id) throws OrganizationmanagerException {
        SpaceUserRequest spaceUserRequest = orgaManagerService.acceptSpaceRequest(authHelper.getAuthenticationModel(token), orgaId, spaceId, id);
        return ResponseEntity.ok(entityConverter.convertToDTO(spaceUserRequest));
    }

    @Operation(summary = "Create UserRequest for Space", description = "Request access to `Space`")
    @PostMapping(produces = APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully created `UserRequest` for `Organization`.")
    @ApiResponse(responseCode = "404", description = "`Organization` or `Space` not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<SpaceUserRequestDTO> createSpaceRequest(@Parameter(hidden = true) JwtAuthenticationToken token, @PathVariable @Parameter(description =
            "The id of the `Organization`.") long orgaId,
            @PathVariable @Parameter(description = "The id of the `Space`.") long spaceId, @RequestBody SpaceUserRequestCreateDTO dto) throws OrganizationmanagerException {
        SpaceUserRequest spaceUserRequest = orgaManagerService.createSpaceRequest(authHelper.getAuthenticationModel(token), orgaId, spaceId,
                entityConverter.convertToEntity(dto));
        return ResponseEntity.ok(entityConverter.convertToDTO(spaceUserRequest));
    }

    @Operation(summary = "Decline UserRequest for Space", description = "Decline the given `UserRequest` for `Space`")
    @PutMapping(path = "/{id}/decline", produces = APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully declined `UserRequest` for `Space`.")
    @ApiResponse(responseCode = "403", description = "User " +
            "does not have permission to accept the `UserRequest`.", content = @Content(schema = @Schema(hidden = true)))
    @ApiResponse(responseCode = "404",
            description = "`Organization`, `Space`, `UserRequest` or `User` not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<SpaceUserRequestDTO> declineSpaceRequest(@Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable @Parameter(description =
                    "The id of the `Organization`.") long orgaId,
            @PathVariable @Parameter(description = "The id of the `Space`.") long spaceId, @PathVariable @Parameter(description = "The id of the " +
            "`UserRequest`.") long id) throws OrganizationmanagerException {
        SpaceUserRequest spaceUserRequest = orgaManagerService.declineSpaceRequest(authHelper.getAuthenticationModel(token), orgaId, spaceId, id);
        return ResponseEntity.ok(entityConverter.convertToDTO(spaceUserRequest));
    }

    @Operation(summary = "Gets UserRequests for Space", description = "Lists `UserRequest`s for `Space`")
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully listed all `UserRequest` for `Space`.")
    @ApiResponse(responseCode = "404", description = "`Organization` or `Space` not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<List<SpaceUserRequestDTO>> listSpaceRequests(@Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "The id of the `Organization`.") long orgaId,
            @PathVariable @Parameter(description = "The id of the `Space`.") long spaceId, @Parameter(description = "Filter `UserRequest`s by a " +
            "certain state - optional") @RequestParam(required = false) UserRequestState state) throws OrganizationmanagerException {
        List<SpaceUserRequest> spaceUserRequests;
        if (state == null) {
            spaceUserRequests = orgaManagerService.listSpaceRequests(authHelper.getAuthenticationModel(token), orgaId, spaceId);
        } else {
            spaceUserRequests = orgaManagerService.listSpaceRequests(authHelper.getAuthenticationModel(token), orgaId, spaceId, state);
        }
        return ResponseEntity.ok(spaceUserRequests.stream().map(entityConverter::convertToDTO).toList());
    }
}
