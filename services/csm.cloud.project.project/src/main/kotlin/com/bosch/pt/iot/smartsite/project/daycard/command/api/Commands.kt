/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.command.api

import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import java.math.BigDecimal

data class CreateDayCardCommand(
    val identifier: DayCardId,
    val taskIdentifier: TaskId,
    val title: String?,
    val manpower: BigDecimal,
    val notes: String?,
    val status: DayCardStatusEnum? = null,
    val reason: DayCardReasonEnum? = null
)

data class UpdateDayCardCommand(
    val identifier: DayCardId,
    val title: String?,
    val manpower: BigDecimal,
    val notes: String?,
    val eTag: ETag
)

data class ApproveDayCardCommand(val identifier: DayCardId, val eTag: ETag)

data class CancelDayCardCommand(
    val identifier: DayCardId,
    val reason: DayCardReasonEnum?,
    val eTag: ETag
)

data class CompleteDayCardCommand(val identifier: DayCardId, val eTag: ETag)

data class ResetDayCardCommand(val identifier: DayCardId, val eTag: ETag)

data class DeleteDayCardCommand(val identifier: DayCardId, val scheduleETag: ETag)

data class DeleteDayCardsFromScheduleCommand(
    val identifiers: Set<DayCardId>,
    val scheduleETag: ETag
)
