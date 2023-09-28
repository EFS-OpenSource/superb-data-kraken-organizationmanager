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

import static com.efs.sdk.organizationmanager.helper.AuthConfiguration.*;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationModelTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void givenNoSpaces_whenGetSpacesByPermission_thenEmpty() throws Exception {
        AuthenticationModel model = new AuthenticationModel();
        JSONAssert.assertEquals(objectMapper.writeValueAsString(new String[0]), objectMapper.writeValueAsString(model.getSpacesByPermission(READ)), false);
    }

    @Test
    void givenREADRequest_whenGetSpacesByPermission_thenUser() throws Exception {
        String orgaName = "testorga";
        String spaceName = "testspace";
        AuthenticationModel model = new AuthenticationModel();
        model.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_user", orgaName, spaceName))});
        JSONAssert.assertEquals(objectMapper.writeValueAsString(new String[]{spaceName}), objectMapper.writeValueAsString(model.getSpacesByPermission(READ)),
                false);
    }

    @Test
    void givenWRITERequest_whenGetSpacesByPermission_thenSupplier() throws Exception {
        String orgaName = "testorga";
        String spaceName = "testspace";
        AuthenticationModel model = new AuthenticationModel();
        model.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_supplier", orgaName, spaceName))});
        JSONAssert.assertEquals(objectMapper.writeValueAsString(new String[]{spaceName}), objectMapper.writeValueAsString(model.getSpacesByPermission(WRITE))
                , false);
    }

    @Test
    void givenWRITERequestUserRole_whenGetSpacesByPermission_thenEmpty() throws Exception {
        String orgaName = "testorga";
        String spaceName = "testspace";
        AuthenticationModel model = new AuthenticationModel();
        model.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_user", orgaName, spaceName))});
        JSONAssert.assertEquals(objectMapper.writeValueAsString(new String[0]), objectMapper.writeValueAsString(model.getSpacesByPermission(WRITE)), false);
    }

    @Test
    void givenDELETERequest_whenGetSpacesByPermission_thenTrustee() throws Exception {
        String orgaName = "testorga";
        String spaceName = "testspace";
        AuthenticationModel model = new AuthenticationModel();
        model.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_trustee", orgaName, spaceName))});
        JSONAssert.assertEquals(objectMapper.writeValueAsString(new String[]{spaceName}),
                objectMapper.writeValueAsString(model.getSpacesByPermission(DELETE)), false);
    }

    @Test
    void givenGETRequest_whenGetSpacesByPermission_thenUser() throws Exception {
        String orgaName = "testorga";
        String spaceName = "testspace";
        AuthenticationModel model = new AuthenticationModel();
        model.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_user", orgaName, spaceName))});
        JSONAssert.assertEquals(objectMapper.writeValueAsString(new String[]{spaceName}), objectMapper.writeValueAsString(model.getSpacesByPermission(GET)),
                false);
    }

    @Test
    void givenREADRequest_whenGetOrganizationsByPermission_thenOk() throws Exception {
        String orgaName = "testorga";
        String spaceName = "testspace";
        AuthenticationModel model = new AuthenticationModel();
        model.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_user", orgaName, spaceName))});
        model.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orgaName, AuthEntityOrganization.ACCESS_ROLE))});
        JSONAssert.assertEquals(objectMapper.writeValueAsString(new String[]{orgaName}),
                objectMapper.writeValueAsString(model.getOrganizationsByPermission(READ)), false);
    }

    @Test
    void givenWRITERequest_whenGetOrganizationsByPermission_thenOk() throws Exception {
        String orgaName = "testorga";
        String spaceName = "testspace";
        AuthenticationModel model = new AuthenticationModel();
        model.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_supplier", orgaName, spaceName))});
        model.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orgaName, AuthEntityOrganization.ACCESS_ROLE))});
        JSONAssert.assertEquals(objectMapper.writeValueAsString(new String[]{orgaName}),
                objectMapper.writeValueAsString(model.getOrganizationsByPermission(WRITE)), false);
    }

    @Test
    void givenDELETERequest_whenGetOrganizationsByPermission_thenOk() throws Exception {
        String orgaName = "testorga";
        String spaceName = "testspace";
        AuthenticationModel model = new AuthenticationModel();
        model.setSpaces(new AuthEntitySpace[]{new AuthEntitySpace(format("%s_%s_trustee", orgaName, spaceName))});
        model.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization(format("org_%s_%s", orgaName, AuthEntityOrganization.ACCESS_ROLE))});
        JSONAssert.assertEquals(objectMapper.writeValueAsString(new String[]{orgaName}),
                objectMapper.writeValueAsString(model.getOrganizationsByPermission(DELETE)), false);
    }

    @Test
    void givenNoOrgas_whenIsAdmin_thenFalse() {
        AuthenticationModel model = new AuthenticationModel();
        assertFalse(model.isAdmin("orga"));
    }

    @Test
    void givenAdmin_whenIsAdmin_thenTrue() {
        AuthenticationModel model = new AuthenticationModel();
        model.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization("org_test_admin")});
        assertTrue(model.isAdmin("test"));
    }

    @Test
    void givenAccess_whenIsAdmin_thenFalse() {
        AuthenticationModel model = new AuthenticationModel();
        model.setOrganizations(new AuthEntityOrganization[]{new AuthEntityOrganization("org_test_access")});
        assertFalse(model.isAdmin("test"));
    }
}
