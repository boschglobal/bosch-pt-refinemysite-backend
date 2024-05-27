/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.user.constants

enum class RoleConstants {
  ADMIN,
  USER;

  fun roleName(): String = "ROLE_$name"
}
