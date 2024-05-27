/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.sideeffect

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

@SmartSiteMockKTest
class UpdateEmailOnJwtListenerTest {

  @MockK private lateinit var retryingUpdateEmailCommandEmitter: RetryingUpdateEmailCommandEmitter

  @Test
  fun `email is not updated if it differs only in case`() {
    val cut = UpdateEmailOnJwtListener(retryingUpdateEmailCommandEmitter)
    val userDetails = mockk<User>()
    val jwtClaims = mapOf("email" to "Hans.Mustermann@example.com")
    every { userDetails.email }.returns("hans.mustermann@example.com")

    cut.onJwtVerifiedEvent(jwtClaims, userDetails)

    verify(atLeast = 0, atMost = 0) {
      retryingUpdateEmailCommandEmitter.updateUserWithEmailIfStillDiffers(any(), any())
    }
  }

  @Test
  fun `email is not updated if none is contained in the JWT`() {
    val cut = UpdateEmailOnJwtListener(retryingUpdateEmailCommandEmitter)
    val userDetails = mockk<User>()
    val jwtClaims = mapOf<String, Any>()
    every { userDetails.email }.returns("hans.mustermann@example.com")

    cut.onJwtVerifiedEvent(jwtClaims, userDetails)

    verify(atLeast = 0, atMost = 0) {
      retryingUpdateEmailCommandEmitter.updateUserWithEmailIfStillDiffers(any(), any())
    }
  }

  @Test
  fun `email is only updated if it differs`() {
    val cut = UpdateEmailOnJwtListener(retryingUpdateEmailCommandEmitter)
    val userDetails = mockk<User>()
    val jwtClaims = mapOf("email" to "H.Mustermann@example.com")
    every { userDetails.email }.returns("hans.mustermann@example.com")
    every { userDetails.identifier }.returns(UserId())
    every {
          retryingUpdateEmailCommandEmitter.updateUserWithEmailIfStillDiffers(
              any(), "H.Mustermann@example.com")
        }
        .returns(userDetails)

    cut.onJwtVerifiedEvent(jwtClaims, userDetails)

    verify(atLeast = 1, atMost = 1) {
      retryingUpdateEmailCommandEmitter.updateUserWithEmailIfStillDiffers(any(), any())
    }
  }
}
