/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.handler

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.usermanagement.common.AbstractAuthorizationIntegrationTest
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.PAT_VALIDATION_ERROR_USER_MISMATCH
import com.bosch.pt.csm.cloud.usermanagement.pat.event.submitPatCreated
import com.bosch.pt.csm.cloud.usermanagement.pat.messages.PatCreatedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.PatId
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.UpdatePatCommand
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.PatScopeEnum.GRAPHQL_API_READ
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

class UpdatePatCommandHandlerAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: UpdatePatCommandHandler

  @Test
  fun `update PAT command for PAT impersonating another user is rejected`() {

    eventStreamGenerator.submitPatCreated("PAT", "user")
    val pat = get<PatCreatedEventAvro>("PAT")!!
    val user = get<UserAggregateAvro>("otherUser")!!
    setAuthentication("otherUser")

    assertThatExceptionOfType(AccessDeniedException::class.java)
        .isThrownBy {
          cut.handle(
              UpdatePatCommand(
                  patId = PatId(pat.aggregateIdentifier.identifier.toString()),
                  version = 0,
                  impersonatedUser = user.getIdentifier().asUserId(),
                  description = "Updating another user's PAT",
                  validForMinutes = 60 * 24 * 3,
                  scopes = listOf(GRAPHQL_API_READ),
              ),
          )
        }
        .withMessage(PAT_VALIDATION_ERROR_USER_MISMATCH)
  }

  @Test
  fun `update PAT command for PAT of another user than authenticated is rejected`() {

    eventStreamGenerator.submitPatCreated("PAT", "user")
    val pat = get<PatCreatedEventAvro>("PAT")!!
    val user = get<UserAggregateAvro>("user")!!
    setAuthentication("otherUser")

    assertThatExceptionOfType(AccessDeniedException::class.java)
        .isThrownBy {
          cut.handle(
              UpdatePatCommand(
                  patId = PatId(pat.aggregateIdentifier.identifier.toString()),
                  version = 0,
                  impersonatedUser = user.getIdentifier().asUserId(),
                  description = "Updating another user's PAT",
                  validForMinutes = 60 * 24 * 3,
                  scopes = listOf(GRAPHQL_API_READ),
              ),
          )
        }
        .withMessage("Access Denied")
  }
}
