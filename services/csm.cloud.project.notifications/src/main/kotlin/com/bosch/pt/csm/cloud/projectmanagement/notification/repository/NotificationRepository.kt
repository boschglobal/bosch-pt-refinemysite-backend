/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.repository

import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.NotificationIdentifier
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.UUID

interface NotificationRepository :
    MongoRepository<Notification, NotificationIdentifier>,
    NotificationRepositoryExtension {

    fun findOneByNotificationIdentifierRecipientIdentifierAndExternalIdentifier(
        notificationIdentifierRecipientIdentifier: UUID,
        externalIdentifier: UUID
    ): Notification?

    fun findOneByExternalIdentifier(externalIdentifier: UUID): Notification

    fun findAllByMergedFalse(): MutableList<Notification>
}
