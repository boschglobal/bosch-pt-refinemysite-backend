/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topic.shared.model

enum class TopicCriticalityEnum(val i18nKey: String) {
  CRITICAL("TopicCriticalityEnum_Critical"),
  UNCRITICAL("TopicCriticalityEnum_Uncritical");

  companion object {
    const val ENUM_VALUES = "CRITICAL,UNCRITICAL"
  }
}
