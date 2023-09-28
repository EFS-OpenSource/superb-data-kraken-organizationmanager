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

import org.apache.commons.lang.ArrayUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthConfigurationTest {

    @ParameterizedTest(name = "given_{0}_when{1}_then{2}")
    @CsvSource({
            "user, READ, true",
            "user, WRITE, false",
            "user, DELETE, false",
            "supplier, READ, true",
            "supplier, WRITE, true",
            "supplier, DELETE, false",
            "trustee, READ, true",
            "trustee, WRITE, true",
            "trustee, DELETE, true"
    })
    void testAllowedRoles(String role, AuthConfiguration config, boolean expectedResult) {
        String[] allowedRoles = config.getAllowedRoles();
        assertEquals(expectedResult, ArrayUtils.contains(allowedRoles, role));
    }
}
