/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.facade.job

import com.bosch.pt.csm.cloud.common.command.AsyncRequestScopeAttributes.Companion.executeWithAsyncRequestScope
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.doWithAuthenticatedUser
import com.bosch.pt.iot.smartsite.project.participant.command.api.CancelInvitationCommand
import com.bosch.pt.iot.smartsite.project.participant.command.handler.CancelInvitationCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Invitation
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.InvitationRepository
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
open class InvitationExpirationJob(
    private val invitationRepository: InvitationRepository,
    private val cancelInvitationCommandHandler: CancelInvitationCommandHandler,
    private val userService: UserService,
    @Value("\${system.user.identifier}") private val systemUserIdentifier: UUID,
    @Value("\${custom.mail.job.expire-invitations.enabled:false}") private val enabled: Boolean,
) {

  @Scheduled(cron = "@midnight")
  @SchedulerLock(name = "InvitationExpirationJob", lockAtLeastFor = "1m")
  open fun cleanUpExpiredInvitations() {
    if (enabled) {
      val before = now().minusExpirationDuration()
      LOGGER.info("Deleting all invitations last sent before '$before'")

      doWithAuthenticatedUser(requireNotNull(userService.findOne(systemUserIdentifier))) {
        executeWithAsyncRequestScope { cancelAllExpiredInvitations(before) }
      }
    } else {
      LOGGER.info("Job to expire invitations automatically after 31 days is disabled")
    }
  }

  private fun cancelAllExpiredInvitations(before: LocalDateTime) {
    invitationRepository
        .findAllByLastSentBefore(before)
        .map(Invitation::participantIdentifier)
        .forEach { cancelInvitationCommandHandler.handle(CancelInvitationCommand(it)) }
  }

  private fun LocalDateTime.minusExpirationDuration() =
      minusDays(EXPIRE_AFTER_DAYS + TIME_ZONE_BUFFER)

  companion object {
    val LOGGER: Logger = LoggerFactory.getLogger(InvitationExpirationJob::class.java)

    const val EXPIRE_AFTER_DAYS = 30L

    /**
     * One day added to ensure that invitations expire earliest after [EXPIRE_AFTER_DAYS] days,
     * regardless of the user's actual time zone.
     *
     * E.g. if this service is deleting invitations at midnight UTC time, the invitation should be
     * still valid when sitting in California. Thereby one day is added as "safety buffer" that we
     * don't annoy users of later timezones.
     */
    private const val TIME_ZONE_BUFFER: Long = 1L
  }
}
