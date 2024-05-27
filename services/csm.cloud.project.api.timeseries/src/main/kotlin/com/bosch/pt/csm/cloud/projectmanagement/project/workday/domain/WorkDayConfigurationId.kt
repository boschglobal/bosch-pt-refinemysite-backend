/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.domain

import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

data class WorkDayConfigurationId(@get:JsonValue val value: UUID) {

  override fun toString() = value.toString()
}

fun UUID.asWorkDayConfigurationId() = WorkDayConfigurationId(this)
