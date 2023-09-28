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

import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.core.space.model.Space;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Objects;

public final class Utils {

    private Utils() {
        // do nothing
    }

    /**
     * Get Subject out of Auth-Token from Security-Context
     *
     * @return Auth-Token
     */
    public static String getSubject() {
        return Objects.requireNonNull(((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getToken(), "JWT token of " +
                "SecurityContextHolder should not be null").getSubject();
    }

    // ******************************************************
    //              ORGA-PERMISSION-MANAGEMENT
    // ******************************************************

    /**
     * Checks, if the current user is owner of the given organization
     *
     * @param orga the organization
     * @return owner or not
     */
    public static boolean isOwner(Organization orga) {
        return Objects.requireNonNull(orga, "Organization must not be null").getOwners().stream().anyMatch(owner -> owner.equals(getSubject()));
    }

    /**
     * Checks, if the current user is admin of the given organization
     *
     * @param authModel the authenticationmodel
     * @param orga      the organization
     * @return admin or not
     */
    public static boolean isAdmin(AuthenticationModel authModel, Organization orga) {
        return authModel.isSuperuser() || authModel.isAdmin(Objects.requireNonNull(orga, "Organization must not be null").getName());
    }

    /**
     * Checks, if the current user is owner or admin of the given organization
     *
     * @param authModel the authenticationmodel
     * @param orga      the organization
     * @return admin or owner
     */
    public static boolean isAdminOrOwner(AuthenticationModel authModel, Organization orga) {
        return isOwner(orga) || isAdmin(authModel, orga);
    }

    // ******************************************************
    //              SPACE-PERMISSION-MANAGEMENT
    // ******************************************************

    /**
     * Checks, if the current user is owner of the given space
     *
     * @param spc the space
     * @return owner or not
     */
    public static boolean isOwner(Space spc) {
        return Objects.requireNonNull(spc, "Space must not be null").getOwners().stream().anyMatch(owner -> owner.equals(getSubject()));
    }

    /**
     * Checks, if the current user is owner of the given space or admin of the given organization
     *
     * @param authModel the authenticationmodel
     * @param orga      the organization
     * @param spc       the space
     * @return admin of space or owner of organization
     */
    public static boolean isAdminOrOwner(AuthenticationModel authModel, Organization orga, Space spc) {
        return isOwner(spc) || isAdmin(authModel, orga);
    }

}
