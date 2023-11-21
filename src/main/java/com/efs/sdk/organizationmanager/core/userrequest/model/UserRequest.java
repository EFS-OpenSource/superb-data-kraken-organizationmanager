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

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.ZonedDateTime;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class UserRequest {

    @Id
    @SequenceGenerator(
            name = "user_request_seq_generator",
            sequenceName = "user_request_seq",
            allocationSize = 50
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "user_request_seq_generator"
    )
    private long id;

    @NotBlank
    @Column(columnDefinition = "text")
    private String userId;

    @Column
    private ZonedDateTime created;

    @Column
    private ZonedDateTime modified;

    @Column
    private UserRequestState state = UserRequestState.OPEN;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public UserRequestState getState() {
        return state;
    }

    public void setState(UserRequestState state) {
        this.state = state;
    }
}
