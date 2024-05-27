/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.message

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.ObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.NotificationIdentifier
import java.util.UUID

fun AggregateIdentifierAvro.buildObjectReference() =
    ObjectReference(getType(), getIdentifier().toUUID())

fun AggregateIdentifierAvro.buildNotificationIdentifier(recipientIdentifier: UUID) =
    NotificationIdentifier(
        type = getType(),
        identifier = getIdentifier().toUUID(),
        version = getVersion(),
        recipientIdentifier = recipientIdentifier)

fun AggregateIdentifierAvro.toAggregateIdentifier() =
    AggregateIdentifier(this.getType(), this.getIdentifier().toUUID(), this.getVersion())
