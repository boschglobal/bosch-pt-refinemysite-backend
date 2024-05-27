/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.company.command.handler

import com.bosch.pt.csm.application.security.AuthorizationTestUtils.authorizeWithAdministrativeUser
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.common.AbstractAuthorizationIntegrationTest
import com.bosch.pt.csm.common.facade.rest.UserTypeAccess
import com.bosch.pt.csm.company.company.command.api.DeleteCompanyCommand
import com.bosch.pt.csm.company.company.asCompanyId
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

@DisplayName("Verify authorization of update company command handler")
class DeleteCompanyCommandAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: DeleteCompanyCommandHandler

  private val companyIdentifier by lazy { getIdentifier("companyDEWithoutEmployees").asCompanyId() }

  @BeforeEach
  fun setup() {
    eventStreamGenerator.submitCompany("companyDEWithoutEmployees") {
      it.postBoxAddress = createPostBoxAddress(IsoCountryCodeEnum.DE)
      it.streetAddress = createStreetAddress(IsoCountryCodeEnum.DE)
    }
  }

  @ParameterizedTest
  @MethodSource("authorizedAdminOnly")
  fun `deleting a company allowed for authorized admin users only`(userType: UserTypeAccess) {
    checkAccessWith(userType) { cut.handle(DeleteCompanyCommand(companyIdentifier)) }
  }

  @Test
  fun `deleting company fails if admin user not authorized for country used in company address`() {
    authorizeWithAdministrativeUser(adminUS)
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      cut.handle(DeleteCompanyCommand(companyIdentifier))
    }
  }
}
