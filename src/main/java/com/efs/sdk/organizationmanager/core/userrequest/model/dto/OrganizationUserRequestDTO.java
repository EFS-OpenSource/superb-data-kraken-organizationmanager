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
package com.efs.sdk.organizationmanager.core.userrequest.model.dto;

import com.efs.sdk.organizationmanager.core.userrequest.model.UserRequestState;
import com.efs.sdk.organizationmanager.helper.RoleHelper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.ZonedDateTime;

public class OrganizationUserRequestDTO extends OrganizationUserRequestCreateDTO {

    @Schema(description = "creation-timestamp")
    private ZonedDateTime created;
    @Schema(description = "id of the userrequest")
    private long id = -1L;
    @Schema(description = "timestamp of last update")
    private ZonedDateTime modified;

    @Schema(description = "role the user is requesting - defaults to &quot;ACCESS&quot;")
    private RoleHelper.OrganizationScopeRole role = RoleHelper.OrganizationScopeRole.ACCESS;

    @Schema(description = "state of the userrequest - defaults to &quot;OPEN&quot;")
    private UserRequestState state = UserRequestState.OPEN;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    public void setCreated(ZonedDateTime created) {
        this.created = created;
    }

    public ZonedDateTime getModified() {
        return modified;
    }

    public void setModified(ZonedDateTime modified) {
        this.modified = modified;
    }

    @Override
    public RoleHelper.OrganizationScopeRole getRole() {
        return role;
    }

    @Override
    public void setRole(RoleHelper.OrganizationScopeRole role) {
        this.role = role;
    }

    public UserRequestState getState() {
        return state;
    }

    public void setState(UserRequestState state) {
        this.state = state;
    }
}
