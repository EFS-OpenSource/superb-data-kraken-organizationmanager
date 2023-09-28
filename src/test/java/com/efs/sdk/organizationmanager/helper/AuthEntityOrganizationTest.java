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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static com.efs.sdk.organizationmanager.helper.AuthEntityOrganization.ACCESS_ROLE;
import static com.efs.sdk.organizationmanager.helper.AuthEntityOrganization.ADMIN_ROLE;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthEntityOrganizationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void givenInvalidRole_whenInitiate_thenError() {
        String roleName = "something-other_than_expected";
        assertThrows(IllegalArgumentException.class, () -> new AuthEntityOrganization(roleName));
    }

    @Test
    void givenAccessRole_whenInititiate_thenOk() throws Exception {
        String roleName = format("org_%s_%s", "test", ACCESS_ROLE);
        AuthEntityOrganization expected = new AuthEntityOrganization();
        expected.setOrganization("test");
        expected.setRole(ACCESS_ROLE);
        JSONAssert.assertEquals(objectMapper.writeValueAsString(expected), objectMapper.writeValueAsString(new AuthEntityOrganization(roleName)), false);
    }

    @Test
    void givenAdminRole_whenInitiate_thenOk() throws Exception {
        String roleName = format("org_%s_%s", "test", ADMIN_ROLE);
        AuthEntityOrganization expected = new AuthEntityOrganization();
        expected.setOrganization("test");
        expected.setRole(ADMIN_ROLE);
        JSONAssert.assertEquals(objectMapper.writeValueAsString(expected), objectMapper.writeValueAsString(new AuthEntityOrganization(roleName)), false);
    }

}
