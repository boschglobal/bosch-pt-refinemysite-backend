/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.facade.rest

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import com.bosch.pt.csm.common.AbstractApiIntegrationTest
import com.bosch.pt.csm.common.i18n.Key.EMPLOYEE_VALIDATION_ERROR_EXIST_USER_EMPLOYEE
import com.bosch.pt.csm.common.i18n.Key.EMPLOYEE_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.csm.company.company.asCompanyId
import com.bosch.pt.csm.company.employee.facade.rest.resource.request.SaveEmployeeResource
import com.bosch.pt.csm.company.employee.shared.model.EmployeeRoleEnum
import java.util.Locale
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus.OK

class EmployeeApiIntegrationTest : AbstractApiIntegrationTest() {

  @Autowired private lateinit var cut: EmployeeController

  @BeforeEach
  fun setup() {
    setAuthentication("admin")
    Locale.setDefault(Locale.UK)
  }

  @Test
  fun `verify create employee fails due to existing one`() {
    eventStreamGenerator
        .submitCompany("company")
        .submitUser("user")
        .submitEmployee("employee") { it.roles = listOf(EmployeeRoleEnumAvro.FM) }
        .submitCompany("company2")

    val company2 = eventStreamGenerator.get<CompanyAggregateAvro>("company2")!!
    val user = eventStreamGenerator.get<UserAggregateAvro>("user")!!

    val employeeResource =
        SaveEmployeeResource(user.getIdentifier().asUserId(), listOf(EmployeeRoleEnum.FM))

    assertThat(
            catchThrowableOfType(
                    {
                      cut.createEmployee(
                          company2.getIdentifier().asCompanyId(), null, employeeResource)
                    },
                    PreconditionViolationException::class.java)
                .messageKey)
        .isEqualTo(EMPLOYEE_VALIDATION_ERROR_EXIST_USER_EMPLOYEE)
  }

  @Test
  fun `verify find employees for all roles`() {
    eventStreamGenerator
        .submitCompany("company")
        .submitUser("user1")
        .submitEmployee("employee1") { it.roles = listOf(CSM) }
        .submitUser("user2")
        .submitEmployee("employee2") { it.roles = listOf(EmployeeRoleEnumAvro.CR) }
        .submitUser("user3")
        .submitEmployee("employee3") { it.roles = listOf(EmployeeRoleEnumAvro.FM) }

    val company = eventStreamGenerator.get<CompanyAggregateAvro>("company")!!

    cut.findEmployeesByCompany(company.getIdentifier(), Pageable.unpaged()).also {
      assertThat(it.statusCode).isEqualTo(OK)
      it.body!!.also { body -> assertThat(body.items.size).isEqualTo(3) }
    }
  }

  @Test
  fun `verify find no employee for null user`() {
    eventStreamGenerator.submitCompany("company").submitUser("user1").submitEmployee("employee1") {
      it.roles = listOf(CSM)
    }

    getContext().useOnlineListener()
    eventStreamGenerator.submitUserTombstones("user1")

    assertThatThrownBy { cut.findEmployeeById(getIdentifier("employee1")) }
        .isInstanceOf(AggregateNotFoundException::class.java)
  }

  @Test
  fun `verify find employee not found`() {
    assertThat(
            catchThrowableOfType(
                    { cut.findEmployeeById(UUID.randomUUID()) },
                    AggregateNotFoundException::class.java)
                .messageKey)
        .isEqualTo(EMPLOYEE_VALIDATION_ERROR_NOT_FOUND)
  }

  @Test
  fun `verify delete employee fails for non existing employee`() {
    assertThat(
            catchThrowableOfType(
                    { cut.deleteEmployee(UUID.randomUUID()) },
                    AggregateNotFoundException::class.java)
                .messageKey)
        .isEqualTo(EMPLOYEE_VALIDATION_ERROR_NOT_FOUND)
  }
}
