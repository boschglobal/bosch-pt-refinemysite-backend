/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.sideeffect

import com.bosch.pt.csm.cloud.common.security.JwtVerificationListener
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.UpdateEmailCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service

/**
 * Responsible for updating the email of a user by emitting an [UpdateEmailCommand] if the email
 * found in the Json Web Token (JWT) does not match the email maintained with our user. As
 * concurrent access needs to be expected it delegates to [retryingUpdateEmailCommandEmitter] to
 * apply the changed email.
 */
@Service
class UpdateEmailOnJwtListener(
    private val retryingUpdateEmailCommandEmitter: RetryingUpdateEmailCommandEmitter
) : JwtVerificationListener {

  override fun onJwtVerifiedEvent(
      jwtClaims: Map<String, *>,
      userDetails: UserDetails
  ): UserDetails =
      if (isUserWithEmailDifferingFromJwtEmail(userDetails, jwtClaims))
          retryingUpdateEmailCommandEmitter.updateUserWithEmailIfStillDiffers(
              (userDetails as User).identifier, jwtClaims[EMAIL_CLAIM_KEY] as String)
      else userDetails

  /**
   * Initial cost-efficient check if the email in JWT differs from the one maintained with the user
   * in RmS.
   */
  private fun isUserWithEmailDifferingFromJwtEmail(
      userDetails: UserDetails,
      jwtClaims: Map<String, *>
  ) =
      userDetails is User &&
          jwtClaims.containsKey(EMAIL_CLAIM_KEY) &&
          jwtClaims[EMAIL_CLAIM_KEY] as String notEqualIgnoringCase (userDetails.email)

  private infix fun String.notEqualIgnoringCase(other: String?) =
      !this.equals(other, ignoreCase = true)

  companion object {
    const val EMAIL_CLAIM_KEY = "email"
  }
}
