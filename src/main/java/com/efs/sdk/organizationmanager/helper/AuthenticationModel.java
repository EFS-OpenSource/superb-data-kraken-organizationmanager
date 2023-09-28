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
package com.efs.sdk.organizationmanager.helper;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.*;
import java.util.stream.Stream;

import static com.efs.sdk.organizationmanager.helper.AuthConfiguration.GET;
import static com.efs.sdk.organizationmanager.helper.AuthConfiguration.READ;
import static com.efs.sdk.organizationmanager.helper.AuthEntityOrganization.ACCESS_ROLE;
import static com.efs.sdk.organizationmanager.helper.AuthEntityOrganization.ADMIN_ROLE;
import static java.util.stream.Collectors.toSet;

public class AuthenticationModel {

    // organizations the user has access to
    private AuthEntityOrganization[] organizations = new AuthEntityOrganization[0];
    // user has access to public organizations
    private boolean orgaPublicAccess = false;
    // user has access to public spaces (will also require access to organization)
    private boolean spacePublicAccess = false;
    // spaces the user has access to
    private AuthEntitySpace[] spaces = new AuthEntitySpace[0];
    // user is superuser
    private boolean superuser = false;

    private JwtAuthenticationToken token;

    public boolean isOrgaPublicAccess() {
        return orgaPublicAccess;
    }

    public void setOrgaPublicAccess(boolean orgaPublicAccess) {
        this.orgaPublicAccess = orgaPublicAccess;
    }

    public boolean isSpacePublicAccess() {
        return spacePublicAccess;
    }

    public void setSpacePublicAccess(boolean spacePublicAccess) {
        this.spacePublicAccess = spacePublicAccess;
    }

    public AuthEntityOrganization[] getOrganizations() {
        return organizations == null ? null : organizations.clone();
    }

    public void setOrganizations(AuthEntityOrganization[] organizations) {
        this.organizations = organizations == null ? null : organizations.clone();
    }

    public AuthEntitySpace[] getSpaces() {
        return spaces == null ? null : spaces.clone();
    }

    public void setSpaces(AuthEntitySpace[] spaces) {
        this.spaces = spaces == null ? null : spaces.clone();
    }

    public boolean isSuperuser() {
        return superuser;
    }

    public void setSuperuser(boolean superuser) {
        this.superuser = superuser;
    }

    public JwtAuthenticationToken getToken() {
        return token;
    }

    public void setToken(JwtAuthenticationToken token) {
        this.token = token;
    }

    public String[] getSpacesByPermission(AuthConfiguration authConfig) {
        if (spaces == null) {
            return new String[0];
        }
        List<String> spacesByPermission = new ArrayList<>();
        // special-case: if AuthConfiguration == GET, all other AuthConfigurations are applicable (additional to orga-admin-role)
        if (GET.equals(authConfig)) {
            // read-permission is enough, as all other AuthConfigurations 'inherit' from it
            Stream.of(READ.getAllowedRoles()).forEach(allowedRole -> spacesByPermission.addAll(Arrays.stream(spaces).filter(spaceRole -> spaceRole.getRole().equals(allowedRole)).map(AuthEntitySpace::getSpace).toList()));
        } else {
            Stream.of(authConfig.getAllowedRoles()).forEach(allowedRole -> spacesByPermission.addAll(Arrays.stream(spaces).filter(spaceRole -> spaceRole.getRole().equals(allowedRole)).map(AuthEntitySpace::getSpace).toList()));
        }
        return spacesByPermission.toArray(String[]::new);
    }

    /**
     * Get organizations, where user has certain permission to (read, write, delete)
     *
     * @param authConfig The AuthConfiguration
     * @return organizations, where user has certain permission to
     */
    public String[] getOrganizationsByPermission(AuthConfiguration authConfig) {
        if (spaces == null) {
            return new String[0];
        }
        Set<String> organizationsByPermission = new HashSet<>();
        // certain permission in space of organization
        Stream.of(authConfig.getAllowedRoles()).forEach(allowedRole -> organizationsByPermission.addAll(Arrays.stream(spaces).filter(spaceRole -> spaceRole.getRole().equals(allowedRole)).map(AuthEntitySpace::getOrganization).collect(toSet())));
        // must also have access-permissions
        Set<String> orgaAccessRoles =
                Arrays.stream(organizations).filter(o -> ACCESS_ROLE.equalsIgnoreCase(o.getRole())).map(AuthEntityOrganization::getOrganization).collect(toSet());
        organizationsByPermission.retainAll(orgaAccessRoles);
        return organizationsByPermission.toArray(String[]::new);
    }

    public boolean isAdmin(String orgaName) {
        if (organizations == null) {
            return false;
        }
        return Arrays.stream(organizations).anyMatch(o -> orgaName.equals(o.getOrganization()) && ADMIN_ROLE.equalsIgnoreCase(o.getRole()));
    }
}
