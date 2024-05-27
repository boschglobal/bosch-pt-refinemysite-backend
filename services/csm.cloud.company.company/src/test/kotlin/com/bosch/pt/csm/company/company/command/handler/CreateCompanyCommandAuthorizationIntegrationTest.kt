/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.company.command.handler

import com.bosch.pt.csm.application.security.AuthorizationTestUtils.authorizeWithAdministrativeUser
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.common.AbstractAuthorizationIntegrationTest
import com.bosch.pt.csm.common.facade.rest.UserTypeAccess
import com.bosch.pt.csm.company.company.command.api.CreateCompanyCommand
import com.bosch.pt.csm.company.company.command.api.ValueObjectTestData
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Verify authorization of create company command handler")
class CreateCompanyCommandAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: CreateCompanyCommandHandler

  @ParameterizedTest
  @MethodSource("adminOnly")
  fun `creating a company allowed for admin users only`(userType: UserTypeAccess) {
    checkAccessWith(userType) { assertThat(cut.handle(newCompanyInGermany())).isNotNull() }
  }

  @Test
  fun `creating company fails if admin user not authorized for country`() {
    authorizeWithAdministrativeUser(adminUS)
    assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
      cut.handle(newCompanyInGermany())
    }
  }

  private fun newCompanyInGermany() =
      CreateCompanyCommand(
          name = "ACME",
          streetAddress = ValueObjectTestData.streetAddress(),
          postBoxAddress = ValueObjectTestData.postBoxAddress()
      )
}
