/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.common.exceptions.ResourceNotFoundException
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.ERROR_MESSAGE_NOTIFICATION_NOT_FOUND
import com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.NotificationService
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.LastSeenResource
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.NotificationListResource
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.NotificationResource
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.UpdateLastSeenResource
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.factory.LastSeenResourceFactory
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.factory.NotificationListResourceFactory
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.factory.NotificationResourceFactory
import com.bosch.pt.csm.cloud.projectmanagement.user.boundary.UserService
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import java.time.Instant
import java.util.UUID
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@RestController
class NotificationController(
    private val notificationListResourceFactory: NotificationListResourceFactory,
    private val notificationResourceFactory: NotificationResourceFactory,
    private val lastSeenResourceFactory: LastSeenResourceFactory,
    private val notificationService: NotificationService,
    private val userService: UserService
) {

  @GetMapping(NOTIFICATION_LAST_SEEN_ENDPOINT)
  fun getLastSeenNotification(
      @AuthenticationPrincipal user: User
  ): ResponseEntity<LastSeenResource> =
      ResponseEntity.ok()
          .body(lastSeenResourceFactory.build(userService.getLastSeen(user.identifier)))

  @PostMapping(NOTIFICATION_LAST_SEEN_ENDPOINT)
  fun setLastSeenNotification(
      @AuthenticationPrincipal user: User,
      @RequestBody lastSeenNotification: UpdateLastSeenResource
  ): ResponseEntity<String> {
    userService.setLastSeen(user.identifier, lastSeenNotification.lastSeen)
    return ResponseEntity.noContent().build()
  }

  @PostMapping(MARK_NOTIFICATION_AS_READ_ENDPOINT)
  fun setNotificationRead(
      @AuthenticationPrincipal user: User,
      @PathVariable(value = "notificationId") notificationId: UUID
  ): ResponseEntity<String> {
    notificationService.markAsRead(user.identifier, notificationId)
    return ResponseEntity.noContent().build()
  }

  @GetMapping(NOTIFICATIONS_FOR_CURRENT_USER_ENDPOINT)
  fun findAllNotificationsForUser(
      @AuthenticationPrincipal user: User,
      @RequestParam(required = false) limit: Int?
  ): ResponseEntity<NotificationListResource> =
      ResponseEntity.ok()
          .body(
              notificationListResourceFactory.build(
                  notificationService.findAll(user.identifier, limit), limit, user))

  @GetMapping(path = [NOTIFICATIONS_FOR_CURRENT_USER_ENDPOINT], params = ["before"])
  fun findAllNotificationsForUserBefore(
      @AuthenticationPrincipal user: User,
      @RequestParam @DateTimeFormat before: Instant,
      @RequestParam(required = false) limit: Int?
  ): ResponseEntity<NotificationListResource> =
      ResponseEntity.ok()
          .body(
              notificationListResourceFactory.buildBefore(
                  notificationService.findAllBefore(user.identifier, before, limit), limit, user))

  @GetMapping(path = [NOTIFICATIONS_FOR_CURRENT_USER_ENDPOINT], params = ["after"])
  fun findAllNotificationsForUserAfter(
      @AuthenticationPrincipal user: User,
      @RequestParam @DateTimeFormat after: Instant,
      @RequestParam(required = false) limit: Int?
  ): ResponseEntity<NotificationListResource> =
      ResponseEntity.ok()
          .body(
              notificationListResourceFactory.buildAfter(
                  notificationService.findAllAfter(user.identifier, after, limit), limit, user))

  @GetMapping(path = [NOTIFICATIONS_FOR_CURRENT_USER_ENDPOINT], params = ["before", "after"])
  fun findAllNotificationsForUserNotImplemented(
      @Suppress("UnusedPrivateMember") @AuthenticationPrincipal user: User,
      @Suppress("UnusedPrivateMember") @RequestParam @DateTimeFormat before: Instant,
      @Suppress("UnusedPrivateMember") @RequestParam @DateTimeFormat after: Instant,
      @Suppress("UnusedPrivateMember") @RequestParam(required = false) limit: Int?
  ): ResponseEntity<NotificationListResource> {
    throw IllegalArgumentException("Unsupported combination of parameters")
  }

  @GetMapping(path = [NOTIFICATION_BY_ID_ENDPOINT])
  fun findNotification(
      @AuthenticationPrincipal user: User,
      @PathVariable(value = "notificationId") notificationId: UUID
  ): ResponseEntity<NotificationResource> =
      ResponseEntity.ok()
          .body(
              notificationResourceFactory.build(
                  notification =
                      notificationService.findOneByExternalIdentifier(
                          user.identifier, notificationId)
                          ?: throw ResourceNotFoundException(ERROR_MESSAGE_NOTIFICATION_NOT_FOUND),
                  user = user))

  companion object {
    const val NOTIFICATIONS_FOR_CURRENT_USER_ENDPOINT = "/projects/notifications"
    const val NOTIFICATION_LAST_SEEN_ENDPOINT = "/projects/notifications/seen"
    const val NOTIFICATION_BY_ID_ENDPOINT = "/projects/notifications/{notificationId}"
    const val MARK_NOTIFICATION_AS_READ_ENDPOINT = "/projects/notifications/{notificationId}/read"
  }
}
