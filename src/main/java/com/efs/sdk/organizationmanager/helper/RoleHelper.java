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
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static java.lang.String.format;

@Component
public class RoleHelper {

    public List<String> getRoles(Organization orga, Space space) {
        return Stream.of(SpaceScopeRole.values()).map(roleScope -> buildSpaceRole(orga, space, roleScope)).toList();
    }

    public String buildSpaceRole(Organization orga, Space space, SpaceScopeRole roleScope) {
        return format("%s_%s_%s", orga.getName(), space.getName(), roleScope.name()).toLowerCase(Locale.getDefault());
    }

    public List<String> getRoles(Organization organization) {
        return Stream.of(OrganizationScopeRole.values()).map(scope -> buildOrganizationRole(organization, scope)).toList();
    }

    public String buildOrganizationRole(Organization orga, OrganizationScopeRole scope) {
        return buildOrganizationRole(orga.getName(), scope);
    }

    public String buildOrganizationRole(String orgaName, OrganizationScopeRole scope) {
        return format("org_%s_%s", orgaName, scope.name()).toLowerCase(Locale.getDefault());
    }

    public enum SpaceScopeRole {
        USER, SUPPLIER, TRUSTEE
    }

    public enum OrganizationScopeRole {
        ACCESS, ADMIN, TRUSTEE
    }
}
