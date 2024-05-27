/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.company.employee.facade.rest

import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.common.AbstractApiIntegrationTest
import com.bosch.pt.csm.company.employee.facade.rest.resource.request.SearchEmployeesFilterResource
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus.OK

class EmployeeSearchApiIntegrationTest : AbstractApiIntegrationTest() {

  @Autowired private lateinit var cut: EmployeeController

  @BeforeEach
  fun setup() {
    setAuthentication("admin")
    Locale.setDefault(Locale.UK)

    eventStreamGenerator
        .submitCompany("company1") { it.name = "Company 1" }
        .submitUser("user1") {
          it.firstName = "Max"
          it.lastName = "Mustermann"
          it.email = "max.mustermann@web.de"
        }
        .submitEmployee("employee1") { it.roles = listOf(EmployeeRoleEnumAvro.FM) }
        .submitUser("user2") {
          it.firstName = "Maya"
          it.lastName = "Mustermann"
          it.email = "maya.mustermann@web.de"
        }
        .submitEmployee("employee2") { it.roles = listOf(EmployeeRoleEnumAvro.CSM) }
        .submitUser("user3") {
          it.firstName = "Mister"
          it.lastName = "Meister"
          it.email = "mister.meister@web.de"
        }
        .submitEmployee("employee3") { it.roles = listOf(EmployeeRoleEnumAvro.CSM) }
        .submitCompany("company2") { it.name = "Company 2" }
        .submitUser("user4") {
          it.firstName = "Albert"
          it.lastName = "Mustermann"
          it.email = "albert.mustermann@web.de"
        }
        .submitEmployee("employee4") { it.roles = listOf(EmployeeRoleEnumAvro.FM) }
        .submitUser("user5") {
          it.firstName = "Anna"
          it.lastName = "Mustermann"
          it.email = "anna.mustermann@web.de"
        }
        .submitEmployee("employee5") { it.roles = listOf(EmployeeRoleEnumAvro.CSM) }

    companyEventStoreUtils.reset()
  }

  @Test
  fun `verify search employee without parameters`() {
    val filter = SearchEmployeesFilterResource()

    cut.search(filter, PageRequest.of(0, 10, Sort.by("company.displayName", "user.firstName")))
        .also {
          assertThat(it.statusCode).isEqualTo(OK)
          it.body!!.also { body -> assertThat(body.items.size).isEqualTo(5) }
        }
  }

  @Test
  fun `verify search employee by name`() {
    val filter = SearchEmployeesFilterResource(name = "maya")

    cut.search(filter, PageRequest.of(0, 10, Sort.by("company.displayName", "user.firstName")))
        .also {
          assertThat(it.statusCode).isEqualTo(OK)
          it.body!!.also { body ->
            assertThat(body.items.size).isEqualTo(1)
            assertThat(body.items.stream().map { e -> e.user.displayName })
                .containsExactly("Maya Mustermann")
          }
        }
  }

  @Test
  fun `verify search employee by company name`() {
    val filter = SearchEmployeesFilterResource(companyName = "Company 2")

    cut.search(filter, PageRequest.of(0, 10, Sort.by("company.displayName", "user.firstName")))
        .also {
          assertThat(it.statusCode).isEqualTo(OK)
          it.body!!.also { body ->
            assertThat(body.items.size).isEqualTo(2)
            assertThat(body.items.stream().map { e -> e.user.displayName })
                .containsExactly("Albert Mustermann", "Anna Mustermann")
          }
        }
  }

  @Test
  fun `verify search employee by email`() {
    val filter = SearchEmployeesFilterResource(email = "max")

    cut.search(filter, PageRequest.of(0, 10, Sort.by("company.displayName", "user.firstName")))
        .also {
          assertThat(it.statusCode).isEqualTo(OK)
          it.body!!.also { body ->
            assertThat(body.items.size).isEqualTo(1)
            assertThat(body.items.stream().map { e -> e.user.displayName })
                .containsExactly("Max Mustermann")
          }
        }
  }

  @Test
  fun `verify search employee by company and email`() {
    val filter = SearchEmployeesFilterResource(email = "mis", companyName = "Company 1")

    cut.search(filter, PageRequest.of(0, 10, Sort.by("company.displayName", "user.firstName")))
        .also {
          assertThat(it.statusCode).isEqualTo(OK)
          it.body!!.also { body ->
            assertThat(body.items.size).isEqualTo(1)
            assertThat(body.items.stream().map { e -> e.user.displayName })
                .containsExactly("Mister Meister")
          }
        }
  }
}
