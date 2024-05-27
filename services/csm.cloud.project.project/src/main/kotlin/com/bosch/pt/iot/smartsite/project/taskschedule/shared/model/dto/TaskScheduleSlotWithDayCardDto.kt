/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.TaskScheduleId
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Date

class TaskScheduleSlotWithDayCardDto(
    // Schedule information
    val identifier: TaskScheduleId,
    // Slot information
    val slotsDate: LocalDate,
    // Day card information
    val slotsDayCardIdentifier: DayCardId,
    val slotsDayCardVersion: Long,
    val slotsDayCardTitle: String,
    val slotsDayCardManpower: BigDecimal,
    val slotsDayCardNotes: String?,
    val slotsDayCardStatus: DayCardStatusEnum,
    val slotsDayCardReason: DayCardReasonEnum?,
    // Day card Create User information
    val slotsDayCardCreatedBy: UserId?,
    val slotsDayCardCreatedDate: Date?,
    // Day card Modify User information
    val slotsDayCardLastModifiedBy: UserId?,
    val slotsDayCardLastModifiedDate: Date?
)
