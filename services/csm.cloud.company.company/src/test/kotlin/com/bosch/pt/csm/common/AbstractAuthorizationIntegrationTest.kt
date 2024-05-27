/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.common

import com.bosch.pt.csm.application.security.AuthorizationTestUtils.doWithAuthorization
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.DE
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro.US
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.company.messages.PostBoxAddressAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.StreetAddressAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.common.facade.rest.UserTypeAccess
import com.bosch.pt.csm.common.facade.rest.UserTypeAccess.Companion.createGrantedGroup
import com.bosch.pt.csm.company.company.asCompanyId
import com.bosch.pt.csm.company.company.shared.model.Company
import com.bosch.pt.csm.company.employee.asEmployeeId
import com.bosch.pt.csm.company.employee.shared.model.Employee
import com.bosch.pt.csm.user.authorization.model.UserCountryRestriction
import com.bosch.pt.csm.user.user.query.UserProjection
import java.util.concurrent.Executors
import java.util.stream.Stream
import org.apache.commons.lang3.tuple.Pair
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.springframework.security.access.AccessDeniedException

/** Server side base class for testing database based tests. */
abstract class AbstractAuthorizationIntegrationTest : AbstractApiIntegrationTest() {

  var userMap: MutableMap<String, Pair<UserProjection, Boolean>> = HashMap()

  lateinit var admin: UserProjection
  lateinit var adminDE: UserProjection
  lateinit var adminUS: UserProjection

  lateinit var userDE: UserProjection
  lateinit var userUS: UserProjection

  lateinit var companyDE: Company
  lateinit var companyUS: Company
  lateinit var employeeDE: Employee
  lateinit var employeeUS: Employee

  @BeforeEach
  protected fun initAbstractAuthorizationIntegrationTest() {
    eventStreamGenerator
        .setUserContext("system")
        .submitUser("admin") {
          it.userId = "admin"
          it.admin = true
          it.country = DE
        }
        .submitUser("adminDE") {
          it.userId = "adminDE"
          it.admin = true
          it.country = DE
        }
        .submitUser("adminUS") {
          it.userId = "adminUS"
          it.admin = true
          it.country = US
        }
        .submitUser("userDE") { it.country = DE }
        .submitCompany("companyDE") {
          it.postBoxAddress = createPostBoxAddress(IsoCountryCodeEnum.DE)
          it.streetAddress = createStreetAddress(IsoCountryCodeEnum.DE)
        }
        .submitEmployee("employeeDE") { it.roles = listOf(CSM) }
        .submitUser("userUS") { it.country = US }
        .submitCompany("companyUS") {
          it.postBoxAddress = createPostBoxAddress(IsoCountryCodeEnum.US)
          it.streetAddress = createStreetAddress(IsoCountryCodeEnum.US)
        }
        .submitEmployee("employeeUS") { it.roles = listOf(CSM) }

    admin = getUser("admin")
    adminDE = getUser("adminDE")
    adminUS = getUser("adminUS")
    userDE = getUser("userDE")
    userUS = getUser("userUS")
    companyDE = getCompany("companyDE")
    companyUS = getCompany("companyUS")
    employeeDE = getEmployee("employeeDE")
    employeeUS = getEmployee("employeeUS")

    userMap[ADMIN] = Pair.of(admin, true)
    userMap[ADMIN_DE] = Pair.of(adminDE, true)
    userMap[ADMIN_US] = Pair.of(adminUS, true)
    userMap[USER_DE] = Pair.of(userDE, false)
    userMap[USER_US] = Pair.of(userUS, false)

    UserCountryRestriction(eventStreamGenerator.getIdentifier("adminDE"), IsoCountryCodeEnum.DE)
        .apply { repositories.userCountryRestrictionRepository.save(this) }

    UserCountryRestriction(eventStreamGenerator.getIdentifier("adminUS"), IsoCountryCodeEnum.US)
        .apply { repositories.userCountryRestrictionRepository.save(this) }

    setAuthentication("userDE")
  }

  protected fun checkAccessWith(userTypeAccess: UserTypeAccess, runnable: Runnable) {
    checkAccessWith(userTypeAccess.userType, userTypeAccess.isAccessGranted, runnable)
  }

  private fun checkAccessWith(userRole: String, isAccessGranted: Boolean, procedure: Runnable) {
    if (isAccessGranted) {
      doWithAuthorization(userMap[userRole]!!.left, Executors.callable(procedure))
    } else {
      doWithAuthorization(userMap[userRole]!!.left) {
        Assertions.assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
          procedure.run()
        }
      }
    }
  }

  private fun getUser(reference: String) =
      repositories.userProjectionRepository.findOneById(
          eventStreamGenerator.getIdentifier(reference).asUserId())!!

  protected fun getCompany(reference: String) =
      repositories.companyRepository.findOneByIdentifier(
          eventStreamGenerator.getIdentifier(reference).asCompanyId())!!

  private fun getEmployee(reference: String) =
      repositories.employeeRepository.findOneByIdentifier(
          eventStreamGenerator.getIdentifier(reference).asEmployeeId())!!

  protected fun createPostBoxAddress(country: IsoCountryCodeEnum) =
      PostBoxAddressAvro.newBuilder()
          .setZipCode("12345")
          .setCity("TestCity")
          .setArea("TestArea")
          .setCountry(country.alternativeCountryName)
          .setPostBox("TestPostBox")
          .build()

  protected fun createStreetAddress(country: IsoCountryCodeEnum) =
      StreetAddressAvro.newBuilder()
          .setStreet("TestStreet")
          .setHouseNumber("1")
          .setZipCode("12345")
          .setCity("TestCity")
          .setArea("TestArea")
          .setCountry(country.alternativeCountryName)
          .build()

  companion object {
    const val ADMIN = "ADMIN"
    const val ADMIN_DE = "ADMIN_DE"
    const val ADMIN_US = "ADMIN_US"
    const val USER_DE = "USER_DE"
    const val USER_US = "USER_US"

    @JvmStatic
    fun adminOnly(): Stream<UserTypeAccess> =
        createGrantedGroup(arrayOf(ADMIN, USER_DE, USER_US), setOf(ADMIN))

    @JvmStatic
    fun authorizedAdminOnly(): Stream<UserTypeAccess> =
        createGrantedGroup(
            arrayOf(ADMIN, ADMIN_DE, ADMIN_US, USER_DE, USER_US), setOf(ADMIN, ADMIN_DE))
  }
}
