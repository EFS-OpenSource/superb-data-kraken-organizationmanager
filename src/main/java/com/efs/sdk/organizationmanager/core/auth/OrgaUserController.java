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
import com.efs.sdk.organizationmanager.core.auth.model.OrganizationUserDTO;
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

@RequestMapping(value = OrgaUserController.ENDPOINT)
@RestController
@Tag(name = OrgaUserController.ENDPOINT)
public class OrgaUserController {
    /**
     * constant containing the organization-user-endpoint
     */
    static final String ENDPOINT = "/api/v1.0/organization/{orgaId}/users";
    private final OrganizationManagerService orgaManagerService;
    private final AuthHelper authHelper;

    public OrgaUserController(AuthHelper authHelper, OrganizationManagerService orgaManagerService) {
        this.authHelper = authHelper;
        this.orgaManagerService = orgaManagerService;
    }

    @Operation(summary = "Gets`User`s in `Organization`", description = "Lists all `User`s in `Organization`")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully listed `User`s within given `Organization`.")
    @ApiResponse(responseCode = "403", description = "User does not have access to `Organization` or the `User` is neither owner nor admin.", content =
    @Content(schema = @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization` not found", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<Set<OrganizationUserDTO>> listUsers(
            @Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "The id of the `Organization`.") long orgaId
    ) throws OrganizationmanagerException {
        return ResponseEntity.ok(orgaManagerService.listUsers(authHelper.getAuthenticationModel(token), orgaId));
    }

    @Operation(summary = "Set roles by user-id", description = """
            Set roles to a `User` by its id. Setting a role includes assigning as well as withdrawing roles.
                        
            **NOTE:** if a `User`'s "ACCESS"-role is revoked, he loses his access to all `Space`s to which he is currently entitled. However, these `Space` roles are not removed by the system (so that it would be easy to restore them).
            """)
    @PutMapping(path = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully set roles.")
    @ApiResponse(responseCode = "400", description = "`User` or role could not be retrieved, role could not be assigned.", content = @Content(schema =
    @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization` or `User` was not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<UserDTO> setRoles(
            @Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "The id of the `Organization`.") long orgaId,
            @Parameter(description = "The id of the `User` as provided by the OAuth-Provider.") @PathVariable String userId,
            @Parameter(description = "The scopes of the role", schema = @Schema(type = "string", allowableValues = {"ACCESS", "ADMIN", "TRUSTEE"}), in =
                    ParameterIn.QUERY) @RequestParam(value = "roleScopes") List<RoleHelper.OrganizationScopeRole> roleScopes
    ) throws OrganizationmanagerException {
        return ResponseEntity.ok(orgaManagerService.setRoles(authHelper.getAuthenticationModel(token), orgaId, userId, roleScopes));
    }

    @Operation(summary = "Set roles by username", description = """
            Set roles to a `User` by its username. Setting a role includes assigning as well as withdrawing roles.
                        
            **NOTE:** if a `User`'s "ACCESS"-role is revoked, he loses his access to all `Space`s to which he is currently entitled. However, these `Space` roles are not removed by the system (so that it would be easy to restore them).
            """)
    @PutMapping(path = "/name/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully set roles.")
    @ApiResponse(responseCode = "400", description = "`User` or role could not be retrieved, role could not be assigned.", content = @Content(schema =
    @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization` or `User` was not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<UserDTO> setRolesByName(
            @Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "The id of the `Organization`.") long orgaId,
            @Parameter(description = "The name of the `User`.") @PathVariable String name,
            @Parameter(description = "The scopes of the role", schema = @Schema(type = "string", allowableValues = {"ACCESS", "ADMIN", "TRUSTEE"}), in =
                    ParameterIn.QUERY) @RequestParam(value = "roleScopes") List<RoleHelper.OrganizationScopeRole> roleScopes
    ) throws OrganizationmanagerException {
        return ResponseEntity.ok(orgaManagerService.setRolesByName(authHelper.getAuthenticationModel(token), orgaId, name, roleScopes));
    }

    @Operation(summary = "Set roles by email", description = """
            Set roles to a `User` by its email. Setting a role includes assigning as well as withdrawing roles.
                        
            **NOTE:** if a `User`'s "ACCESS"-role is revoked, he loses his access to all `Space`s to which he is currently entitled. However, these `Space` roles are not removed by the system (so that it would be easy to restore them).
            """)
    @PutMapping(path = "/email/{email}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200", description = "Successfully set roles.")
    @ApiResponse(responseCode = "400", description = "`User` or role could not be retrieved, role could not be assigned.", content = @Content(schema =
    @Schema(hidden = true)))
    @ApiResponse(responseCode = "404", description = "`Organization` or `User` was not found.", content = @Content(schema = @Schema(hidden = true)))
    public ResponseEntity<UserDTO> setRolesByEmail(
            @Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "The id of the `Organization`.") long orgaId,
            @Parameter(description = "The email of the user.") @PathVariable String email,
            @Parameter(description = "The scopes of the role", schema = @Schema(type = "string", allowableValues = {"ACCESS", "ADMIN", "TRUSTEE"}),
                    in = ParameterIn.QUERY) @RequestParam(value = "roleScopes") List<RoleHelper.OrganizationScopeRole> roleScopes
    ) throws OrganizationmanagerException {
        return ResponseEntity.ok(orgaManagerService.setRolesByEmail(authHelper.getAuthenticationModel(token), orgaId, email, roleScopes));
    }
}
