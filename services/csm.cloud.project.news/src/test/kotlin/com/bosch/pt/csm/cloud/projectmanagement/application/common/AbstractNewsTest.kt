/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.common

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.util.HttpTestUtils.setFakeUrlWithApiVersion
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.ObjectRelationRepository
import com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.NewsController
import com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.resource.NewsResource
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.news.repository.NewsRepository
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CR
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.FM
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.ACTIVE
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.INVITED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.VALIDATION
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.repository.ParticipantMappingRepository
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import java.time.Instant
import java.util.UUID.randomUUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.provider.Arguments
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@Suppress("UnnecessaryAbstractClass")
@SmartSiteSpringBootTest
abstract class AbstractNewsTest : AbstractEventStreamIntegrationTest() {

  @Autowired lateinit var controller: NewsController

  @Autowired lateinit var newsRepository: NewsRepository

  @Autowired lateinit var objectRelationRepository: ObjectRelationRepository

  @Autowired lateinit var participantMappingRepository: ParticipantMappingRepository

  val compactedUserIdentifier: AggregateIdentifierAvro =
      AggregateIdentifierAvro(
          randomUUID().toString(), 0, UsermanagementAggregateTypeEnum.USER.value)

  @Value("\${testadmin.user.id}") lateinit var testadminUserId: String

  lateinit var projectAggregateIdentifier: AggregateIdentifierAvro
  lateinit var taskAggregateIdentifier: AggregateIdentifierAvro

  lateinit var participantCsm1: ParticipantAggregateG3Avro
  lateinit var participantCsm2: ParticipantAggregateG3Avro
  lateinit var participantCr1: ParticipantAggregateG3Avro
  lateinit var participantCr2: ParticipantAggregateG3Avro
  lateinit var participantFm: ParticipantAggregateG3Avro

  @BeforeEach
  fun beforeEach() {
    context.clear()
    setFakeUrlWithApiVersion()
    submitUserEvents()
    submitCompanyEvents()
    submitProjectEvents()
    eventStreamGenerator.setUserContext("csm-user-1")
  }

  fun buildNews(
      context: AggregateIdentifierAvro,
      parent: AggregateIdentifierAvro,
      root: AggregateIdentifierAvro,
      createdDate: Instant? = null,
      lastModifiedDate: Instant? = null
  ) =
      NewsResource(
          ObjectIdentifier(context),
          ObjectIdentifier(parent),
          ObjectIdentifier(root),
          createdDate,
          lastModifiedDate)

  private fun submitUserEvents() {
    eventStreamGenerator
        .submitUserAndActivate(asReference = "testadmin") { it.userId = testadminUserId }
        .submitUser(asReference = "csm-user-1") {
          it.firstName = "Daniel"
          it.lastName = "Düsentrieb"
        }
        .submitUser(asReference = "csm-user-2") {
          it.firstName = "Other"
          it.lastName = "CSM-User"
        }
        .submitUser(asReference = "cr-user-1") {
          it.firstName = "Carlos"
          it.lastName = "Caracho"
        }
        .submitUser(asReference = "cr-user-2") {
          it.firstName = "Other"
          it.lastName = "CR-User"
        }
        .submitUser(asReference = "cr-user-inactive") {
          it.firstName = "Inactive"
          it.lastName = "CR"
        }
        .submitUser(asReference = "fm-user") {
          it.firstName = "Ali"
          it.lastName = "Albatros"
        }
        .submitUser(asReference = "fm-user-inactive") {
          it.firstName = "Inactive"
          it.lastName = "FM"
        }
        .submitUser(asReference = "other-company-cr-user") {
          it.firstName = "Other"
          it.lastName = "Company-CR-User"
        }
        .submitUser(asReference = "other-company-fm-user") {
          it.firstName = "Other"
          it.lastName = "FM-User"
        }
  }

