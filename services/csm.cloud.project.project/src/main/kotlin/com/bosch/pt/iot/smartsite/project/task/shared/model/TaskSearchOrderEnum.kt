/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.shared.model

enum class TaskSearchOrderEnum(val property: String) {
  NAME("name"),
  LOCATION("location"),
  START("start"),
  END("end"),
  PROJECT_CRAFT("projectCraft"),
  COMPANY("company"),
  STATUS("status"),
  WORK_AREA("workArea"),
  TOPIC("topic");

  companion object {

    fun of(property: String): TaskSearchOrderEnum {
      for (field in values()) {
        if (property == field.property) {
          return field
        }
      }

      throw IllegalArgumentException("No such property: $property")
    }
  }
}
