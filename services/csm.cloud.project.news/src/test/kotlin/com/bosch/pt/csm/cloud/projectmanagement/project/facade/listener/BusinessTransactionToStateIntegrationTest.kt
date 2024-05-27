/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.facade.listener

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.util.HttpTestUtils.setFakeUrlWithApiVersion
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractEventStreamIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.MESSAGE
import com.bosch.pt.csm.cloud.projectmanagement.common.getDetails
import com.bosch.pt.csm.cloud.projectmanagement.event.facade.listener.LiveUpdateEventProcessor
import com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.NewsController
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CR
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.FM
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCopyFinished
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCopyStarted
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectImportFinished
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectImportStarted
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@SmartSiteSpringBootTest
class BusinessTransactionToStateIntegrationTest : AbstractEventStreamIntegrationTest() {

  @Value("\${testadmin.user.id}") private lateinit var testadminUserId: String

  @Autowired private lateinit var liveUpdateEventProcessor: LiveUpdateEventProcessor

  @Autowired private lateinit var controller: NewsController

  @BeforeEach
  fun beforeEach() {
    context.clear()
    setFakeUrlWithApiVersion()

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
        .submitUser(asReference = "fm-user") {
          it.firstName = "Ali"
          it.lastName = "Albatros"
        }
        .submitCompany()
        .submitEmployee(asReference = "csm-employee-1") { it.user = getByReference("csm-user-1") }
        .submitEmployee(asReference = "csm-employee-2") { it.user = getByReference("csm-user-2") }
        .submitEmployee(asReference = "cr-employee-1") { it.user = getByReference("cr-user-1") }
        .submitEmployee(asReference = "fm-employee") { it.user = getByReference("fm-user") }
        .setUserContext("csm-user-1")
        .submitProject()
        .submitProjectCraftG2()
        .submitParticipantG3(asReference = "csm-participant-1") {
          it.user = getByReference("csm-user-1")
          it.role = CSM
        }
        .submitParticipantG3(asReference = "csm-participant-2") {
          it.user = getByReference("csm-user-2")
          it.role = CSM
        }
        .submitParticipantG3(asReference = "cr-participant-1") {
          it.user = getByReference("cr-user-1")
          it.role = CR
        }
        .submitParticipantG3(asReference = "fm-participant") {
          it.user = getByReference("fm-user")
          it.role = FM
        }

    employeeCsm2 = context["csm-employee-2"] as EmployeeAggregateAvro
    employeeCr = context["cr-employee-1"] as EmployeeAggregateAvro
    employeeFm = context["fm-employee"] as EmployeeAggregateAvro

    clearMocks(liveUpdateEventProcessor)
  }

  @MethodSource(employees)
  @ParameterizedTest(name = "for {0}")
  fun `verify state is updated but not news and live updates are created for project import`(
      @Suppress("UNUSED_PARAMETER") role: String,
      employee: () -> EmployeeAggregateAvro
  ) {

    eventStreamGenerator
        .submitProjectImportStarted()
        .submitTask()
        .submitTaskSchedule()
        .submitTopicG2()
        .submitTopicAttachment()
        .submitMessage()
        .submitMessageAttachment()
        .submitProjectImportFinished()

    val taskAggregateIdentifier = getByReference("task")
    val messageAttachmentAggregateIdentifier = getByReference("messageAttachment")

    assertThat(controller.getDetails(employee, taskAggregateIdentifier)).isEmpty()

    // Check that the message - message attachment relation exists to ensure that the
    // state was updated
    assertThat(
            repositories.objectRelationRepository.findOneByLeftAndRightType(
                ObjectIdentifier(messageAttachmentAggregateIdentifier), MESSAGE.name))
        .isNotNull

    confirmVerified(liveUpdateEventProcessor)
  }

  @MethodSource(employees)
  @ParameterizedTest(name = "for {0}")
  fun `verify state is updated but not news and live updates are created for project copy`(
      @Suppress("UNUSED_PARAMETER") role: String,
      employee: () -> EmployeeAggregateAvro
  ) {

    eventStreamGenerator
        .submitProjectCopyStarted()
        .submitTask()
        .submitTaskSchedule()
        .submitTopicG2()
        .submitTopicAttachment()
        .submitMessage()
        .submitMessageAttachment()
        .submitProjectCopyFinished()

    val taskAggregateIdentifier = getByReference("task")
    val messageAttachmentAggregateIdentifier = getByReference("messageAttachment")

    assertThat(controller.getDetails(employee, taskAggregateIdentifier)).isEmpty()

    // Check that the message - message attachment relation exists to ensure that the
    // state was updated
    assertThat(
            repositories.objectRelationRepository.findOneByLeftAndRightType(
                ObjectIdentifier(messageAttachmentAggregateIdentifier), MESSAGE.name))
        .isNotNull

    confirmVerified(liveUpdateEventProcessor)
  }

  @Test
  fun `verify non business transaction code work as expected with state update, news and live updates`() {

    eventStreamGenerator
        .submitTask()
        .submitTaskSchedule()
        .submitTopicG2()
        .submitTopicAttachment()
        .submitMessage()
        .submitMessageAttachment()

    val taskAggregateIdentifier = getByReference("task")
    val messageAttachmentAggregateIdentifier = getByReference("messageAttachment")

    assertThat(controller.getDetails(employeeCsm2, taskAggregateIdentifier)).hasSize(6)

    // Check that the message - message attachment relation exists to ensure that the
    // state was updated
    assertThat(
            repositories.objectRelationRepository.findOneByLeftAndRightType(
                ObjectIdentifier(messageAttachmentAggregateIdentifier), MESSAGE.name))
        .isNotNull

    verify(exactly = 6) { liveUpdateEventProcessor.process(any(), any(), any()) }
  }

  companion object {
    private lateinit var employeeCsm2: EmployeeAggregateAvro
    private lateinit var employeeCr: EmployeeAggregateAvro
    private lateinit var employeeFm: EmployeeAggregateAvro

    private const val employees =
        "com.bosch.pt.csm.cloud.projectmanagement.project.facade.listener." +
            "BusinessTransactionToStateIntegrationTest#employees"

    @JvmStatic
    fun employees() =
        listOf(
            Arguments.of("construction site manager", { employeeCsm2 }),
            Arguments.of("company representative", { employeeCr }),
            Arguments.of("foreman", { employeeFm }))
  }
}
