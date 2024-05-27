/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.graphql.resource.response

import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class DayCardPayloadV1(
    val id: UUID,
    val version: Long,
    val status: String,
    val title: String,
    val manpower: BigDecimal,
    val notes: String? = null,
    val reason: DayCardReasonPayloadV1? = null,
    val eventDate: LocalDateTime,

    // Additional attributes only for internal querying
    val taskId: TaskId,
)

data class DayCardReasonPayloadV1(val key: String, val displayName: String)
