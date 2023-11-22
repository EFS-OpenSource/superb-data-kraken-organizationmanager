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
package com.efs.sdk.organizationmanager.core.space.model;

import com.efs.sdk.common.domain.model.Capability;
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


@Entity
@Table(name = "space", uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "name"}))
public class Space {

    /**
     * <a href="https://docs.microsoft.com/en-us/azure/azure-resource-manager/management/resource-name-rules#microsoftstorage">...</a>
     */
    public static final String REGEX_NAME = "[a-z0-9-]{3,63}";
    public static final String SPACE_LOADINGZONE = "loadingzone";

    @OneToMany(cascade = {CascadeType.ALL})
    private final List<Tag> tags = new ArrayList<>();
    @ElementCollection
    @Enumerated(EnumType.STRING)
    private final List<Capability> capabilities = new ArrayList<>();
    @ElementCollection
    private final List<String> owners = new ArrayList<>();
    @OneToMany(cascade = {CascadeType.ALL})
    private final List<AppConfiguration> appConfigs = new ArrayList<>();
    @Id
    @SequenceGenerator(
            name = "space_seq_generator",
            sequenceName = "space_seq",
            allocationSize = 50
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "space_seq_generator"
    )
    @Column(name = "id")
    private long id;
    @NotBlank
    @Column(columnDefinition = "text")
    private String name;
    @Column(columnDefinition = "text")
    private String identifier;
    @Column
    private int defaultRetentionTime;
    @Column
    private State state;
    @Column(columnDefinition = "text")
    private String description;
    @Column
    private Confidentiality confidentiality = INTERNAL;
    @Column
    private ZonedDateTime created;
    @Column(name = "organization_id")
    private Long organizationId;
    @Column(name = "metadata_generate")
    private Boolean metadataGenerate = Boolean.FALSE;

    @Column
    private String displayName;
    @Column
    private String schemaRef;

    @Column
    private String descriptionRef;
    @Column
    private String metadataIndexName;
    @Column
    private ZonedDateTime modified;
    @Column
    private boolean gdprRelevant = false;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public int getDefaultRetentionTime() {
        return defaultRetentionTime;
    }

    public void setDefaultRetentionTime(int defaultRetentionTime) {
        this.defaultRetentionTime = defaultRetentionTime;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public boolean isMetadataGenerate() {
        return metadataGenerate == null ? Boolean.FALSE : metadataGenerate;
    }

    public void setMetadataGenerate(boolean metadataGenerate) {
        this.metadataGenerate = metadataGenerate;
    }

    public List<Tag> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public void setTags(List<Tag> tags) {
        this.tags.clear();
        this.tags.addAll(tags);
    }

    public void addTag(Tag tag) {
        this.tags.add(tag);
    }

    public List<String> getOwners() {
        return Collections.unmodifiableList(owners);
    }

    public void setOwners(List<String> owners) {
        this.owners.clear();
        this.owners.addAll(owners);
    }

    public void addOwner(String owner) {
        this.owners.add(owner);
    }

    public List<AppConfiguration> getAppConfigs() {
        return Collections.unmodifiableList(appConfigs);
    }

    public void setAppConfigs(List<AppConfiguration> appConfigs) {
        this.appConfigs.clear();
        this.appConfigs.addAll(appConfigs);
    }

    public void addAppConfig(AppConfiguration appConfig) {
        this.appConfigs.add(appConfig);
    }

    public List<Capability> getCapabilities() {
        return Collections.unmodifiableList(capabilities);
    }

    public void setCapabilities(List<Capability> capabilities) {
        this.capabilities.clear();
        this.capabilities.addAll(capabilities.stream().distinct().toList()); // add distinct list
    }

    public void addCapability(Capability capability) {
        if (!this.capabilities.contains(capability)) {
            this.capabilities.add(capability);
        }
    }

    public void removeCapability(Capability capability) {
        if (!this.capabilities.contains(capability)) {
            this.capabilities.remove(capability);
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getSchemaRef() {
        return schemaRef;
    }

    public void setSchemaRef(String schemaRef) {
        this.schemaRef = schemaRef;
    }

    public String getDescriptionRef() {
        return descriptionRef;
    }

    public void setDescriptionRef(String descriptionRef) {
        this.descriptionRef = descriptionRef;
    }

    public String getMetadataIndexName() {
        return metadataIndexName;
    }

    public void setMetadataIndexName(String metadataIndexName) {
        this.metadataIndexName = metadataIndexName;
    }

    public ZonedDateTime getModified() {
        return modified;
    }

    public void setModified(ZonedDateTime modified) {
        this.modified = modified;
    }

    public boolean isGdprRelevant() {
        return gdprRelevant;
    }

    public void setGdprRelevant(boolean gdprRelevant) {
        this.gdprRelevant = gdprRelevant;
    }
}
