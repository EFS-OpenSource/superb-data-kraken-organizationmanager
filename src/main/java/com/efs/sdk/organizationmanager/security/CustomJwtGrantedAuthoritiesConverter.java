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
package com.efs.sdk.organizationmanager.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Custom converter for authorities contained in a JWT token.
 *
 * @author e:fs TechHub GmbH
 */
public class CustomJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    public static final String CLAIM_ROLES = "roles";

    /**
     * Extracts the authorities from the given token and returns them.
     * <p>
     * Supports mapper- as well as realm-access-roles.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<String> authorities = new ArrayList<>();
        authorities.addAll(getRolesFromRealm(jwt));
        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    /**
     * Get roles from realm-access.
     *
     * @param jwt The JSON Web Token (JWT)
     * @return A collection of roles from realm-access, or an empty set if none are found.
     */
    private Collection<String> getRolesFromRealm(Jwt jwt) {
        Objects.requireNonNull(jwt, "JWT cannot be null");

        Map<String, Object> realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");
        if (realmAccess == null) {
            return Collections.emptySet();
        }

        Collection<String> roles = (Collection<String>) realmAccess.get(CLAIM_ROLES);
        return roles != null ? roles : Collections.emptySet();
    }

}