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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.efs.sdk.organizationmanager.utils.TestUtils.getJwt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomJwtGrantedAuthoritiesConverterTest {

    private CustomJwtGrantedAuthoritiesConverter converter;

    @BeforeEach
    void setup() {
        this.converter = new CustomJwtGrantedAuthoritiesConverter();
    }

    @Test
    void givenEmptyRoles_whenExtractResourceRoles_thenOk() {
        List<String> roles = Collections.emptyList();
        Collection<? extends GrantedAuthority> collection = converter.convert(getJwt(roles));
        assertThat(collection, hasSize(0));
    }

    @Test
    void givenRealmRoles_whenConvert_thenOk() {
        List<String> roles = List.of("offline_access", "uma_authorization");
        Collection<? extends GrantedAuthority> roleAuthorities = converter.convert(getJwt(roles));
        assertThat(roleAuthorities, hasSize(2));
        for (String role : roles) {
            assertTrue(roleAuthorities.stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase(role)));
        }
    }

}