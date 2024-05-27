/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.template

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.company.messages.StreetAddressAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.COMPANY
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.COMPANY_2
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.CR_USER
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.CSM_EMPLOYEE
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.CSM_USER
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.FM_EMPLOYEE
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.FM_USER
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.FM_USER_INACTIVE
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.OTHER_COMPANY_CR_USER
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.OTHER_COMPANY_FM_USER
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.OTHER_CR_USER
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.OTHER_CSM_USER
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest.Companion.OTHER_FM_USER
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones

fun EventStreamGenerator.setupUsersAndCompanies(
    testadminUserId: String,
    testadminUserIdentifier: String
): EventStreamGenerator {
  submitUserAndActivate(asReference = "testadmin") {
    it.aggregateIdentifierBuilder.identifier = testadminUserIdentifier
    it.userId = testadminUserId
  }
  submitCompany(asReference = COMPANY) {
    it.streetAddress =
        StreetAddressAvro.newBuilder()
            .setArea("1")
            .setCity("2")
            .setHouseNumber("3")
            .setStreet("4")
            .setZipCode("5")
            .setCountry("6")
            .build()
  }
  submitUser(asReference = CSM_USER) {
    it.firstName = "Daniel"
    it.lastName = "Düsentrieb"
    it.email = "daniel@example.com"
  }
  submitEmployee(asReference = CSM_EMPLOYEE)
  submitUser(asReference = OTHER_CSM_USER) {
    it.firstName = "Other"
    it.lastName = "csm-user"
  }
  submitEmployee(asReference = "other-csm-employee")
  submitUser(asReference = CR_USER) {
    it.firstName = "Carlos"
    it.lastName = "Caracho"
  }
  submitEmployee(asReference = "cr-employee")
  submitUser(asReference = OTHER_CR_USER) {
    it.firstName = "Other"
    it.lastName = "cr-user"
  }
  submitEmployee(asReference = "other-cr-employee")
  submitUser(asReference = "cr-user-inactive") {
    it.firstName = "Inactive"
    it.lastName = "CR"
  }
  submitEmployee(asReference = "cr-employee-inactive")
  submitUser(asReference = FM_USER) {
    it.firstName = "Ali"
    it.lastName = "Albatros"
  }
  submitEmployee(asReference = FM_EMPLOYEE)
  submitUser(asReference = OTHER_FM_USER) {
    it.firstName = "Other"
    it.lastName = "fm-user"
  }
  submitEmployee(asReference = "other-fm-employee")
  submitUser(asReference = FM_USER_INACTIVE) {
    it.firstName = "Inactive"
    it.lastName = "FM"
  }
  submitEmployee(asReference = "fm-employee-inactive")
  submitUser("deleted-user") { it.email = "deleted@example.com" }
  submitUserTombstones(reference = "deleted-user")
  submitCompany(asReference = COMPANY_2) {
    it.streetAddress =
        StreetAddressAvro.newBuilder()
            .setArea("1")
            .setCity("2")
            .setHouseNumber("3")
            .setStreet("4")
            .setZipCode("5")
            .setCountry("6")
            .build()
  }
  submitUser(asReference = OTHER_COMPANY_CR_USER) {
    it.firstName = "Other"
    it.lastName = "Company-CR-User"
  }
  submitEmployee(asReference = "other-company-cr-employee")
  submitUser(asReference = OTHER_COMPANY_FM_USER) {
    it.firstName = "Other"
    it.lastName = "Company-FM-User"
  }
  submitEmployee(asReference = "other-company-fm-employee")
  submitCompany(asReference = "company3") {
    it.name = "company3"
    it.streetAddress =
        StreetAddressAvro.newBuilder()
            .setArea("1")
            .setCity("2")
            .setHouseNumber("3")
            .setStreet("4")
            .setZipCode("5")
            .setCountry("6")
            .build()
  }
  setUserContext(CSM_USER)
  return this
}
