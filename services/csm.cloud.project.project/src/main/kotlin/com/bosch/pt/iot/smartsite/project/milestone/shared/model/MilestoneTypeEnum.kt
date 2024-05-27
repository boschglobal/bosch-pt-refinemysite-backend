/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.milestone.shared.model

enum class MilestoneTypeEnum {

  /*
   * attention: do not rearrange these fields or add a new enum in between! Currently, we do store
   * the enum position in the database and not the enum name.
   */

  CRAFT,
  INVESTOR,
  PROJECT;

  companion object {
    const val ENUM_VALUES = "CRAFT,INVESTOR,PROJECT"
  }
}
