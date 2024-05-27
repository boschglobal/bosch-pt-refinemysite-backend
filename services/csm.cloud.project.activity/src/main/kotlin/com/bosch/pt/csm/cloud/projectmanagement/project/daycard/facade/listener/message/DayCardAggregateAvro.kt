/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Context
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.daycard.message.getTaskIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.model.DayCard
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.model.DayCardStatusEnum
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import java.util.UUID

fun DayCardAggregateG2Avro.toEntity(projectIdentifier: UUID) =
    DayCard(
        identifier = buildAggregateIdentifier(),
        projectIdentifier = projectIdentifier,
        taskIdentifier = getTaskIdentifier(),
        status = DayCardStatusEnum.valueOf(getStatus().name),
        title = getTitle(),
        manpower = getManpower().stripTrailingZeros(),
        notes = getNotes(),
        reason = getReason()?.run { DayCardReasonEnum.valueOf(getReason().name) })

fun DayCardAggregateG2Avro.buildContext(projectIdentifier: UUID) =
    Context(project = projectIdentifier, task = getTaskIdentifier())
