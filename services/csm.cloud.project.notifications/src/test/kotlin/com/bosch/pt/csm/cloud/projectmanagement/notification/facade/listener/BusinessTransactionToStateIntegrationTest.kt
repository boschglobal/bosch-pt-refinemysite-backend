/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.util.HttpTestUtils.setFakeUrlWithApiVersion
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCopyFinished
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCopyStarted
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserAndActivate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@SmartSiteSpringBootTest
class BusinessTransactionToStateIntegrationTest : BaseNotificationTest() {

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

    employeeCsm2 = context["csm-employee-2"] as EmployeeAggregateAvro
    employeeCr = context["cr-employee-1"] as EmployeeAggregateAvro
    employeeFm = context["fm-employee"] as EmployeeAggregateAvro
  }

  @MethodSource(employees)
  @ParameterizedTest(name = "for {0}")
  @Suppress("UnusedPrivateMember")
  fun `verify state is updated but no notifications are created for project copy`(
      role: String,
      employee: () -> EmployeeAggregateAvro
  ) {

    // submit project with some content within a copy project business transaction
    eventStreamGenerator
        .submitProjectCopyStarted()
        .submitProject()
        .submitProjectCraftG2()
        .submitParticipantG3(asReference = "csm-participant-1") {
          it.user = getByReference("csm-user-1")
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .submitParticipantG3(asReference = "csm-participant-2") {
          it.user = getByReference("csm-user-2")
          it.role = ParticipantRoleEnumAvro.CSM
        }
        .submitParticipantG3(asReference = "cr-participant-1") {
          it.user = getByReference("cr-user-1")
          it.role = ParticipantRoleEnumAvro.CR
        }
        .submitParticipantG3(asReference = "fm-participant") {
          it.user = getByReference("fm-user")
          it.role = ParticipantRoleEnumAvro.FM
        }
        .submitTask()
        .submitTaskSchedule()
        .submitTopicG2()
        .submitTopicAttachment()
        .submitMessage()
        .submitMessageAttachment()
        .submitProjectCopyFinished()

    // verify project state was applied
    val projectAggregateIdentifier = getByReference("project").toAggregateIdentifier()
    assertThat(repositories.projectRepository.findById(projectAggregateIdentifier)).isNotNull

    // verify task state was applied
    val taskAggregateIdentifier = getByReference("task").toAggregateIdentifier()
    assertThat(
            repositories.taskRepository.find(
                taskAggregateIdentifier.identifier, 0L, projectAggregateIdentifier.identifier))
        .isNotNull

    // verify task schedule state was applied
    val taskScheduleAggregateIdentifier = getByReference("taskSchedule").toAggregateIdentifier()
    assertThat(repositories.taskScheduleRepository.findById(taskScheduleAggregateIdentifier))
        .isNotNull

    // verify topic schedule state was applied
    val topicAggregateIdentifier = getByReference("topic").toAggregateIdentifier()
    assertThat(repositories.topicRepository.findById(topicAggregateIdentifier)).isNotNull

    // verify topic attachment schedule state was applied
    val topicAttachmentAggregateIdentifier =
        getByReference("topicAttachment").toAggregateIdentifier()
    assertThat(repositories.topicAttachmentRepository.findById(topicAttachmentAggregateIdentifier))
        .isNotNull

    // verify message state was applied
    val messageAggregateIdentifier = getByReference("message").toAggregateIdentifier()
    assertThat(repositories.messageRepository.findById(messageAggregateIdentifier)).isNotNull

    // verify message attachment state was applied
    val messageAttachmentAggregateIdentifier =
        getByReference("messageAttachment").toAggregateIdentifier()
    assertThat(
            repositories.messageAttachmentRepository.findById(messageAttachmentAggregateIdentifier))
        .isNotNull

    // verify that no notifications were generated within the project copy business transaction
    assertThat(repositories.notificationRepository.findAll()).isEmpty()
  }

  companion object {
    private lateinit var employeeCsm2: EmployeeAggregateAvro
    private lateinit var employeeCr: EmployeeAggregateAvro
    private lateinit var employeeFm: EmployeeAggregateAvro

    private const val employees =
        "com.bosch.pt.csm.cloud.projectmanagement.notification.facade.listener." +
            "BusinessTransactionToStateIntegrationTest#employees"

    @JvmStatic
    @Suppress("Unused")
    fun employees() =
        listOf(
            Arguments.of("construction site manager", { employeeCsm2 }),
            Arguments.of("company representative", { employeeCr }),
            Arguments.of("foreman", { employeeFm }))
  }
}
