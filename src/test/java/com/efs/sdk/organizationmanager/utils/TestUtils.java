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
package com.efs.sdk.organizationmanager.utils;

import net.minidev.json.JSONObject;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

public final class TestUtils {

    public static final String MY_USERNAME = "me";

    private TestUtils() {
        // hidden constructor
    }


    public static void assumeAuthToken() {
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(getJwt(emptyList()));
        // Mockito.whens() for your authorization object
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }


    public static Jwt getJwt(List<String> roles) {
        Jwt.Builder jwtBuilder = Jwt.withTokenValue("token").header("alg", "none").claim("sub", MY_USERNAME).claim("scope", "openid email profile");

        if (!roles.isEmpty()) {
            Map<String, Object> roleMap = new HashMap<>();
            roleMap.put("roles", roles);
            jwtBuilder.claim("realm_access", new JSONObject(roleMap));
        }

        return jwtBuilder.build();
    }
}
