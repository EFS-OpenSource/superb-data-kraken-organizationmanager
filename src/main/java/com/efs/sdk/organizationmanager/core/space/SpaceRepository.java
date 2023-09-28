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
package com.efs.sdk.organizationmanager.core.space;

import com.efs.sdk.common.domain.model.Confidentiality;
import com.efs.sdk.organizationmanager.core.space.model.Space;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpaceRepository extends JpaRepository<Space, Long> {
    Optional<Space> findByOrganizationIdAndId(Long orgaId, Long id);

    Optional<Space> findByOrganizationIdAndName(Long orgaId, String name);

    List<Space> findByOrganizationId(Long orgaId);

    List<Space> findByOrganizationIdAndNameIn(Long orgaId, Collection<String> names);

    List<Space> findByOrganizationIdAndConfidentiality(Long orgaId, Confidentiality confidentiality);
}
