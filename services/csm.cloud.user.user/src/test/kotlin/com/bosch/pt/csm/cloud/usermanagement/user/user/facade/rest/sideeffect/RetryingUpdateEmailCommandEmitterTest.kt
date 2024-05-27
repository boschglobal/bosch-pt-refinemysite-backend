/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.sideeffect

import com.bosch.pt.csm.cloud.common.command.exceptions.EntityOutdatedException
import com.bosch.pt.csm.cloud.usermanagement.application.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler.UpdateEmailCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.sideeffect.RetryingUpdateEmailCommandEmitterTest.EnableRetryUnitTestConfiguration
import com.bosch.pt.csm.cloud.usermanagement.user.user.query.UserQueryService
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.annotation.EnableRetry
import org.springframework.test.context.ContextConfiguration

@SmartSiteMockKTest
@ContextConfiguration(classes = [EnableRetryUnitTestConfiguration::class])
class RetryingUpdateEmailCommandEmitterTest {

  @MockkBean private lateinit var updateEmailCommandHandler: UpdateEmailCommandHandler

  @MockkBean private lateinit var userQueryService: UserQueryService

  @MockkBean(relaxed = true) @Suppress("UnusedPrivateMember") private lateinit var logger: Logger

  @SpykBean private lateinit var cut: RetryingUpdateEmailCommandEmitter

  @Test
  fun `failure to update the email is handled gracefully after two attempts using the original user`() {

    val changedEmail = "new@example.com"
    val oldEmail = "old@examble.com"
    val user = spyk<User>().apply { email = oldEmail }

    every { userQueryService.findOneByIdentifier(user.identifier) } returns user
    every { updateEmailCommandHandler.handle(any()) } throws
        ObjectOptimisticLockingFailureException(User::class.java, user.identifier)

    val result = cut.updateUserWithEmailIfStillDiffers(user.identifier, changedEmail) as User

    // If the user's email really cannot be updated and the profile also has not been
    // updated concurrently the recovery method logging the issue should be called
    verify(exactly = 2) { updateEmailCommandHandler.handle(any()) }
    verify(exactly = 2) { cut.updateUserWithEmailIfStillDiffers(any(), any()) }
    verify(exactly = 1) { cut.onlyLogFailureForOptimisticLockingFailure(any(), any(), any()) }
    verify(exactly = 0) { cut.onlyLogFailureForOutdatedEntity(any(), any(), any()) }
    assertEquals(oldEmail, result.email)
  }

  @Test
  fun `failure to update the email with outdated entity is handled gracefully after two attempts`() {

    val changedEmail = "new@example.com"
    val oldEmail = "old@examble.com"
    val user = spyk<User>().apply { email = oldEmail }

    every { userQueryService.findOneByIdentifier(user.identifier) } returns user
    every { updateEmailCommandHandler.handle(any()) } throws
        EntityOutdatedException("user is outdated")

    val result = cut.updateUserWithEmailIfStillDiffers(user.identifier, changedEmail) as User

    // If the user's email really cannot be updated and the profile also has not been
    // updated concurrently the recovery method logging the issue should be called
    verify(exactly = 2) { updateEmailCommandHandler.handle(any()) }
    verify(exactly = 2) { cut.updateUserWithEmailIfStillDiffers(any(), any()) }
    verify(exactly = 1) { cut.onlyLogFailureForOutdatedEntity(any(), any(), any()) }
    verify(exactly = 0) { cut.onlyLogFailureForOptimisticLockingFailure(any(), any(), any()) }
    assertEquals(oldEmail, result.email)
  }

  @Test
  fun `concurrent access is handled gracefully on second attempt`() {

    val changedEmail = "new@example.com"
    val oldEmail = "old@examble.com"
    val user = mockk<User>(relaxed = true)

    every { user.email } returns oldEmail andThen changedEmail
    every { userQueryService.findOneByIdentifier(user.identifier) } returns user
    every { updateEmailCommandHandler.handle(any()) } throws
        ObjectOptimisticLockingFailureException(User::class.java, user.identifier)

    val result = cut.updateUserWithEmailIfStillDiffers(user.identifier, changedEmail) as User

    // the update command should be attempted exactly once
    // (on the first attempt when the email still differs)
    // on the second attempt the user already updated (by another process)
    // should simply be returned (no more interactions)
    verify(exactly = 1) { updateEmailCommandHandler.handle(any()) }
    verify(exactly = 2) { cut.updateUserWithEmailIfStillDiffers(any(), any()) }
    verify { cut.onlyLogFailureForOptimisticLockingFailure(any(), any(), any()) wasNot Called }
    assertEquals(changedEmail, result.email)
  }

  @Test
  fun `failure to update the email is handled gracefully on second attempt`() {

    val changedEmail = "new@example.com"
    val oldEmail = "old@examble.com"
    val user = mockk<User>(relaxed = true)

    every { user.email } returns oldEmail andThen oldEmail andThen changedEmail

    every { userQueryService.findOneByIdentifier(user.identifier) } returns user
    every { updateEmailCommandHandler.handle(any()) } throws
        ObjectOptimisticLockingFailureException(User::class.java, user.identifier) andThen
        Unit

    val result = cut.updateUserWithEmailIfStillDiffers(user.identifier, changedEmail) as User

    // in case there is a hick-up with the update command on the first attempt
    // and the email still differs in the newly fetched user object
    // the update command should be issued again
    verify(exactly = 2) { updateEmailCommandHandler.handle(any()) }
    verify(exactly = 2) { cut.updateUserWithEmailIfStillDiffers(any(), any()) }
    verify { cut.onlyLogFailureForOptimisticLockingFailure(any(), any(), any()) wasNot Called }
    assertEquals(changedEmail, result.email)
  }

  @AfterEach
  internal fun resetInvocationCounters() {
    clearAllMocks()
  }

  @Configuration
  @EnableRetry(proxyTargetClass = true)
  @Import(RetryingUpdateEmailCommandEmitter::class)
  @Suppress("UtilityClassWithPublicConstructor")
  class EnableRetryUnitTestConfiguration {
    companion object
  }
}
