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
package com.efs.sdk.organizationmanager.core.organization.model;

import com.efs.sdk.common.domain.model.Confidentiality;
import com.efs.sdk.common.domain.model.State;
import com.efs.sdk.organizationmanager.core.model.AppConfiguration;
import com.efs.sdk.organizationmanager.core.model.Tag;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.efs.sdk.common.domain.model.Confidentiality.INTERNAL;
import static com.efs.sdk.common.domain.model.State.OPEN;


@Entity
@Table(name = "organization")
public class Organization {

    /**
     * <a href="https://docs.microsoft.com/en-us/azure/azure-resource-manager/management/resource-name-rules#microsoftstorage">...</a>
     */
    public static final String REGEX_NAME = "[a-z0-9]{3,24}";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Basic(optional = false)
    @Column(name = "id", unique = true, nullable = false)
    private long id = -1L;

    @NotBlank
    @Column(columnDefinition = "text", unique = true)
    private String name;
    @Column(columnDefinition = "text")
    private String description;
    @Column
    private Confidentiality confidentiality = INTERNAL;
    @Column
    private ZonedDateTime created;

    @OneToMany(cascade = {CascadeType.ALL})
    private List<Tag> tags = new ArrayList<>();

    @Column
    private State state = OPEN;

    @Column
    private String company;

    @ElementCollection
    private List<String> owners = new ArrayList<>();

    @Column
    private String displayName;

    @Column
    private ZonedDateTime modified;

    @OneToMany(cascade = {CascadeType.ALL})
    private List<AppConfiguration> appConfigs = new ArrayList<>();

    public void addAppConfig(AppConfiguration appConfig) {
        this.appConfigs.add(appConfig);
    }

    public void addOwner(String owner) {
        this.owners.add(owner);
    }

    public void addTag(Tag tag) {
        this.tags.add(tag);
    }

    public List<AppConfiguration> getAppConfigs() {
        return Collections.unmodifiableList(appConfigs);
    }

    public void setAppConfigs(List<AppConfiguration> appConfigs) {
        this.appConfigs.clear();
        this.appConfigs.addAll(appConfigs);
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public Confidentiality getConfidentiality() {
        return confidentiality;
    }

    public void setConfidentiality(Confidentiality confidentiality) {
        this.confidentiality = confidentiality;
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    public void setCreated(ZonedDateTime created) {
        this.created = created;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ZonedDateTime getModified() {
        return modified;
    }

    public void setModified(ZonedDateTime modified) {
        this.modified = modified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getOwners() {
        return Collections.unmodifiableList(owners);
    }

    public void setOwners(List<String> owners) {
        this.owners.clear();
        this.owners.addAll(owners);
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public List<Tag> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public void setTags(List<Tag> tags) {
        this.tags.clear();
        this.tags.addAll(tags);
    }
}
