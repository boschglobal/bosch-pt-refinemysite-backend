/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

enum class RoleConstants {
    ADMIN,

    USER;

    fun roleName(): String {
        return "ROLE_$name"
    }
}