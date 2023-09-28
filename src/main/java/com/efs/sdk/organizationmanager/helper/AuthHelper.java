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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Stream;

import static com.efs.sdk.organizationmanager.helper.AuthEntityOrganization.ORGA_REGEX;
import static com.efs.sdk.organizationmanager.helper.AuthEntitySpace.SPACE_REGEX;
import static java.lang.String.format;

/**
 * Helper class for extracting values from JWT tokens.
 *
 * @author e:fs TechHub GmbH
 */
@Component
public class AuthHelper {

    public static final String SUPERUSER_ROLE = "SDK_ADMIN";
    public static final String ORG_CREATE_PERMISSION_ROLE = "org_create_permission";
    private static final String PUBLIC_ACCESS = "all_public";
    private static final String ORG_PUBLIC_ACCESS = "org_" + PUBLIC_ACCESS;
    private static final String SPC_PUBLIC_ACCESS = "spc_" + PUBLIC_ACCESS;

    private static final Logger LOG = LoggerFactory.getLogger(AuthHelper.class);

    /**
     * Check if user has Superuser-Role
     *
     * @param token The user token.
     * @return If user has Superuser-Role.
     */
    public boolean isSuperuser(JwtAuthenticationToken token) {
        return hasRights(token, new String[]{SUPERUSER_ROLE});
    }

    public AuthenticationModel getAuthenticationModel(JwtAuthenticationToken token) {
        AuthEntityOrganization[] orgaRoles = getMatchingRoles(token, ORGA_REGEX).map(AuthEntityOrganization::new).toArray(AuthEntityOrganization[]::new);
        AuthEntitySpace[] spaceRoles = getMatchingRoles(token, SPACE_REGEX).map(AuthEntitySpace::new).toArray(AuthEntitySpace[]::new);
        AuthenticationModel authModel = new AuthenticationModel();
        authModel.setOrganizations(orgaRoles);
        authModel.setSuperuser(isSuperuser(token));
        authModel.setSpaces(spaceRoles);
        authModel.setOrgaPublicAccess(token.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(ORG_PUBLIC_ACCESS::equals));
        authModel.setSpacePublicAccess(token.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(SPC_PUBLIC_ACCESS::equals));
        authModel.setToken(token);
        return authModel;
    }

    private Stream<String> getMatchingRoles(JwtAuthenticationToken token, String regex) {
        return token.getAuthorities().stream().map(GrantedAuthority::getAuthority).filter(a -> a.matches(regex));
    }


    /**
     * Determines if the provided JWT token contains any of the specified roles.
     * <p>
     * This method iterates over the token's authorities and compares them to the list of valid roles provided.
     * The comparison is case-insensitive. If a match is found, the user is considered to have the necessary rights.
     * </p>
     *
     * @param token      The JWT authentication token whose authorities are to be checked.
     * @param validRoles An array of roles considered valid for the check.
     * @return {@code true} if the token's authorities contain at least one of the valid roles; {@code false} otherwise.
     */
    private boolean hasRights(JwtAuthenticationToken token, String[] validRoles) {
        LOG.debug("checking if user has required roles...");
        for (GrantedAuthority auth : token.getAuthorities()) {
            for (String role : validRoles) {
                if (role.equalsIgnoreCase(auth.getAuthority())) {
                    LOG.debug("User has necessary rights. Found match for {}", role);
                    return true;
                }
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("User does NOT have necessary rights. User needs one of those roles: {}", Arrays.toString(validRoles));
        }
        return false;
    }
}