  private fun submitCompanyEvents() {
    eventStreamGenerator
        .submitCompany()
        .submitEmployee(asReference = "csm-employee-1") { it.user = getByReference("csm-user-1") }
        .submitEmployee(asReference = "csm-employee-2") { it.user = getByReference("csm-user-2") }
        .submitEmployee(asReference = "cr-employee-1") { it.user = getByReference("cr-user-1") }
        .submitEmployee(asReference = "cr-employee-2") { it.user = getByReference("cr-user-2") }
        .submitEmployee(asReference = "cr-employee-inactive") {
          it.user = getByReference("cr-user-inactive")
        }
        .submitEmployee(asReference = "fm-employee") { it.user = getByReference("fm-user") }
        .submitEmployee(asReference = "fm-employee-inactive") {
          it.user = getByReference("fm-user-inactive")
        }
        .submitCompany(asReference = "other-company")
        .submitEmployee(asReference = "other-company-cr-employee") {
          it.company = getByReference("other-company")
          it.user = getByReference("other-company-cr-user")
        }
        .submitEmployee(asReference = "other-company-fm-employee") {
          it.company = getByReference("other-company")
          it.user = getByReference("other-company-fm-user")
        }
        .submitCompany(asReference = "company-for-compacted-user")
        .submitEmployee(asReference = "employee-for-compacted-user") {
          it.company = getByReference("company-for-compacted-user")
          it.user = compactedUserIdentifier
        }

    employeeCsm1 = context["csm-employee-1"] as EmployeeAggregateAvro
    employeeCsm2 = context["csm-employee-2"] as EmployeeAggregateAvro
    employeeCr1 = context["cr-employee-1"] as EmployeeAggregateAvro
    employeeCr2 = context["cr-employee-2"] as EmployeeAggregateAvro
    employeeFm = context["fm-employee"] as EmployeeAggregateAvro
  }

  private fun submitProjectEvents() {
    eventStreamGenerator
        .setUserContext("csm-user-1")
        .submitProject()
        .submitProjectCraftG2()
        .submitParticipantG3(asReference = "csm-participant-1") {
          it.user = getByReference("csm-user-1")
          it.role = CSM
          it.status = INVITED
        }
        .submitParticipantG3(asReference = "csm-participant-1") {
          it.user = getByReference("csm-user-1")
          it.role = CSM
          it.status = VALIDATION
        }
        .submitParticipantG3(asReference = "csm-participant-1") {
          it.user = getByReference("csm-user-1")
          it.role = CSM
          it.status = ACTIVE
        }
        .submitParticipantG3(asReference = "csm-participant-2") {
          it.user = getByReference("csm-user-2")
          it.role = CSM
        }
        .submitParticipantG3(asReference = "cr-participant-inactive") {
          it.user = getByReference("cr-user-inactive")
          it.role = CR
        }
        .submitParticipantG3(
            asReference = "cr-participant-inactive",
            eventType = ParticipantEventEnumAvro.DEACTIVATED) {
          it.user = getByReference("cr-user-inactive")
          it.role = CR
        }
        .submitParticipantG3(asReference = "cr-participant-1") {
          it.user = getByReference("cr-user-1")
          it.role = CR
        }
        .submitParticipantG3(asReference = "cr-participant-2") {
          it.user = getByReference("cr-user-2")
          it.role = CR
        }
        .submitParticipantG3(asReference = "fm-participant") {
          it.user = getByReference("fm-user")
          it.role = FM
        }
        .submitParticipantG3(asReference = "fm-participant-inactive") {
          it.user = getByReference("fm-user-inactive")
          it.role = FM
        }
        .submitParticipantG3(asReference = "fm-participant-inactive") {
          it.user = getByReference("fm-user-inactive")
          it.role = FM
        }
        .submitParticipantG3(
            asReference = "fm-participant-inactive",
            eventType = ParticipantEventEnumAvro.DEACTIVATED) {
          it.user = getByReference("fm-user-inactive")
          it.role = FM
        }
        .setUserContext("csm-user-1")
        .submitTask { it.assignee = getByReference("fm-participant") }

    projectAggregateIdentifier = getByReference("project")

    participantCsm1 = context["csm-participant-1"] as ParticipantAggregateG3Avro
    participantCsm2 = context["csm-participant-2"] as ParticipantAggregateG3Avro
    participantCr1 = context["cr-participant-1"] as ParticipantAggregateG3Avro
    participantCr2 = context["cr-participant-2"] as ParticipantAggregateG3Avro
    participantFm = context["fm-participant"] as ParticipantAggregateG3Avro

    taskAggregateIdentifier = getByReference("task")
  }

  companion object {

    lateinit var employeeCsm1: EmployeeAggregateAvro
    lateinit var employeeCsm2: EmployeeAggregateAvro
    lateinit var employeeCr1: EmployeeAggregateAvro
    lateinit var employeeCr2: EmployeeAggregateAvro
    lateinit var employeeFm: EmployeeAggregateAvro

    const val employeesCsmCr =
        "com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractNewsTest#employeesCsmCr"
    const val employeesCsmCrFm =
        "com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractNewsTest#employeesCsmCrFm"

    @JvmStatic
    fun employeesCsmCr() =
        listOf(
            Arguments.of("another construction site manager", { employeeCsm2 }),
            Arguments.of("the company representative", { employeeCr1 }),
            Arguments.of("another company representative", { employeeCr2 }))

    @JvmStatic
    fun employeesCsmCrFm() = employeesCsmCr().plus(Arguments.of("the foreman", { employeeFm }))
  }
}
