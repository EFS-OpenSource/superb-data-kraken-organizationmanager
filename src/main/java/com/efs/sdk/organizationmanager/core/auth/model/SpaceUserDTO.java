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
package com.efs.sdk.organizationmanager.core.auth.model;

import com.efs.sdk.organizationmanager.helper.RoleHelper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

public class SpaceUserDTO extends UserDTO {
    @Schema(description = "List of users space-permissions - List of SpaceScopeRole")
    private final List<RoleHelper.SpaceScopeRole> permissions = new ArrayList<>();


    public List<RoleHelper.SpaceScopeRole> getPermissions() {
        return permissions;
    }

    public void addPermission(RoleHelper.SpaceScopeRole permission) {
        this.permissions.add(permission);
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
