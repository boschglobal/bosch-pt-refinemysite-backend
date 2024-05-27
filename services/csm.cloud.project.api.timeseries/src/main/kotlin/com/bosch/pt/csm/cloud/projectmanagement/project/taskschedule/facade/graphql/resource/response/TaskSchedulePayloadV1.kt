/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.graphql.resource.response

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TaskSchedulePayloadV1(
    val id: UUID,
    val version: Long,
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val eventDate: LocalDateTime
)
