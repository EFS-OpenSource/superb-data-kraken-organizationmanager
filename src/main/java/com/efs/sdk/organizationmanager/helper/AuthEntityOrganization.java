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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.lang.String.join;

public class AuthEntityOrganization {

    public static final String ADMIN_ROLE = "admin";
    public static final String ACCESS_ROLE = "access";

    private static final String GROUPNAME_ORGA = "orgaName";
    private static final String GROUPNAME_ROLE = "roleName";
    public static final String ORGA_REGEX = format("^(org_)(?<%s>%s)_(?<%s>%s)$", GROUPNAME_ORGA, Organization.REGEX_NAME, GROUPNAME_ROLE, join("|",
            ADMIN_ROLE, ACCESS_ROLE));
    private static final Pattern ORGA_PATTERN = Pattern.compile(ORGA_REGEX);

    private String organization;
    private String role;

    public AuthEntityOrganization(String roleName) {
        Matcher m = ORGA_PATTERN.matcher(roleName);
        // according to the filter the role must match the pattern
        if (!m.matches()) {
            throw new IllegalArgumentException("no role found!");
        }
        setOrganization(m.group(GROUPNAME_ORGA));
        setRole(m.group(GROUPNAME_ROLE));
    }

    public AuthEntityOrganization() {
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getOrganization() {
        return organization;
    }

    public String getRole() {
        return role;
    }

}
