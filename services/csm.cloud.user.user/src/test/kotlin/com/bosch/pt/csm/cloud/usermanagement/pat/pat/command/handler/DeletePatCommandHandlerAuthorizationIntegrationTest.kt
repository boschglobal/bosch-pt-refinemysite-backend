/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.handler

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractAuthorizationIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.PAT_VALIDATION_ERROR_USER_MISMATCH
import com.bosch.pt.csm.cloud.usermanagement.pat.event.submitPatCreated
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatCreatedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.DeletePatCommand
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.PatId
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

class DeletePatCommandHandlerAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: DeletePatCommandHandler

  @Test
  fun `delete PAT command for PAT impersonating another user is rejected`() {

    eventStreamGenerator.submitPatCreated("PAT", "user")
    val pat = get<PatCreatedEventAvro>("PAT")!!

    val user = get<UserAggregateAvro>("otherUser")!!
    setAuthentication("otherUser")

    assertThatExceptionOfType(AccessDeniedException::class.java)
        .isThrownBy {
          cut.handle(
              DeletePatCommand(
                  patId = PatId(pat.aggregateIdentifier.identifier.toString()),
                  version = 0,
                  impersonatedUser = user.getIdentifier().asUserId()))
        }
        .withMessage(PAT_VALIDATION_ERROR_USER_MISMATCH)
  }

  @Test
  fun `delete PAT command for PAT of another user than authenticated is rejected`() {

    eventStreamGenerator.submitPatCreated("PAT", "user")
    val pat = get<PatCreatedEventAvro>("PAT")!!
    val user = get<UserAggregateAvro>("user")!!
    setAuthentication("otherUser")

    assertThatExceptionOfType(AccessDeniedException::class.java)
        .isThrownBy {
          cut.handle(
              DeletePatCommand(
                  patId = PatId(pat.aggregateIdentifier.identifier.toString()),
                  version = 0,
                  impersonatedUser = user.getIdentifier().asUserId()))
        }
        .withMessage("Access Denied")
  }

  @Test
  fun `delete PAT command for non-existing PAT is rejected`() {

    val user = get<UserAggregateAvro>("user")!!
    setAuthentication("user")

    assertThatExceptionOfType(AggregateNotFoundException::class.java)
        .isThrownBy {
          cut.handle(
              DeletePatCommand(
                  patId = PatId(), version = 0, impersonatedUser = user.getIdentifier().asUserId()))
        }
        .withMessage("Pat_ValidationError_NotFound")
  }
}
