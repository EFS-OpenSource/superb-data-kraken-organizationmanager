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
import com.efs.sdk.organizationmanager.core.OrganizationManagerService;
import com.efs.sdk.organizationmanager.core.auth.model.SpaceUserDTO;
import com.efs.sdk.organizationmanager.core.auth.model.UserDTO;
import com.efs.sdk.organizationmanager.helper.AuthHelper;
import com.efs.sdk.organizationmanager.helper.RoleHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RequestMapping(value = SpaceUserController.ENDPOINT)
@RestController
@Tag(name = SpaceUserController.ENDPOINT)
public class SpaceUserController {
    /**
     * constant containing the space-endpoint
     */
    static final String ENDPOINT = "/api/v1.0/organization/{orgaId}/space/{spaceId}/users";
    private final OrganizationManagerService orgaManagerService;
    private final AuthHelper authHelper;

    public SpaceUserController(AuthHelper authHelper, OrganizationManagerService orgaManagerService) {
        this.authHelper = authHelper;
        this.orgaManagerService = orgaManagerService;
    }

    @Operation(summary = "Gets users in `Space`", description = "Lists all users with access to the given `Space` with their respective roles.")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully listed users within given `Space`.")
    @ApiResponse(responseCode = "403", description = "User does not have access to `Organization`, `Space` or the `User` is neither `Space`-owner nor " +
            "orga-admin.", content = @Content(schema = @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization` or `Space` not found", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<Set<SpaceUserDTO>> listUsers(
            @Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "The id of the `Organization` the required `Space` is within.") long orgaId,
            @PathVariable @Parameter(description = "The id of the `Space`.") long spaceId
    ) throws OrganizationmanagerException {
        return ResponseEntity.ok(orgaManagerService.listUsers(authHelper.getAuthenticationModel(token), orgaId, spaceId));
    }

    @Operation(summary = "Set roles by user-id", description = """
            Set roles to a `User` by its id. Setting a role includes assigning as well as withdrawing roles.
                        
            **NOTE:** `Space`-permissions are only valid, if a `User` also has "ACCESS"-role on its `Organization`.
            """)
    @PutMapping(path = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully set roles.")
    @ApiResponse(responseCode = "400", description = "`User` or role could not be retrieved, role could not be assigned.", content = @Content(schema =
    @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization`, `Space` or `User` was not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<UserDTO> setRoles(
            @Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "The id of the `Organization` the required `Space` is within.") long orgaId,
            @PathVariable @Parameter(description = "The id of the `Space`.") long spaceId,
            @Parameter(description = "The id of the `User` as provided by the OAuth-Provider.") @PathVariable String userId,
            @Parameter(description = "The scopes of the role", schema = @Schema(type = "string", allowableValues = {"USER", "SUPPLIER", "TRUSTEE"}), in =
                    ParameterIn.QUERY) @RequestParam(value = "roleScopes") List<RoleHelper.SpaceScopeRole> roleScopes
    ) throws OrganizationmanagerException {
        UserDTO user = orgaManagerService.setRoles(authHelper.getAuthenticationModel(token), orgaId, spaceId, userId, roleScopes);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Set roles by username", description = """
            Set roles to a `User` by its username. Setting a role includes assigning as well as withdrawing roles.
                        
            **NOTE:** `Space`-permissions are only valid, if a `User` also has "ACCESS"-role on its `Organization`.
            """)
    @PutMapping(path = "/name/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully set roles.")
    @ApiResponse(responseCode = "400", description = "`User` or role could not be retrieved, role could not be assigned.", content = @Content(schema =
    @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization`, `Space` or `User` was not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<UserDTO> setRolesByName(
            @Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "The id of the `Organization` the required `Space` is within.") long orgaId,
            @PathVariable @Parameter(description = "The id of the `Space`.") long spaceId,
            @Parameter(description = "The name of the `User`.") @PathVariable String name,
            @Parameter(description = "The scopes of the role", schema = @Schema(type = "string", allowableValues = {"USER", "SUPPLIER", "TRUSTEE"}), in =
                    ParameterIn.QUERY) @RequestParam(value = "roleScopes") List<RoleHelper.SpaceScopeRole> roleScopes
    ) throws OrganizationmanagerException {
        UserDTO user = orgaManagerService.setRolesByName(authHelper.getAuthenticationModel(token), orgaId, spaceId, name, roleScopes);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Set roles by email", description = """
            Set roles to a `User` by its email. Setting a role includes assigning as well as withdrawing roles.
                        
            **NOTE:** `Space`-permissions are only valid, if a `User` also has "ACCESS"-role on its `Organization`.
            """)
    @PutMapping(path = "/email/{email}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully set roles.")
    @ApiResponse(responseCode = "400", description = "`User` or role could not be retrieved, role could not be assigned.", content = @Content(schema =
    @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization`, `Space` or `User` was not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<UserDTO> setRolesByEmail(
            @Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "The id of the `Organization` the required `Space` is within.") long orgaId,
            @PathVariable @Parameter(description = "The id of the `Space`.") long spaceId,
            @Parameter(description = "The email of the `User`.") @PathVariable String email,
            @Parameter(description = "The scopes of the role", schema = @Schema(type = "string", allowableValues = {"USER", "SUPPLIER", "TRUSTEE"}), in =
                    ParameterIn.QUERY) @RequestParam(value = "roleScopes") List<RoleHelper.SpaceScopeRole> roleScopes
    ) throws OrganizationmanagerException {
        UserDTO user = orgaManagerService.setRolesByEmail(authHelper.getAuthenticationModel(token), orgaId, spaceId, email, roleScopes);
        return ResponseEntity.ok(user);
    }
}
