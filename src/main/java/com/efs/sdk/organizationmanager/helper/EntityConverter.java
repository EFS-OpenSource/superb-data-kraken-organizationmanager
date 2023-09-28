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

import com.efs.sdk.common.domain.dto.SpaceUpdateDTO;
import com.efs.sdk.organizationmanager.core.organization.model.Organization;
import com.efs.sdk.organizationmanager.core.space.model.Space;
import com.efs.sdk.organizationmanager.core.userrequest.model.OrganizationUserRequest;
import com.efs.sdk.organizationmanager.core.userrequest.model.SpaceUserRequest;
import com.efs.sdk.organizationmanager.core.userrequest.model.dto.OrganizationUserRequestCreateDTO;
import com.efs.sdk.organizationmanager.core.userrequest.model.dto.OrganizationUserRequestDTO;
import com.efs.sdk.organizationmanager.core.userrequest.model.dto.SpaceUserRequestCreateDTO;
import com.efs.sdk.organizationmanager.core.userrequest.model.dto.SpaceUserRequestDTO;
import com.efs.sdk.storage.SpaceBase;
import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Component;

/**
 * implementation for type-conversions
 */
@Component
public class EntityConverter {
    /**
     * Instance of the ModelMapper.
     */
    private final ModelMapper modelMapper;

    /**
     * Constructor.
     *
     * @param modelMapper The ModelMapper
     */
    public EntityConverter(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        this.modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT);
        this.modelMapper.getConfiguration().setPropertyCondition(Conditions.isNotNull());
    }

    public SpaceUpdateDTO convertToDTO(Space entity) {
        return modelMapper.map(entity, SpaceUpdateDTO.class);
    }

    // ******************************************************
    // Organization-conversions
    // ******************************************************

    public <T> T convertToDTO(Organization entity, Class<T> targetType) {
        return modelMapper.map(entity, targetType);
    }

    // ******************************************************
    // Space-conversions
    // ******************************************************

    public <T> T convertToDTO(Space entity, Class<T> targetType) {
        return modelMapper.map(entity, targetType);
    }

    // ******************************************************
    // Organization-UserRequest-conversions
    // ******************************************************
    public OrganizationUserRequestDTO convertToDTO(OrganizationUserRequest entity) {
        return modelMapper.map(entity, OrganizationUserRequestDTO.class);
    }

    // ******************************************************
    // Space-UserRequest-conversions
    // ******************************************************
    public SpaceUserRequestDTO convertToDTO(SpaceUserRequest entity) {
        return modelMapper.map(entity, SpaceUserRequestDTO.class);
    }

    public OrganizationUserRequest convertToEntity(OrganizationUserRequestCreateDTO dto) {
        return modelMapper.map(dto, OrganizationUserRequest.class);
    }

    public SpaceUserRequest convertToEntity(SpaceUserRequestCreateDTO dto) {
        return modelMapper.map(dto, SpaceUserRequest.class);
    }

    // ******************************************************
    // SpaceBase Conversions
    // ******************************************************
    public SpaceBase convertToSerializable(Space entity) {
        return modelMapper.map(entity, SpaceBase.class);
    }


    // ******************************************************
    // Generic Conversions
    // ******************************************************
    public <T, D> D convertToEntity(T dto, Class<D> targetType) {
        return modelMapper.map(dto, targetType);
    }

    public <T, D> D convertToDTO(T entity, Class<D> targetType) {
        return modelMapper.map(entity, targetType);
    }

}