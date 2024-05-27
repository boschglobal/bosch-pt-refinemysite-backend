/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.shared.model.dto

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import java.math.BigDecimal
import java.util.Date

class DayCardDto(
    val identifier: DayCardId,
    val version: Long,
    val title: String,
    val manpower: BigDecimal,
    val notes: String?,
    val status: DayCardStatusEnum,
    val reason: DayCardReasonEnum?,
    val createdByIdentifier: UserId,
    val createdDate: Date?,
    // Day card modified by user information
    val lastModifiedByIdentifier: UserId,
    val lastModifiedDate: Date?,
    // Task Information
    val taskScheduleTaskIdentifier: TaskId,
    val taskScheduleTaskName: String,
    // Project Information
    val taskScheduleTaskProjectIdentifier: ProjectId
)
