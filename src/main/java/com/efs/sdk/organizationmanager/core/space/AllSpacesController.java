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

import com.efs.sdk.organizationmanager.commons.OrganizationmanagerException;
import com.efs.sdk.organizationmanager.core.OrganizationManagerService;
import com.efs.sdk.organizationmanager.helper.AuthConfiguration;
import com.efs.sdk.organizationmanager.helper.AuthHelper;
import com.efs.sdk.organizationmanager.helper.AuthenticationModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.efs.sdk.organizationmanager.helper.AuthConfiguration.GET;

@RequestMapping(value = AllSpacesController.ENDPOINT)
@RestController
@Tag(name = AllSpacesController.ENDPOINT)
public class AllSpacesController {
    /**
     * constant containing the space-endpoint
     */
    static final String ENDPOINT = "/api/v2.0/spaces";
    private static final Logger LOG = LoggerFactory.getLogger(AllSpacesController.class);

    private final OrganizationManagerService orgaManagerService;

    private final AuthHelper authHelper;

    public AllSpacesController(AuthHelper authHelper, OrganizationManagerService orgaManagerService) {
        this.authHelper = authHelper;
        this.orgaManagerService = orgaManagerService;
    }


    @Operation(summary = "Lists all Spaces in all Organizations", description = """
            Lists the names of all spaces the user has access to, in the form of the organization name and space name separated by "_", i.e. as <organization-name>_<space-name>.
                        
                 """)
    @GetMapping(produces = "application/json")
    @ApiResponse(responseCode = "200", description = "Successfully listed all spaces the user has access to.")
    public ResponseEntity<List<String>> getAllSpaces(
            @Parameter(hidden = true) JwtAuthenticationToken token, @Parameter(description = "Name of the permissions.", schema = @Schema(type = "string", allowableValues
            = {"READ", "WRITE", "DELETE", "GET"}), in = ParameterIn.QUERY) @RequestParam(required = false) AuthConfiguration permissions) throws OrganizationmanagerException {
        LOG.debug("getting all organizations");
        // persisted in database
        AuthenticationModel authModel = authHelper.getAuthenticationModel(token);
        AuthConfiguration authConfig = permissions == null ? GET : permissions;

        return ResponseEntity.ok(orgaManagerService.getSpaceNamesWithOrganizationPrefix(authModel, authConfig));
    }
}
