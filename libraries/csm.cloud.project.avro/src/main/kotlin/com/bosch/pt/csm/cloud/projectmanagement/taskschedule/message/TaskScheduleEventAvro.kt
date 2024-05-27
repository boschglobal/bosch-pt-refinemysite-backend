/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.taskschedule.message

import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventAvro

fun TaskScheduleEventAvro.getLastModifiedDate() = getAggregate().getLastModifiedDate()

fun TaskScheduleEventAvro.getLastModifiedByUserIdentifier() =
    getAggregate().getLastModifiedByUserIdentifier()

fun TaskScheduleEventAvro.getTaskIdentifier() = getAggregate().getTaskIdentifier()

fun TaskScheduleEventAvro.getTaskVersion() = getAggregate().getTaskVersion()

fun TaskScheduleEventAvro.getIdentifier() = getAggregate().getIdentifier()

fun TaskScheduleEventAvro.getVersion() = getAggregate().getVersion()
