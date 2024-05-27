/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.message.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.buildNotificationIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message.buildObjectReference
import java.util.UUID

fun MessageAggregateAvro.buildObjectReference() = aggregateIdentifier.buildObjectReference()

fun MessageAggregateAvro.buildNotificationIdentifier(recipientIdentifier: UUID) =
    aggregateIdentifier.buildNotificationIdentifier(recipientIdentifier)