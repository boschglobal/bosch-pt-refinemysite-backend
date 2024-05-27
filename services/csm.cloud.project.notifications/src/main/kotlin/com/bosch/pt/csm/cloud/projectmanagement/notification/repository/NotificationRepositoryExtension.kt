/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.repository

import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import java.time.Instant
import java.util.UUID

interface NotificationRepositoryExtension {

    fun markAsRead(userIdentifier: UUID, externalIdentifier: UUID)

    fun markAsMerged(userIdentifier: UUID, externalIdentifier: UUID)

    fun findAll(userIdentifier: UUID, limit: Int): List<Notification>

    fun findAllBefore(userIdentifier: UUID, before: Instant, limit: Int): List<Notification>

    fun findAllAfter(userIdentifier: UUID, after: Instant, limit: Int): List<Notification>

    fun deleteNotifications(userIdentifier: UUID, projectIdentifier: UUID)

    fun findMergeableTaskUpdatedNotification(
        recipient: UUID,
        projectIdentifier: UUID,
        taskIdentifier: UUID,
        summaryMessageKey: String,
        eventDate: Instant,
        eventUser: UUID
    ): Notification?
}
