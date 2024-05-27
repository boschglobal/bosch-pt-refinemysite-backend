/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.util

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleSlotAvro
import java.time.LocalDate
import java.time.ZoneOffset.UTC

fun AggregateIdentifierAvro.asSlot(date: LocalDate): TaskScheduleSlotAvro =
    TaskScheduleSlotAvro.newBuilder()
        .setDate(date.atStartOfDay().toInstant(UTC).toEpochMilli())
        .setDayCard(this)
        .build()
