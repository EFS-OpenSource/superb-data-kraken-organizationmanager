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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.efs.sdk.organizationmanager.helper.AuthConfiguration.READ;
import static java.lang.String.format;
import static java.lang.String.join;

public class AuthEntitySpace {

    private static final String GROUPNAME_ORGA = "orgaName";
    private static final String GROUPNAME_SPACE = "spaceName";
    private static final String GROUPNAME_ROLE = "roleName";
    public static final String SPACE_REGEX = format("^(?<%s>%s)_(?<%s>%s)_(?<%s>%s)$", GROUPNAME_ORGA, Organization.REGEX_NAME, GROUPNAME_SPACE,
            Space.REGEX_NAME, GROUPNAME_ROLE, join("|", AuthConfiguration.getRegex(READ)));
    private static final Pattern SPACE_PATTERN = Pattern.compile(SPACE_REGEX);

    private String organization;
    private String space;
    private String role;

    public AuthEntitySpace(String roleName) {
        Matcher m = SPACE_PATTERN.matcher(roleName);
        // according to the filter the role must match the pattern
        if (!m.matches()) {
            throw new IllegalArgumentException("no role found!");
        }
        setOrganization(m.group(GROUPNAME_ORGA));
        setSpace(m.group(GROUPNAME_SPACE));
        setRole(m.group(GROUPNAME_ROLE));
    }

    public AuthEntitySpace() {
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setSpace(String space) {
        this.space = space;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getOrganization() {
        return organization;
    }

    public String getSpace() {
        return space;
    }

    public String getRole() {
        return role;
    }

}
