/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.taskattachment.message

import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventAvro

fun TaskAttachmentEventAvro.getIdentifier() = getAggregate().getIdentifier()

fun TaskAttachmentEventAvro.getLastModifiedDate() = getAggregate().getLastModifiedDate()

fun TaskAttachmentEventAvro.getLastModifiedByUserIdentifier() =
    getAggregate().getLastModifiedByUserIdentifier()

fun TaskAttachmentEventAvro.getTaskIdentifier() = getAggregate().getTaskIdentifier()

fun TaskAttachmentEventAvro.getTaskVersion() = getAggregate().getTaskVersion()
