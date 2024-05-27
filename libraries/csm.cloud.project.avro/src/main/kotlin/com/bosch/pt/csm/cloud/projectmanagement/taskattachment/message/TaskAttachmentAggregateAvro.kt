/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.taskattachment.message

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentAggregateAvro

fun TaskAttachmentAggregateAvro.getIdentifier() = getAggregateIdentifier().getIdentifier().toUUID()

fun TaskAttachmentAggregateAvro.getTaskIdentifier() = getTask().getIdentifier().toUUID()

fun TaskAttachmentAggregateAvro.getLastModifiedDate() =
    getAuditingInformation().getLastModifiedDate().toInstantByMillis()

fun TaskAttachmentAggregateAvro.getLastModifiedByUserIdentifier() =
    getAuditingInformation().getLastModifiedBy().getIdentifier().toUUID()

fun TaskAttachmentAggregateAvro.getTaskVersion() = getTask().getVersion()

fun TaskAttachmentAggregateAvro.buildAggregateIdentifier() =
    getAggregateIdentifier().buildAggregateIdentifier()
