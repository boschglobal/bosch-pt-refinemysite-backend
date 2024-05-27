/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.buildNotificationIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.buildObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleAggregateAvro
import java.util.UUID

fun TaskScheduleAggregateAvro.buildObjectReference() = getAggregateIdentifier().buildObjectReference()

fun TaskScheduleAggregateAvro.buildNotificationIdentifier(recipientIdentifier: UUID) =
    getAggregateIdentifier().buildNotificationIdentifier(recipientIdentifier)