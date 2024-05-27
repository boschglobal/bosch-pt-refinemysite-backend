/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.task.message

import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventAvro

fun TaskEventAvro.getIdentifier() = getAggregate().getIdentifier()

fun TaskEventAvro.getTaskIdentifier() = getAggregate().getTaskIdentifier()

fun TaskEventAvro.getAssigneeIdentifier() = getAggregate().getAssigneeIdentifier()

fun TaskEventAvro.getLastModifiedDate() = getAggregate().getLastModifiedDate()

fun TaskEventAvro.getLastModifiedByUserIdentifier() =
    getAggregate().getLastModifiedByUserIdentifier()
