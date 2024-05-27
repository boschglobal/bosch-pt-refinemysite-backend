/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.task.message

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro

fun TaskAggregateAvro.getIdentifier() = getAggregateIdentifier().getIdentifier().toUUID()

fun TaskAggregateAvro.getTaskIdentifier() = getAggregateIdentifier().getIdentifier().toUUID()

fun TaskAggregateAvro.getAssigneeIdentifier() = getAssignee()?.getIdentifier()?.toUUID()

fun TaskAggregateAvro.getProjectIdentifier() = getProject().getIdentifier().toUUID()

fun TaskAggregateAvro.getCraftIdentifier() = getCraft().getIdentifier().toUUID()

fun TaskAggregateAvro.getWorkAreaIdentifier() = getWorkarea()?.getIdentifier()?.toUUID()

fun TaskAggregateAvro.getLastModifiedDate() =
    getAuditingInformation().getLastModifiedDate().toInstantByMillis()

fun TaskAggregateAvro.getLastModifiedByUserIdentifier() =
    getAuditingInformation().getLastModifiedBy().getIdentifier().toUUID()

fun TaskAggregateAvro.buildAggregateIdentifier() =
    getAggregateIdentifier().buildAggregateIdentifier()
