/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.impl.CriteriaOperator
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.TemplateWithPlaceholders
import com.bosch.pt.csm.cloud.projectmanagement.notification.repository.NotificationRepositoryExtension
import com.bosch.pt.csm.cloud.projectmanagement.notification.repository.impl.NotificationAttributeNames.CLASS
import com.bosch.pt.csm.cloud.projectmanagement.notification.repository.impl.NotificationAttributeNames.EVENT_DATE
import com.bosch.pt.csm.cloud.projectmanagement.notification.repository.impl.NotificationAttributeNames.EVENT_USER
import com.bosch.pt.csm.cloud.projectmanagement.notification.repository.impl.NotificationAttributeNames.EXTERNAL_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.notification.repository.impl.NotificationAttributeNames.INSERT_DATE
import com.bosch.pt.csm.cloud.projectmanagement.notification.repository.impl.NotificationAttributeNames.MERGED
import com.bosch.pt.csm.cloud.projectmanagement.notification.repository.impl.NotificationAttributeNames.PROJECT_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.notification.repository.impl.NotificationAttributeNames.READ
import com.bosch.pt.csm.cloud.projectmanagement.notification.repository.impl.NotificationAttributeNames.RECIPIENT_IDENTIFIER
import com.bosch.pt.csm.cloud.projectmanagement.notification.repository.impl.NotificationAttributeNames.SUMMARY_TEMPLATE_MESSAGE_KEY
import com.bosch.pt.csm.cloud.projectmanagement.notification.repository.impl.NotificationAttributeNames.TASK_IDENTIFIER
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Order.asc
import org.springframework.data.domain.Sort.Order.desc
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.Update

open class NotificationRepositoryExtensionImpl(
    private val mongoOperations: MongoOperations
) : NotificationRepositoryExtension {

    override fun markAsRead(userIdentifier: UUID, externalIdentifier: UUID) {
        val query = Query().apply {
            addCriteria(
                where(RECIPIENT_IDENTIFIER).`is`(userIdentifier)
                    .and(EXTERNAL_IDENTIFIER).`is`(externalIdentifier)
            )
        }

        val update = Update().apply { set(READ, true) }

        mongoOperations.updateFirst(query, update, Notification::class.java)
    }

    override fun markAsMerged(userIdentifier: UUID, externalIdentifier: UUID) {
        val query = Query().apply {
            addCriteria(
                where(RECIPIENT_IDENTIFIER).`is`(userIdentifier)
                    .and(EXTERNAL_IDENTIFIER).`is`(externalIdentifier)
            )
        }

        val update = Update().apply { set(MERGED, true) }

        mongoOperations.updateFirst(query, update, Notification::class.java)
    }

    override fun findAll(userIdentifier: UUID, limit: Int): List<Notification> =
        find(userIdentifier, null, true, limit)

    override fun findAllBefore(userIdentifier: UUID, before: Instant, limit: Int) =
        find(userIdentifier, before, true, limit)

    override fun findAllAfter(userIdentifier: UUID, after: Instant, limit: Int) =
        find(userIdentifier, after, false, limit)

    override fun deleteNotifications(userIdentifier: UUID, projectIdentifier: UUID) {
        val criteria = CriteriaOperator.and(
            belongsToProject(projectIdentifier), belongsToUser(userIdentifier)
        )
        mongoOperations.remove(query(criteria), Collections.NOTIFICATION)
    }

    override fun findMergeableTaskUpdatedNotification(
        recipient: UUID,
        projectIdentifier: UUID,
        taskIdentifier: UUID,
        summaryMessageKey: String,
        eventDate: Instant,
        eventUser: UUID
    ): Notification? {
        val query = query(
            where(RECIPIENT_IDENTIFIER).`is`(recipient)
                .and(PROJECT_IDENTIFIER).`is`(projectIdentifier)
                .and(TASK_IDENTIFIER).`is`(taskIdentifier)
                .and(SUMMARY_TEMPLATE_MESSAGE_KEY).`is`(summaryMessageKey)
                .and(CLASS).ne(TemplateWithPlaceholders::class.simpleName)
                .and(EVENT_USER).`is`(eventUser)
                .and(EVENT_DATE).gt(eventDate)
        )
            .with(Sort.by(desc(INSERT_DATE)))
            .limit(1)
        return mongoOperations.find(query, Notification::class.java).firstOrNull()
    }

    private fun belongsToProject(identifier: UUID): Criteria {
        return where(PROJECT_IDENTIFIER).`is`(identifier)
    }

    private fun belongsToUser(identifier: UUID): Criteria {
        return where(RECIPIENT_IDENTIFIER).`is`(identifier)
    }

    private fun find(
        userIdentifier: UUID,
        date: Instant?,
        descending: Boolean,
        limit: Int
    ): List<Notification> {
        val sort = if (descending) Sort.by(desc(INSERT_DATE)) else Sort.by(asc(INSERT_DATE))
        val criteria = where(RECIPIENT_IDENTIFIER).`is`(userIdentifier).and(MERGED).`is`(false)
        var notifications = mutableListOf<Notification>()
        val limitIncludingNextElement = limit + 1

        if (date == null) {
            val pageable = PageRequest.of(0, limitIncludingNextElement, sort)
            val query = query(criteria).with(pageable)
            notifications = mongoOperations.find(query, Notification::class.java)
        } else {
            if (descending) criteria.and(INSERT_DATE).lt(date) else criteria.and(INSERT_DATE).gt(date)

            val query = query(criteria).with(sort).limit(MAX_CURSOR_ITERATIONS)
            mongoOperations.stream(query, Notification::class.java).use { cursor ->
                var iterator = cursor.iterator()
                var nextNotification: Notification? = if (iterator.hasNext()) iterator.next() else null
                while (true) {
                    if (nextNotification == null) {
                        return notifications
                    } else {
                        notifications.add(nextNotification)
                    }

                    nextNotification = if (iterator.hasNext()) iterator.next() else null
                    // If the next notification has the same time as the last one then continue until another
                    // date is found
                    if (notifications.size >= limitIncludingNextElement &&
                        nextNotification != null &&
                        notifications[notifications.size - 1]
                            .insertDate == nextNotification.insertDate
                    ) continue
                    else if (notifications.size >= limitIncludingNextElement) return notifications
                }
            }
        }
        return notifications
    }

    companion object {
        private const val MAX_CURSOR_ITERATIONS = 100
    }
}
