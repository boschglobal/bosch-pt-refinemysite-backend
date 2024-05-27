/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import java.util.UUID

fun MutableList<Notification>.selectFirstFor(user: User) =
    first { it.notificationIdentifier.recipientIdentifier == user.identifier }

fun MutableList<Notification>.selectFirstFor(user: User, aggregateIdentifier: AggregateIdentifierAvro) = first {
    it.notificationIdentifier.recipientIdentifier == user.identifier &&
        it.notificationIdentifier.identifier == UUID.fromString(aggregateIdentifier.getIdentifier())
}
