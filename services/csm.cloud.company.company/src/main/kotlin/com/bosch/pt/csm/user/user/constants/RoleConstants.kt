/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.user.user.constants

enum class RoleConstants {
  ADMIN, USER;

  fun roleName(): String {
    return "ROLE_$name"
  }
}