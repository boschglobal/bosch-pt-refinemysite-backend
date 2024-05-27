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
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.command.api.CreatePatCommand
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.PatScopeEnum.GRAPHQL_API_READ
import com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.model.PatTypeEnum.RMSPAT1
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

class CreatePatCommandHandlerAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: CreatePatCommandHandler

  @Test
  fun `create PAT command for PAT of another user than authenticated is rejected`() {

    val user = get<UserAggregateAvro>("user")!!
    setAuthentication("otherUser")

    assertThatExceptionOfType(AccessDeniedException::class.java)
        .isThrownBy {
          cut.handle(
              CreatePatCommand(
                  impersonatedUser = user.getIdentifier().asUserId(),
                  description = "Updating another user's PAT",
                  validForMinutes = 60 * 24 * 3,
                  scopes = listOf(GRAPHQL_API_READ),
                  type = RMSPAT1,
              ),
          )
        }
        .withMessage("Access Denied")
  }
}
