/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.TimeLineGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.util.HttpTestUtils.setFakeUrlWithApiVersion
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.config.EnableKafkaListeners
import com.bosch.pt.csm.cloud.projectmanagement.common.extensions.DefaultLocaleExtension
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CR
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.FM
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.ACTIVE
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.INVITED
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro.VALIDATION
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.User
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import java.time.LocalDate
import java.util.UUID
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder

@Suppress("UnnecessaryAbstractClass")
@EnableKafkaListeners
@SmartSiteSpringBootTest
@ExtendWith(DefaultLocaleExtension::class)
abstract class AbstractStatisticsIntegrationTest {

  @Autowired protected lateinit var timeLineGenerator: TimeLineGenerator
  @Autowired protected lateinit var repositories: Repositories
  @Autowired protected lateinit var eventStreamGenerator: EventStreamGenerator

  var companyIdentifierToNameMap = mutableMapOf<AggregateIdentifierAvro, String>()
  var craftIdentifierToNameMap = mutableMapOf<AggregateIdentifierAvro, String>()

  protected val startDate: LocalDate by lazy { LocalDate.ofYearDay(2018, 1) }

  protected val companyA: AggregateIdentifierAvro by lazy {
    eventStreamGenerator.getByReference("company-a")
  }
  protected val companyB: AggregateIdentifierAvro by lazy {
    eventStreamGenerator.getByReference("company-b")
  }

  protected val employeeCsmA: AggregateIdentifierAvro by lazy {
    eventStreamGenerator.getByReference("employee-csm-a")
  }
  protected val employeeCrA: AggregateIdentifierAvro by lazy {
    eventStreamGenerator.getByReference("employee-cr-a")
  }
  protected val employeeFmA: AggregateIdentifierAvro by lazy {
    eventStreamGenerator.getByReference("employee-fm-a")
  }
  protected val employeeCsmB: AggregateIdentifierAvro by lazy {
    eventStreamGenerator.getByReference("employee-csm-b")
  }
  protected val employeeCrB: AggregateIdentifierAvro by lazy {
    eventStreamGenerator.getByReference("employee-cr-b")
  }
  protected val employeeFmB: AggregateIdentifierAvro by lazy {
    eventStreamGenerator.getByReference("employee-fm-b")
  }

  protected val craft1: AggregateIdentifierAvro by lazy {
    eventStreamGenerator.getByReference("craft-1")
  }

  protected val craft2: AggregateIdentifierAvro by lazy {
    eventStreamGenerator.getByReference("craft-2")
  }

  protected val project: AggregateIdentifierAvro by lazy {
    eventStreamGenerator.getByReference("project")
  }

  protected val craftA: AggregateIdentifierAvro by lazy {
    eventStreamGenerator.getByReference("craft-1")
  }
  protected val craftB: AggregateIdentifierAvro by lazy {
    eventStreamGenerator.getByReference("craft-2")
  }

  protected val participantCsmA: AggregateIdentifierAvro by lazy {
    eventStreamGenerator.getByReference("participant-csm-a")
  }
  protected val participantCrA: AggregateIdentifierAvro by lazy {
    eventStreamGenerator.getByReference("participant-cr-a")
  }
  protected val participantFmA: AggregateIdentifierAvro by lazy {
    eventStreamGenerator.getByReference("participant-fm-a")
  }

  protected val participantFmB: AggregateIdentifierAvro by lazy {
    eventStreamGenerator.getByReference("participant-fm-b")
  }

  @BeforeEach
  fun beforeEach() {
    setFakeUrlWithApiVersion()
    timeLineGenerator.reset()
    submitUserEvents()
    submitCompanyEvents()
    submitProjectEvents()
  }

  @AfterEach
  fun cleanupBase() {
    SecurityContextHolder.clearContext()
    repositories.truncateDatabase()
  }

  protected fun initSecurityContext(userId: UUID, admin: Boolean) {
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(
            User("TEST", userId),
            "n/a",
            AuthorityUtils.createAuthorityList(if (admin) "ROLE_ADMIN" else "ROLE_USER"))
  }

