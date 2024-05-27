/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.NotificationService.Companion.MAX_NOTIFICATIONS
import com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.dto.GenericPageListWrapper
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.NotificationController
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.NotificationListResource
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.user.boundary.UserService
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import org.springframework.stereotype.Component

@Component
class NotificationListResourceFactory(
    private val notificationResourceFactory: NotificationResourceFactory,
    private val userService: UserService,
    private val linkFactory: CustomLinkBuilderFactory
) {

  fun build(
      notifications: GenericPageListWrapper<Notification>,
      limit: Int?,
      user: User
  ): NotificationListResource {
    return buildResource(notifications, user).apply {
      addBeforeLinkIfPrevious(notifications, limit)
    }
  }

  fun buildBefore(
      notifications: GenericPageListWrapper<Notification>,
      limit: Int?,
      user: User
  ): NotificationListResource {
    return buildResource(notifications, user).apply {
      addBeforeLinkIfPrevious(notifications, limit)
    }
  }

  fun buildAfter(
      notifications: GenericPageListWrapper<Notification>,
      limit: Int?,
      user: User
  ): NotificationListResource {
    return buildResource(notifications, user).apply { addAfterLinkIfPrevious(notifications, limit) }
  }

  private fun buildResource(
      notifications: GenericPageListWrapper<Notification>,
      user: User
  ): NotificationListResource {
    val notificationList = notifications.resources!!

    return NotificationListResource(
        items =
            notificationList
                .map { notification -> notificationResourceFactory.build(notification, user) }
                .toList(),
        lastSeen = userService.getLastSeen(user.identifier))
  }

  private fun NotificationListResource.addBeforeLinkIfPrevious(
      notifications: GenericPageListWrapper<Notification>,
      limit: Int?
  ) {
    // Add previous link for page containing more notifications
    if (notifications.previous) {
      val notificationList = notifications.resources!!
      val next = notificationList.last().insertDate

      this.add(
          linkFactory
              .linkTo(NotificationController.NOTIFICATIONS_FOR_CURRENT_USER_ENDPOINT)
              .withQueryParameters(mapOf("before" to next, "limit" to (limit ?: MAX_NOTIFICATIONS)))
              .withRel(NotificationListResource.LINK_PREVIOUS))
    }
  }

  private fun NotificationListResource.addAfterLinkIfPrevious(
      notifications: GenericPageListWrapper<Notification>,
      limit: Int?
  ) {
    // Add previous link for page containing more notifications
    if (notifications.previous) {
      val notificationList = notifications.resources!!
      val next = notificationList.last().insertDate

      this.add(
          linkFactory
              .linkTo(NotificationController.NOTIFICATIONS_FOR_CURRENT_USER_ENDPOINT)
              .withQueryParameters(mapOf("after" to next, "limit" to (limit ?: MAX_NOTIFICATIONS)))
              .withRel(NotificationListResource.LINK_PREVIOUS))
    }
  }
}
