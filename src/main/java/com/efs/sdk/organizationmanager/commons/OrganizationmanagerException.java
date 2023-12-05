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
package com.efs.sdk.organizationmanager.commons;

import org.springframework.http.HttpStatus;

/**
 * Custom exception for errors in the organizationmanager service.
 *
 * @author e:fs TechHub GmbH
 */
public class OrganizationmanagerException extends Exception {

    private final HttpStatus httpStatus;
    private final int errorCode;

    public OrganizationmanagerException(ORGANIZATIONMANAGER_ERROR error) {
        super(error.msg);
        httpStatus = error.status;
        errorCode = error.code;
    }

    public OrganizationmanagerException(ORGANIZATIONMANAGER_ERROR error, String additionalMessage) {
        super(error.msg + " " + additionalMessage);
        httpStatus = error.status;
        errorCode = error.code;
    }

    /**
     * Provides the errors to the application.
     *
     * @author e:fs TechHub GmbH
     */
    public enum ORGANIZATIONMANAGER_ERROR {
        SAVE_PROVIDE_ID(10001, HttpStatus.METHOD_NOT_ALLOWED,
                "POST method is only allowed for new objects. Use PUT for existing entities instead."),
        SAVE_REQUIRED_INFO_MISSING(10011, HttpStatus.NOT_ACCEPTABLE,
                "A name has to be provided for creating new organizations or spaces."),
        SAVE_ORGANIZATION_NAME_FOUND(10012, HttpStatus.CONFLICT,
                "An organization with the given name already exists."),
        SAVE_SPACE_NAME_FOUND(10013, HttpStatus.CONFLICT,
                "A space with the given name already exists in organization"),
        GET_SINGLE_NOT_FOUND(10020, HttpStatus.NOT_FOUND, "Requested organization does not exist."),
        GET_SINGLE_SPACE_NOT_FOUND(10021, HttpStatus.NOT_FOUND, "space does not exist."),
        INVALID_NAME(10022, HttpStatus.BAD_REQUEST,
                "Invalid name provided, be aware that the name must apply to the following regular expression"),
        NO_ACCESS_TO_ORGANIZATION(10025, HttpStatus.FORBIDDEN, "You do not have access to get organization"),
        NO_ACCESS_TO_SPACE(10026, HttpStatus.FORBIDDEN, "You do not have access to get space"),
        RENAMING_OBJECT_FORBIDDEN(10027, HttpStatus.FORBIDDEN, "renaming the object is forbidden"),
        FORBIDDEN(10028, HttpStatus.FORBIDDEN, "The requested action is forbidden"),
        UNKNOWN_RIGHT(10031, HttpStatus.BAD_REQUEST, "unknown right."),

        UNABLE_CREATE_ROLE(20001, HttpStatus.BAD_REQUEST, "unable to create role"),
        UNABLE_DELETE_ROLE(20011, HttpStatus.BAD_REQUEST, "unable to delete role"),
        UNABLE_GET_ROLE(20021, HttpStatus.BAD_REQUEST, "unable to get role"),
        UNABLE_GET_TOKEN(20022, HttpStatus.BAD_REQUEST, "unable to retrieve token for user"),
        UNABLE_GET_USERS(20023, HttpStatus.BAD_REQUEST, "unable to retrieve users for role"),
        UNABLE_ASSIGN_ROLE(20024, HttpStatus.BAD_REQUEST, "unable to assign role to user "),
        UNABLE_WITHDRAW_ROLE(20025, HttpStatus.BAD_REQUEST, "unable to withdraw role from user"),
        UNABLE_GET_USER(20026, HttpStatus.BAD_REQUEST, "unable to retrieve user"),
        UNABLE_FIND_USERREQUEST(20030, HttpStatus.NOT_FOUND, "unable to find given userrequest"),
        CONFLICTING_IDS_PROVIDED(20031, HttpStatus.CONFLICT, "conflicting object-ids provided"),
        VALIDATION_ERROR(40000, HttpStatus.BAD_REQUEST, "dto validation error."),
        UNABLE_DELETE_ORGA(40001, HttpStatus.BAD_REQUEST, "could not delete organization."),
        BAD_REQUEST_PARAM_VALUE(40002, HttpStatus.BAD_REQUEST, "bad request param value."),

        UNKNOWN_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "something unexpected happened."),
        DOWNSTREAM_ERROR(50200, HttpStatus.BAD_GATEWAY, "downstream error: "),
        METADATA_SERVICE_ERROR(50201, HttpStatus.BAD_GATEWAY, "connection error [metadataservice]"),
        STORAGEMANAGER_SERVICE_ERROR(50202, HttpStatus.BAD_GATEWAY, "connection error [storagemanager]");

        private final int code;
        private final HttpStatus status;
        private final String msg;

        ORGANIZATIONMANAGER_ERROR(int code, HttpStatus status, String msg) {
            this.code = code;
            this.status = status;
            this.msg = msg;
        }

    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
