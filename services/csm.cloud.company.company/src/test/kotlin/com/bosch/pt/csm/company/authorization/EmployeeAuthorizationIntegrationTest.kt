/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.authorization

import com.bosch.pt.csm.application.security.AuthorizationTestUtils.authorizeWithAdministrativeUser
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.common.AbstractAuthorizationIntegrationTest
import com.bosch.pt.csm.common.facade.rest.UserTypeAccess
import com.bosch.pt.csm.company.employee.query.EmployeeQueryService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

@DisplayName("Verify authorization of employee service")
class EmployeeAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: EmployeeQueryService

  @ParameterizedTest
  @MethodSource("adminOnly")
  fun `find all employees for company allowed for admin users only`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      assertThat(cut.findAllByCompany(companyDE, PageRequest.of(0, 10))).isNotEmpty
    }
  }

  @Test
  fun `find all employees for company returns only authorized items`() {
    addGermanUserToUSCompanyAsCSM()
    authorizeWithAdministrativeUser(adminUS)
    cut.findAllByCompany(companyUS, PageRequest.of(0, 10)).also {
      assertThat(it.content).hasSize(1)
      assertThat(it.content.first().identifier).isEqualTo(employeeUS.identifier)
    }
  }

  @ParameterizedTest
  @MethodSource("authorizedAdminOnly")
  fun `find employee by identifier is allowed for authorized admins only`(
      userType: UserTypeAccess
  ) {
    checkAccessWith(userType) {
      assertThat(cut.findEmployeeWithDetailsByIdentifier(employeeDE.identifier)).isNotNull()
    }
  }

  private fun addGermanUserToUSCompanyAsCSM() =
      eventStreamGenerator.submitUser("userDE_US").submitEmployee("employeeDE_US") {
        it.user = getByReference("userDE_US")
        it.company = getByReference("companyUS")
        it.roles = listOf(EmployeeRoleEnumAvro.CSM)
      }
}
