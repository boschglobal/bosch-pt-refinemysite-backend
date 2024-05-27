/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.buildNotificationIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.buildObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import java.util.UUID

fun TopicAggregateG2Avro.buildObjectReference() = getAggregateIdentifier().buildObjectReference()

fun TopicAggregateG2Avro.buildNotificationIdentifier(recipientIdentifier: UUID) =
    getAggregateIdentifier().buildNotificationIdentifier(recipientIdentifier)