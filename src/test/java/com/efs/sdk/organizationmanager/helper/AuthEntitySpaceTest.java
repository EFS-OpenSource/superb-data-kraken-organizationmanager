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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthEntitySpaceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void givenInvalidRole_whenInitiate_thenError() {
        String roleName = "something-other_than_expected";
        assertThrows(IllegalArgumentException.class, () -> new AuthEntitySpace(roleName));
    }

    @ParameterizedTest(name = "given{0}Role_whenInitiate_thenOk")
    @ValueSource(strings = {"user", "supplier", "trustee"})
    void givenRole_whenInitiate_thenOk(String role) throws Exception {
        String roleName = format("%s_%s_%s", "test", "test", role);
        AuthEntitySpace expected = new AuthEntitySpace();
        expected.setOrganization("test");
        expected.setSpace("test");
        expected.setRole(role);
        JSONAssert.assertEquals(objectMapper.writeValueAsString(expected), objectMapper.writeValueAsString(new AuthEntitySpace(roleName)), false);
    }
}