  protected fun initSecurityContext() {
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(
            User("random", UUID.randomUUID()),
            "n/a",
            AuthorityUtils.createAuthorityList("ROLE_ADMIN"))
  }

  private fun submitUserEvents() {
    eventStreamGenerator
        .submitUser(asReference = "user-csm-a")
        .submitUser(asReference = "user-csm-b")
        .submitUser(asReference = "user-cr-a")
        .submitUser(asReference = "user-cr-b")
        .submitUser(asReference = "user-fm-a")
        .submitUser(asReference = "user-fm-b")
  }

  private fun submitCompanyEvents() {
    eventStreamGenerator
        .setUserContext("user-csm-a")
        .submitCompany(asReference = "company-a") { it.name = "company-a" }
        .submitEmployee(asReference = "employee-csm-a")
        .submitEmployee(asReference = "employee-cr-a")
        .submitEmployee(asReference = "employee-fm-a")
        .submitCompany(asReference = "company-b") { it.name = "company-b" }
        .submitEmployee(asReference = "employee-csm-b")
        .submitEmployee(asReference = "employee-cr-b")
        .submitEmployee(asReference = "employee-fm-b")

    companyIdentifierToNameMap[companyA] = "company-a"
    companyIdentifierToNameMap[companyB] = "company-b"
  }

  private fun submitProjectEvents() {
    eventStreamGenerator.submitProject()

    submitParticipants()

    eventStreamGenerator
        .setUserContext("user-csm-a")
        .submitProjectCraftG2(asReference = "craft-1") { it.name = "craft-1" }
        .setUserContext("user-csm-b")
        .submitProjectCraftG2(asReference = "craft-2") { it.name = "craft-2" }

    submitTasks()

    craftIdentifierToNameMap[craftA] = "craft-1"
    craftIdentifierToNameMap[craftB] = "craft-2"
  }

  private fun submitTasks() {
    eventStreamGenerator
        .submitTask(asReference = "task-1") {
          it.craft = craft1
          it.assignee = participantFmA
        }
        .submitTask(asReference = "task-2") {
          it.craft = craft2
          it.assignee = participantFmB
        }
        .submitTask(asReference = "task-3") {
          it.craft = craft1
          it.assignee = participantFmA
        }
        .submitTask(
            asReference = "task-3",
            eventType = TaskEventEnumAvro.UNASSIGNED,
        ) {
          it.craft = craft1
          it.assignee = null
        }
  }

  private fun submitParticipants() {
    submitParticipant("participant-csm-a", companyA, employeeCsmA, CSM, INVITED)
    submitParticipant("participant-csm-a", companyA, employeeCsmA, CSM, VALIDATION)
    submitParticipant("participant-csm-a", companyA, employeeCsmA, CSM, ACTIVE)
    submitParticipantWithoutStatus("participant-cr-a", companyA, employeeCrA, CR)
    submitParticipantWithoutStatus("participant-fm-a", companyA, employeeFmA, FM)
    submitParticipantWithoutStatus("participant-csm-b", companyB, employeeCsmB, CSM)
    submitParticipantWithoutStatus("participant-cr-b", companyB, employeeCrB, CR)
    submitParticipantWithoutStatus("participant-fm-b", companyB, employeeFmB, FM)
  }

  private fun submitParticipant(
      participantName: String,
      company: AggregateIdentifierAvro,
      employeeCsm: AggregateIdentifierAvro,
      participantRole: ParticipantRoleEnumAvro,
      participantStatus: ParticipantStatusEnumAvro
  ) {
    eventStreamGenerator.submitParticipantG3(participantName) {
      it.company = company
      it.user = employeeCsm
      it.role = participantRole
      it.status = participantStatus
    }
  }

  private fun submitParticipantWithoutStatus(
      participantName: String,
      company: AggregateIdentifierAvro,
      employeeCsm: AggregateIdentifierAvro,
      participantRole: ParticipantRoleEnumAvro,
  ) {
    eventStreamGenerator.submitParticipantG3(participantName) {
      it.company = company
      it.user = employeeCsm
      it.role = participantRole
    }
  }
}
