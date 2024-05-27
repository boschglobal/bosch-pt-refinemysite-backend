/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workday.facade.graphql.resource.response

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class WorkDayConfigurationPayloadV1(
    val id: UUID,
    val version: Long,
    val startOfWeek: String,
    val workingDays: List<String>,
    val holidays: List<HolidayPayloadV1>,
    val allowWorkOnNonWorkingDays: Boolean,
    val eventDate: LocalDateTime
)

data class HolidayPayloadV1(val name: String, val date: LocalDate)
