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

import static java.lang.String.join;

/**
 * Enum for storing right-role-mappings
 *
 * @author e:fs TechHub GmbH
 */
public enum AuthConfiguration {

    /**
     * Instance for read-right
     */
    READ(new String[]{"user", "supplier", "trustee"}),
    /**
     * Instance for write-right
     */
    WRITE(new String[]{"supplier", "trustee"}),
    /**
     * Instance for delete-right
     */
    DELETE(new String[]{"trustee"}),
    /**
     * Instance for get-/list-right
     */
    GET(new String[]{"user", "supplier", "trustee"});

    private final String[] allowedRoles;

    /**
     * Constructor.
     *
     * @param allowedRoles The roles assigned to given right.
     */
    AuthConfiguration(String[] allowedRoles) {
        this.allowedRoles = allowedRoles != null ? allowedRoles.clone() : new String[0];
    }

    /**
     * Gets the assigned roles.
     *
     * @return the assigned roles.
     */
    public String[] getAllowedRoles() {
        return allowedRoles;
    }

    public static String getRegex(AuthConfiguration instance) {
        return join("|", instance.getAllowedRoles());
    }
}
