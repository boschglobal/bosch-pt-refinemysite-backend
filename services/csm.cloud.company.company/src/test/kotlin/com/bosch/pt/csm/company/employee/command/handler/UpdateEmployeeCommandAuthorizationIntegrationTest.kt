/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.command.handler

import com.bosch.pt.csm.application.security.AuthorizationTestUtils.authorizeWithAdministrativeUser
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.common.AbstractAuthorizationIntegrationTest
import com.bosch.pt.csm.common.facade.rest.UserTypeAccess
import com.bosch.pt.csm.company.employee.command.api.UpdateEmployeeCommand
import com.bosch.pt.csm.company.employee.asEmployeeId
import com.bosch.pt.csm.company.employee.shared.model.EmployeeRoleEnum
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

@DisplayName("Verify authorization of update employee command handler")
class UpdateEmployeeCommandAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: UpdateEmployeeCommandHandler

  @ParameterizedTest
  @MethodSource("authorizedAdminOnly")
  fun `updating employees allowed for admin users only`(userType: UserTypeAccess) {
    checkAccessWith(userType) {
      cut.handle(
          UpdateEmployeeCommand(
              employeeDE.identifier, 0L, listOf(EmployeeRoleEnum.CSM, EmployeeRoleEnum.CA)))
    }
  }

  @Test
  fun `updating employee fails if admin user is authorized for company but not for user`() {
    eventStreamGenerator
        .submitUser("newUser") { it.country = IsoCountryCodeEnumAvro.DE }
        .submitEmployee("newEmployee") {
          it.user = getByReference("newUser")
          it.company = getByReference("companyUS")
        }

    val newEmployee =
        repositories.employeeRepository.findOneByIdentifier(
            eventStreamGenerator.getIdentifier("newEmployee").asEmployeeId())!!

    authorizeWithAdministrativeUser(adminUS)
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      cut.handle(
          UpdateEmployeeCommand(
              newEmployee.identifier, 0L, listOf(EmployeeRoleEnum.CSM, EmployeeRoleEnum.CA)))
    }
  }
}
