/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.company.command.handler

import com.bosch.pt.csm.application.security.AuthorizationTestUtils.authorizeWithAdministrativeUser
import com.bosch.pt.csm.common.AbstractAuthorizationIntegrationTest
import com.bosch.pt.csm.common.facade.rest.UserTypeAccess
import com.bosch.pt.csm.company.company.command.api.UpdateCompanyCommand
import com.bosch.pt.csm.company.company.command.snapshotstore.toPostBoxAddressVo
import com.bosch.pt.csm.company.company.command.snapshotstore.toStreetAddressVo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

@DisplayName("Verify authorization of company delete command handler")
class UpdateCompanyCommandAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: UpdateCompanyCommandHandler

  @ParameterizedTest
  @MethodSource("authorizedAdminOnly")
  fun `updating a company allowed for authorized admin users only`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      assertThat(
              cut.handle(
                  UpdateCompanyCommand(
                      identifier = companyDE.identifier,
                      version = 0,
                      name = "New Name",
                      streetAddress = companyDE.streetAddress!!.toStreetAddressVo(),
                      postBoxAddress = companyDE.postBoxAddress!!.toPostBoxAddressVo())
              ))
          .isNotNull()
    }
  }

  @Test
  fun `updating company address fails if admin user not authorized for country`() {
    authorizeWithAdministrativeUser(adminUS)
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      cut.handle(
          UpdateCompanyCommand(
              identifier = companyDE.identifier,
              version = 0,
              name = "New Name",
              streetAddress = companyDE.streetAddress!!.toStreetAddressVo(),
              postBoxAddress = companyDE.postBoxAddress!!.toPostBoxAddressVo())
      )
    }
  }
}
