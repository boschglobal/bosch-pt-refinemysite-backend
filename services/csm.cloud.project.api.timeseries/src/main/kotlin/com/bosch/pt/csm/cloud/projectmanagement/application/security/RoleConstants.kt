/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

enum class RoleConstants {
  ADMIN,
  USER;

  fun roleName() = "ROLE_$name"
}
