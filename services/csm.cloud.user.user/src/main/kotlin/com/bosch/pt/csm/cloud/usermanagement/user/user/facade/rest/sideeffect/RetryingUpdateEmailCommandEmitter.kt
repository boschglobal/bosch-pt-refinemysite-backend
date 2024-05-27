/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.sideeffect

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.exceptions.EntityOutdatedException
import com.bosch.pt.csm.cloud.common.security.JwtVerificationListener
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.UpdateEmailCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler.UpdateEmailCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.user.user.query.UserQueryService
import java.io.Closeable
import org.slf4j.Logger
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextHolder.createEmptyContext
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component

/** Encapsulates the retry behavior emitting an [UpdateEmailCommand]. */
@Component
class RetryingUpdateEmailCommandEmitter(
    private val updateEmailCommandHandler: UpdateEmailCommandHandler,
    private val userQueryService: UserQueryService,
    private val logger: Logger
) {

  /**
   * Updates the email of the user if it still differs. Designed to be called in concurrent access
   * scenarios where it is expected that another process may have been faster (in which case the
   * [UpdateEmailCommand] fails with a stale version issue). If the email of the user already
   * reflects the [changedEmail] value we can exit gracefully.
   */
  @Retryable(
      maxAttempts = 2,
      backoff = Backoff(delay = 100),
      value = [ObjectOptimisticLockingFailureException::class, EntityOutdatedException::class])
  fun updateUserWithEmailIfStillDiffers(
      userId: UserId,
      changedEmail: String,
  ): UserDetails =
      userQueryService.findOneByIdentifier(userId).apply {
        if (this.email != changedEmail) {

          logger.info("Email in JWT differs for user $userId. Attempting to update now..")
          if (logger.isTraceEnabled) {
            logger.trace("Updating Email ${this.email} to $changedEmail for user $userId")
          }

          InterimAuthentication(this).use {
            updateEmailCommandHandler.handle(
                UpdateEmailCommand(this.identifier, this.version, changedEmail))
            logger.info("Successfully updated email for user ${this.identifier}")
            return userQueryService.findOneByIdentifier(userId)
          }
        } else {
          logger.info(
              "Email for user ${this.identifier} has already been adjusted in concurrent access.")
        }
      }

  @Recover
  fun onlyLogFailureForOptimisticLockingFailure(
      e: ObjectOptimisticLockingFailureException,
      userId: UserId,
      changedEmail: String
  ): UserDetails {
    logger.warn("Unable to apply email $changedEmail for $userId after 2 attempts: ${e.message}", e)
    return userQueryService.findOneByIdentifier(userId)
  }

  @Recover
  fun onlyLogFailureForOutdatedEntity(
      e: EntityOutdatedException,
      userId: UserId,
      changedEmail: String
  ): UserDetails {
    logger.warn(
        "Updating email $changedEmail failed for $userId after 2 attempts: ${e.message}. " +
            "The entity has already changed.",
        e)
    return userQueryService.findOneByIdentifier(userId)
  }

  /**
   * Interim authentication is required for the user's update email command to be executed in the
   * user's name during the user's authentication process that [JwtVerificationListener] instances
   * are called within. It can be set here unhesitatingly as the JWT has already been verified and
   * mapped to the user details successfully. Use of the resource interface [Closeable] ensures that
   * interim authentication is removed and the final authentication for the request will be set by
   * [org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter].
   */
  private class InterimAuthentication(userDetails: UserDetails) : Closeable {

    init {
      SecurityContextHolder.setContext(
          createEmptyContext().apply {
            authentication =
                UsernamePasswordAuthenticationToken(
                    userDetails, NO_CREDENTIALS, listOf(SimpleGrantedAuthority(USER_ROLE)))
          })
    }

    override fun close() = SecurityContextHolder.clearContext()
  }

  companion object {
    const val USER_ROLE = "USER_ROLE"
    const val NO_CREDENTIALS = "n/a"
  }
}
