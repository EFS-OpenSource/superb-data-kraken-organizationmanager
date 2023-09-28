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
package com.efs.sdk.organizationmanager.core.userrequest.model;

import com.efs.sdk.organizationmanager.helper.RoleHelper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class SpaceUserRequest extends UserRequest {

    @Column
    private Long orgaId;

    @Column
    private Long spaceId;

    @Column
    private RoleHelper.SpaceScopeRole role;

    public Long getOrgaId() {
        return orgaId;
    }

    public void setOrgaId(Long orgaId) {
        this.orgaId = orgaId;
    }

    public Long getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(Long spaceId) {
        this.spaceId = spaceId;
    }

    public RoleHelper.SpaceScopeRole getRole() {
        return role;
    }

    public void setRole(RoleHelper.SpaceScopeRole role) {
        this.role = role;
    }
}
