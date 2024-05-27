/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.facade.job

import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.doWithAuthenticatedUser
import com.bosch.pt.iot.smartsite.project.participant.command.api.ResendInvitationCommand
import com.bosch.pt.iot.smartsite.project.participant.command.handler.ResendInvitationCommandHandler
import com.bosch.pt.iot.smartsite.project.participant.query.InvitationQueryService
import com.bosch.pt.iot.smartsite.project.participant.query.ParticipantQueryService
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.INVITED
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import java.time.LocalDate
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * This job automatically resends invitations 7 and 21 days after they were created. This happens
 * independently of any manually resent invitation. For now, the purpose is to increase the number
 * of sign-ups based on invitations. The job is triggered at midnight and picks up all invitation
 * created 7 or 21 days ago. If the job fails, no reminders are sent since the next run will only
 * pick up new invitations. To improve this, we could in the future also take the last sent
 * information of an invitation into account.
 */
@Component
open class InvitationReminderJob(
    private val invitationService: InvitationQueryService,
    private val resendInvitationCommandHandler: ResendInvitationCommandHandler,
    private val participantQueryService: ParticipantQueryService,
    private val userService: UserService,
    @Value("\${custom.mail.job.resend-invitation.enabled:false}") private val enabled: Boolean,
    @Value("\${custom.mail.job.resend-invitation.first-reminder-period-days:7}")
    private val firstReminderDays: Long,
    @Value("\${custom.mail.job.resend-invitation.second-reminder-period-days:21}")
    private val secondReminderDays: Long
) {

  @Scheduled(cron = "@midnight")
  @SchedulerLock(name = "FirstInvitationReminderJob", lockAtLeastFor = "1m")
  open fun sendFirstReminders() {
    resendInvitations(firstReminderDays)
  }

  @Scheduled(cron = "@midnight")
  @SchedulerLock(name = "SecondInvitationReminderJob", lockAtLeastFor = "1m")
  open fun sendSecondReminders() {
    resendInvitations(secondReminderDays)
  }

  private fun resendInvitations(daysAgo: Long) {
    if (enabled) {
      LOGGER.info("Resending invitations automatically that are '$daysAgo' days old")
      invitationService.findAllCreatedAtDay(LocalDate.now().minusDays(daysAgo)).forEach {
        doWithAuthenticatedUser(
            requireNotNull(userService.findOne(it.lastModifiedBy.get().identifier))) {
              requireNotNull(
                      participantQueryService.findParticipantWithDetails(it.participantIdentifier))
                  .apply {
                    // Invitations are currently kept until user is active. Therefore, we need to
                    // filter only those that are still in status INVITED
                    if (this.status == INVITED) {
                      resendInvitationCommandHandler.handle(
                          ResendInvitationCommand(it.participantIdentifier))
                      LOGGER.info(
                          "Invitation for project ${it.projectIdentifier} resent to ${it.email}")
                    }
                  }
            }
      }
    } else {
      LOGGER.info("Job to resend invitations automatically after $daysAgo days is disabled")
    }
  }

  companion object {
    val LOGGER: Logger = LoggerFactory.getLogger(InvitationReminderJob::class.java)
  }
}
