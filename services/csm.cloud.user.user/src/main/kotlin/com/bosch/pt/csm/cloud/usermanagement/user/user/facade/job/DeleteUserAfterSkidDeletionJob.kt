/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.job

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.application.security.AuthorizationUtils.doWithAuthenticatedUser
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.DeleteUserAfterSkidDeletionCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler.DeleteUserAfterSkidDeletionCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.user.user.integration.SkidIntegrationService
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.UserDeletionState
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.repository.UserDeletionStateRepository
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.repository.UserRepository
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Profile("skid-deleted-user-propagation")
@Component
class DeleteUserAfterSkidDeletionJob(
    private val deleteUserAfterSkidDeletionCommandHandler:
        DeleteUserAfterSkidDeletionCommandHandler,
    private val skidIntegrationService: SkidIntegrationService,
    private val userDeletionStateRepository: UserDeletionStateRepository,
    private val userRepository: UserRepository,
    private val logger: Logger,
    @Value("\${custom.skid.dryRun:false}") private val dryRun: Boolean,
    @Value("\${system.user.identifier}") private val systemUserIdentifier: UserId,
) {

  @Scheduled(fixedDelay = 12, timeUnit = TimeUnit.HOURS)
  @SchedulerLock(name = "DeleteUserAfterSkidDeletionJob")
  fun deleteUsersDeletedInSkid() {
    // get deletion state from database
    val deletionState =
        userDeletionStateRepository.findAll().singleOrNull()
            ?: UserDeletionState(FIRST_USER_CREATED_AT)

    val from = deletionState.deletedToDateTime
    val to =
        if (from.plusDays(1).isBefore(LocalDateTime.now())) {
          from.plusDays(1)
        } else {
          LocalDateTime.now()
        }

    val deletedExternalIdsInBatches =
        skidIntegrationService.findUsersDeletedInDateRangeInBatches(from, to)

    var batchCount = 0
    deletedExternalIdsInBatches.forEach { batchOfUsers ->
      batchCount++
      val usersToDelete = userRepository.findAllByExternalUserIdIn(batchOfUsers)

      usersToDelete.forEach {
        if (!dryRun) {

          // note: this will also delete the user's profile picture
          doWithAuthenticatedUser(findSystemUser()) {
            logger.info("Deleting user ${it.identifier} (SKID identifier ${it.externalUserId})")

            deleteUserAfterSkidDeletionCommandHandler.handle(
                DeleteUserAfterSkidDeletionCommand(it.identifier))
          }
        } else {
          logger.info(
              "Received deletion request for user $it but dry run is active. " +
                  "Otherwise the user would have been deleted.")
        }
      }
    }
    logger.info("Checked deleted users from SKID from $from to $to in $batchCount batches")

    // save state
    deletionState.deletedToDateTime = to
    userDeletionStateRepository.save(deletionState)
  }

  private fun findSystemUser() =
      (userRepository.findOneByIdentifier(systemUserIdentifier)
          ?: error("Couldn't load system user by identifier $systemUserIdentifier"))

  companion object {

    /** the creation date of the first prod user */
    private val FIRST_USER_CREATED_AT = LocalDateTime.of(2018, 11, 8, 0, 0)
  }
}
