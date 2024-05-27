/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro

fun TaskScheduleAggregateAvro.getLastModifiedDate() =
    getAuditingInformation().getLastModifiedDate().toInstantByMillis()

fun TaskScheduleAggregateAvro.getLastModifiedByUserIdentifier() =
    getAuditingInformation().getLastModifiedBy().getIdentifier().toUUID()

fun TaskScheduleAggregateAvro.getTaskIdentifier() = getTask().getIdentifier().toUUID()

fun TaskScheduleAggregateAvro.getTaskVersion(): Long = getTask().getVersion()

fun TaskScheduleAggregateAvro.getIdentifier() = getAggregateIdentifier().getIdentifier().toUUID()

fun TaskScheduleAggregateAvro.getVersion(): Long = getAggregateIdentifier().getVersion()

fun TaskScheduleAggregateAvro.buildAggregateIdentifier() =
    getAggregateIdentifier().buildAggregateIdentifier()
