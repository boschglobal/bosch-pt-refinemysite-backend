/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.user.command

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.DE
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.US
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractAuthorizationIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum.MALE
import com.bosch.pt.csm.cloud.usermanagement.user.user.api.RegisterUserCommand
import com.bosch.pt.csm.cloud.usermanagement.user.user.command.handler.RegisterUserCommandHandler
import java.util.Locale.GERMANY
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

class RegisterUserCommandHandlerPreconditionsIntegrationTest :
    AbstractAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: RegisterUserCommandHandler

  @BeforeEach
  fun setAuthentication() {
    setSecurityContextAsUnregisteredUser()
  }

  @Test
  fun `Registration fails when eula not accepted and country is US`() {
    RegisterUserCommand(
            eulaAccepted = false,
            firstName = "hans",
            lastName = "müller",
            externalUserId = "UNREGISTERED",
            email = "test@example.com",
            gender = MALE,
            locale = GERMANY,
            country = US,
            crafts = emptyList())
        .apply {
          assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
            cut.handle(this)
          }
        }
  }

  @Test
  fun `Registration succeeds when eula not accepted and country is not US`() {
    RegisterUserCommand(
            eulaAccepted = false,
            firstName = "hans",
            lastName = "müller",
            externalUserId = "UNREGISTERED",
            email = "test@example.com",
            gender = MALE,
            locale = GERMANY,
            country = DE,
            crafts = emptyList())
        .apply { assertThat(cut.handle(this)).isNotNull }
  }

  @Test
  fun `Registration fails when external user id blank`() {
    RegisterUserCommand(
            eulaAccepted = true,
            firstName = "hans",
            lastName = "müller",
            externalUserId = "",
            email = "test@example.com",
            gender = MALE,
            locale = GERMANY,
            country = DE,
            crafts = emptyList())
        .apply {
          assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
            cut.handle(this)
          }
        }
  }

  @Test
  fun `Registration fails when first name is blank`() {
    RegisterUserCommand(
            eulaAccepted = true,
            firstName = "",
            lastName = "müller",
            externalUserId = "UNREGISTERED",
            email = "test@example.com",
            gender = MALE,
            locale = GERMANY,
            country = DE,
            crafts = emptyList())
        .apply {
          assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
            cut.handle(this)
          }
        }
  }

  @Test
  fun `Registration fails when last name is blank`() {
    RegisterUserCommand(
            eulaAccepted = true,
            firstName = "hans",
            lastName = "",
            externalUserId = "UNREGISTERED",
            email = "test@example.com",
            gender = MALE,
            locale = GERMANY,
            country = DE,
            crafts = emptyList())
        .apply {
          assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
            cut.handle(this)
          }
        }
  }

  @Test
  fun `Registration fails when email is blank`() {
    RegisterUserCommand(
            eulaAccepted = true,
            firstName = "hans",
            lastName = "müller",
            externalUserId = "UNREGISTERED",
            email = "",
            gender = MALE,
            locale = GERMANY,
            country = DE,
            crafts = emptyList())
        .apply {
          assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
            cut.handle(this)
          }
        }
  }

  @Test
  fun `Registration fails when position is blank`() {
    RegisterUserCommand(
            eulaAccepted = true,
            firstName = "hans",
            lastName = "müller",
            externalUserId = "UNREGISTERED",
            email = "abc@test.de",
            gender = MALE,
            locale = GERMANY,
            country = DE,
            crafts = emptyList(),
            position = "")
        .apply {
          assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
            cut.handle(this)
          }
        }
  }

  @Test
  fun `Registration succeeds when only position is null`() {
    RegisterUserCommand(
            eulaAccepted = true,
            firstName = "hans",
            lastName = "müller",
            externalUserId = "UNREGISTERED",
            email = "abc@test.de",
            gender = MALE,
            locale = GERMANY,
            country = DE,
            crafts = emptyList(),
            position = null)
        .apply { assertThat(cut.handle(this)).isNotNull }
  }
}
