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
package com.efs.sdk.organizationmanager.core.space;

import com.efs.sdk.common.domain.dto.SpaceCreateDTO;
import com.efs.sdk.common.domain.dto.SpaceReadDTO;
import com.efs.sdk.common.domain.dto.SpaceUpdateDTO;
import com.efs.sdk.logging.AuditLogger;
import com.efs.sdk.organizationmanager.commons.OrganizationmanagerException;
import com.efs.sdk.organizationmanager.core.OrganizationManagerService;
import com.efs.sdk.organizationmanager.core.space.model.Space;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.List;

import static com.efs.sdk.organizationmanager.helper.AuthConfiguration.GET;

@RequestMapping(value = SpaceControllerV2.ENDPOINT)
@RestController
@Tag(name = SpaceControllerV2.ENDPOINT)
public class SpaceControllerV2 {
    /**
     * constant containing the space-endpoint
     */
    static final String ENDPOINT = "/api/v2.0/organization/{orgaId}/space";
    private static final Logger LOG = LoggerFactory.getLogger(SpaceControllerV2.class);
    private final SpaceService service;
    private final OrganizationManagerService orgaManagerService;
    private final EntityConverter converter;
    private final AuthHelper authHelper;

    public SpaceControllerV2(AuthHelper authHelper, SpaceService service, EntityConverter converter, OrganizationManagerService orgaManagerService) {
        this.authHelper = authHelper;
        this.service = service;
        this.converter = converter;
        this.orgaManagerService = orgaManagerService;
    }

