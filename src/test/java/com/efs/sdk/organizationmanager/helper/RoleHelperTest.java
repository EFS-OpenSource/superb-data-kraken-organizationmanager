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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

class RoleHelperTest {

    private RoleHelper roleHelper;

    @BeforeEach
    public void setup() {
        this.roleHelper = new RoleHelper();
    }

    @Test
    void givenSpace_whenGetRolesForSpace_thenSpaceRoles() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        Space space = new Space();
        space.setName("test");
        space.setId(1L);
        space.setOrganizationId(orga.getId());

        List<String> actual = roleHelper.getRoles(orga, space);
        assertThat(actual, contains(format("%s_%s_user", orga.getName(), space.getName()), format("%s_%s_supplier", orga.getName(), space.getName()), format(
                "%s_%s_trustee", orga.getName(), space.getName())));
    }

    @Test
    void givenOrganization_whenGetRolesForOrganization_thenOrganizationRoles() {
        Organization orga = new Organization();
        orga.setId(1L);
        orga.setName("test");

        List<String> actual = roleHelper.getRoles(orga);
        assertThat(actual, contains(format("org_%s_access", orga.getName()), format("org_%s_admin", orga.getName()), format("org_%s_trustee", orga.getName())));
    }

}
