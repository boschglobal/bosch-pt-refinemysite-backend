/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.user.user.model

/** Enumeration for user gender. */
enum class GenderEnum {
  /** Male gender. */
  MALE,

  /** Female gender. */
  FEMALE;

  companion object {
    /** Valid enum values for documentation. */
    const val ENUM_VALUES = "MALE,FEMALE"
  }
}