    @Operation(summary = "Creates a Space", description = "Creates a new `Space` to given `Organization` (only allowed, if user is admin or owner to the " +
            "`Organization`), also calls dedicated services in order to create `Space`-context.")
    @PostMapping(produces = "application/json")
    @ApiResponse(responseCode = "200", description = "Successfully created `Space`.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "403", description = "User does not have the required permissions.", content = @Content(schema = @Schema(hidden =
            true)))
    public ResponseEntity<SpaceReadDTO> createSpace(@Parameter(hidden = true) JwtAuthenticationToken token, @PathVariable @Parameter(description = "The id of" +
            " the `Organization`.") long orgaId, @Valid @RequestBody SpaceCreateDTO dto) throws OrganizationmanagerException {
        LOG.debug("creating space {} in organization {}", dto.getName(), orgaId);
        AuditLogger.info(LOG, "creating space {} in organization {}", token, dto.getName(), orgaId);
        Space space = converter.convertToEntity(dto, Space.class);
        space.setOwners(List.of(token.getToken().getSubject()));
        Space item = orgaManagerService.createSpace(authHelper.getAuthenticationModel(token), orgaId, space);
        updateOwners(item);
        SpaceReadDTO spaceDTO = converter.convertToDTO(item, SpaceReadDTO.class);
        return ResponseEntity.ok(spaceDTO);
    }

    @Operation(summary = "Gets Space", description = "Gets the given `Space` if the user has access to.")
    @GetMapping(path = "{spaceId}", produces = "application/json")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved `Space`-information.")
    @ApiResponse(responseCode = "403", description = "User does not have permissions to the `Organization` or `Space`.", content = @Content(schema =
    @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization` or `Space` was not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<SpaceReadDTO> getSpace(@Parameter(hidden = true) JwtAuthenticationToken token, @PathVariable @Parameter(description = "The id of " +
            "the `Organization`.") long orgaId, @PathVariable @Parameter(description = "The id of the `Space`.") long spaceId) throws OrganizationmanagerException {
        LOG.debug("get space {} of organization {}", spaceId, orgaId);
        Space item = service.getSpaceById(authHelper.getAuthenticationModel(token), orgaId, spaceId);
        updateOwners(item);
        SpaceReadDTO spaceDTO = converter.convertToDTO(item, SpaceReadDTO.class);
        return ResponseEntity.ok(spaceDTO);
    }

    @Operation(summary = "Gets Space by name", description = "Gets `Space` by name of given `Organization` (only allowed, if user has access to " +
            "`Organization` and `Space`.")
    @GetMapping(path = "/name/{spaceName}", produces = "application/json")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved `Space`-information.")
    @ApiResponse(responseCode = "403", description = "User does not have permissions to the `Organization` or `Space`.", content = @Content(schema =
    @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization` or `Space` was not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<SpaceReadDTO> getSpaceByName(@Parameter(hidden = true) JwtAuthenticationToken token, @PathVariable @Parameter(description = "The id" +
            " of the `Organization`.") long orgaId, @PathVariable @Parameter(description = "The name of the `Space`.") String spaceName) throws OrganizationmanagerException {
        Space item = service.getSpaceByName(authHelper.getAuthenticationModel(token), orgaId, spaceName);
        updateOwners(item);
        SpaceReadDTO spaceDTO = converter.convertToDTO(item, SpaceReadDTO.class);
        return ResponseEntity.ok(spaceDTO);
    }

    @Operation(summary = "Lists Spaces", description = """
            Lists all `Space`s the user has access to. 

            You can also specify `permissions`, then only those `Space`s are listed which the user has the appropriate permission. This feature can be used, for example, to generate a list of `Space`s to which the user is allowed to upload data.
            """)
    @GetMapping(produces = "application/json")
    @ApiResponse(responseCode = "200", description = "Successfully listed all `Space`s the user has access to.")
    public ResponseEntity<List<SpaceReadDTO>> getSpaces(@Parameter(hidden = true) JwtAuthenticationToken token, @PathVariable @Parameter(description = "The " +
            "id of the `Organization`.") long orgaId, @Parameter(description = "Name of the permissions.", schema = @Schema(type = "string", allowableValues
            = {"READ", "WRITE", "DELETE", "GET"}), in = ParameterIn.QUERY) @RequestParam(required = false) AuthConfiguration permissions) throws OrganizationmanagerException {
        LOG.debug("list spaces of org {}", orgaId);
        AuthConfiguration authConfig = permissions == null ? GET : permissions;
        AuthenticationModel authModel = authHelper.getAuthenticationModel(token);
        List<Space> spaces =
                service.getSpaces(authModel, orgaId, authConfig).stream().filter(s -> !Space.SPACE_LOADINGZONE.equalsIgnoreCase(s.getName())).toList();
        for (Space space : spaces) {
            updateOwners(space);
        }
        List<SpaceReadDTO> spaceDTOs = spaces.stream().map(s -> converter.convertToDTO(s, SpaceReadDTO.class)).toList();
        return ResponseEntity.ok(spaceDTOs);
    }

    @Operation(summary = "Updates a Space", description = """
            Updates the given `Space` at given `Organization` (only allowed, if user is admin to the `Organization` or owner to the `Space`).
                        
            Updating `Capabilities` may lead to creating or destroying Context - so be cautious of possible information-loss!""")
    @PutMapping(path = "{spaceId}", produces = "application/json")
    @ApiResponse(responseCode = "200", description = "Successfully updated the `Space`.")
    @ApiResponse(responseCode = "403",
            description = "User does not have permissions to update the `Space`.", content = @Content(schema = @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization` or `Space` was not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<SpaceReadDTO> updateSpace(@Parameter(hidden = true) JwtAuthenticationToken token, @PathVariable @Parameter(description = "The id of" +
            " the `Organization`.") long orgaId, @PathVariable @Parameter(description = "The id of the `Space`.") long spaceId,
            @Valid @RequestBody SpaceUpdateDTO payload) throws OrganizationmanagerException {
        LOG.debug("updating space {} in organization {}", payload.getName(), orgaId);
        Space space = converter.convertToEntity(payload, Space.class);
        // overwrite ids from path
        space.setId(spaceId);
        space.setOrganizationId(orgaId);
        space.setOwners(List.of(token.getToken().getSubject()));
        Space item = orgaManagerService.updateSpace(authHelper.getAuthenticationModel(token), orgaId, space);
        updateOwners(item);
        SpaceReadDTO spaceDTO = converter.convertToDTO(item, SpaceReadDTO.class);
        return ResponseEntity.ok(spaceDTO);
    }

    @Operation(summary = "Deletes Space", description = """
            Deletes `Space` of an `Organization` by id (can be executed only by Superuser).
                        
            Also deletes `Space`-context (indices, storage, roles).
                        
            **CAUTION**: leads to information-loss!!
            """)
    @DeleteMapping(path = "{spaceId}", produces = "application/json")
    @ApiResponse(responseCode = "200", description = "Successfully deleted the `Space`.")
    @ApiResponse(responseCode = "403",
            description = "User does not have permissions to delete the `Space`.", content = @Content(schema = @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization` or `Space` was not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<Void> deleteSpaceById(@Parameter(hidden = true) JwtAuthenticationToken token, @PathVariable @Parameter(description = "The id of the" +
            " `Organization`.") long orgaId, @PathVariable @Parameter(description = "The id of the `Space`.") long spaceId) throws OrganizationmanagerException {
        LOG.debug("deleting space {}", spaceId);
        orgaManagerService.deleteSpace(authHelper.getAuthenticationModel(token), orgaId, spaceId);
        AuditLogger.info(LOG, "deleting space {}", token, spaceId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Sets a state for the suitable Space for deletion operation", description = """
            Sets a `Space` as marked to be deleted later (only allowed, if user is admin to the `Organization` or owner to the `Space`). A job later will collect all `Space`s marked and deletes all `Space`-context (indices, storage, roles).
                        
            **CAUTION**: leads to information-loss (if not reverted by updating the `Space`)!!
            """)
    @PutMapping(path = "/{spaceId}/setDeletionState", produces = "application/json")
    @ApiResponse(responseCode = "200", description = "Successfully marked the `Space` for deletion.")
    @ApiResponse(responseCode = "403", description = "User does not have permissions to mark the `Space`.", content = @Content(schema = @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization` or `Space` was not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<SpaceReadDTO> markSpaceForDeletion(@Parameter(hidden = true) JwtAuthenticationToken token, @PathVariable @Parameter(description =
            "The id of the `Organization`.") long orgaId, @PathVariable @Parameter(description = "The id of the `Space`.") long spaceId,
            @Parameter(description = "Should the `Space` be deleted or rather closed?") @RequestParam boolean willBeDeleted) throws OrganizationmanagerException {
        LOG.debug("marking space {} for deletion", spaceId);
        Space space = service.setDeletionState(authHelper.getAuthenticationModel(token), orgaId, spaceId, willBeDeleted);
        SpaceReadDTO spaceDTO = converter.convertToDTO(space, SpaceReadDTO.class);
        return ResponseEntity.ok(spaceDTO);
    }

    private void updateOwners(Space space) throws OrganizationmanagerException {
        List<String> userIds = space.getOwners();
        List<String> userNames = new ArrayList<>();
        for (String userId : userIds) {
            userNames.add(orgaManagerService.getUserName(userId));
        }
        space.setOwners(userNames);
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
