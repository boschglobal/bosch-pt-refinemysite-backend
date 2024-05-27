/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user

object UserConstants {
  const val SYSTEM_USER_ID = "SYSTEM"
}

enum class UserRoleEnum {
  ADMIN,
  USER;

  fun roleName(): String = "ROLE_$name"
}

enum class GenderEnum {
  MALE,
  FEMALE;

  companion object {

    /** Valid enum values for documentation. */
    const val ENUM_VALUES = "MALE,FEMALE"
  }
}
