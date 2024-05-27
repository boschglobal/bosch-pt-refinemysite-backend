/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskattachment.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.buildNotificationIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.buildObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentAggregateAvro
import java.util.UUID

fun TaskAttachmentAggregateAvro.buildObjectReference() = getAggregateIdentifier().buildObjectReference()

fun TaskAttachmentAggregateAvro.buildNotificationIdentifier(recipientIdentifier: UUID) =
    getAggregateIdentifier().buildNotificationIdentifier(recipientIdentifier)
