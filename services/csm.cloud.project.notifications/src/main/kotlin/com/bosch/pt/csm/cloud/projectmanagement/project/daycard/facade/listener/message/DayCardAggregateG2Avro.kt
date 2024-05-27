/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.buildNotificationIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.buildObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro
import java.util.UUID

fun DayCardAggregateG2Avro.buildObjectReference() = getAggregateIdentifier().buildObjectReference()

fun DayCardAggregateG2Avro.buildNotificationIdentifier(recipientIdentifier: UUID) =
    getAggregateIdentifier().buildNotificationIdentifier(recipientIdentifier)