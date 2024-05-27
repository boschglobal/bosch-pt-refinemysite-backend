/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.domain

import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

data class DayCardId(@get:JsonValue val value: UUID) {

  override fun toString() = value.toString()
}

fun UUID.asDayCardId() = DayCardId(this)
