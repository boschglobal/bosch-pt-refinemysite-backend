/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.command.handler

import com.bosch.pt.csm.application.security.AuthorizationTestUtils
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.common.AbstractAuthorizationIntegrationTest
import com.bosch.pt.csm.common.exceptions.ReferencedEntityNotFoundException
import com.bosch.pt.csm.common.facade.rest.UserTypeAccess
import com.bosch.pt.csm.company.company.asCompanyId
import com.bosch.pt.csm.company.employee.command.api.CreateEmployeeCommand
import com.bosch.pt.csm.company.employee.shared.model.EmployeeRoleEnum
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Verify authorization of create employee command handler")
class CreateEmployeeCommandAuthorizationIntegrationTest : AbstractAuthorizationIntegrationTest() {

  @Autowired private lateinit var cut: CreateEmployeeCommandHandler

  @ParameterizedTest
  @MethodSource("adminOnly")
  fun `creating an employee allowed for admin users only`(userType: UserTypeAccess) {

    eventStreamGenerator.submitUser("newUser")
    val userIdentifier = eventStreamGenerator.getIdentifier("newUser").asUserId()

    checkAccessWith(userType) {
      Assertions.assertThat(
              cut.handle(
                  CreateEmployeeCommand(
                      userRef = userIdentifier,
                      companyRef = companyDE.identifier,
                      roles = listOf(EmployeeRoleEnum.FM))))
          .isNotNull()
    }
  }

  @Test
  fun `creating an employee cannot be done for non-existing user`() {

    AuthorizationTestUtils.authorizeWithAdministrativeUser(adminDE)
    Assertions.assertThatExceptionOfType(ReferencedEntityNotFoundException::class.java).isThrownBy {
      cut.handle(
          CreateEmployeeCommand(
              userRef = randomUUID().asUserId(),
              companyRef = companyDE.identifier,
              roles = listOf(EmployeeRoleEnum.FM)))
    }
  }

  @Test
  fun `creating an employee cannot be done for non-existing company`() {

    eventStreamGenerator.submitUser("newUser")
    val userIdentifier = eventStreamGenerator.getIdentifier("newUser").asUserId()

    AuthorizationTestUtils.authorizeWithAdministrativeUser(adminDE)
    Assertions.assertThatExceptionOfType(ReferencedEntityNotFoundException::class.java).isThrownBy {
      cut.handle(
          CreateEmployeeCommand(
              userRef = userIdentifier,
              companyRef = randomUUID().asCompanyId(),
              roles = listOf(EmployeeRoleEnum.FM)))
    }
  }

  @Test
  fun `creating employee fails if admin user not authorized for company country`() {
    AuthorizationTestUtils.authorizeWithAdministrativeUser(adminUS)
    Assertions.assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
      cut.handle(
          CreateEmployeeCommand(
              userRef = userUS.id,
              companyRef = companyDE.identifier,
              roles = listOf(EmployeeRoleEnum.FM)))
    }
  }

  @Test
  fun `creating employee fails if admin user not authorized for user country`() {
    AuthorizationTestUtils.authorizeWithAdministrativeUser(adminDE)
    Assertions.assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
      cut.handle(
          CreateEmployeeCommand(
              userRef = userUS.id,
              companyRef = companyDE.identifier,
              roles = listOf(EmployeeRoleEnum.FM)))
    }
  }
}
