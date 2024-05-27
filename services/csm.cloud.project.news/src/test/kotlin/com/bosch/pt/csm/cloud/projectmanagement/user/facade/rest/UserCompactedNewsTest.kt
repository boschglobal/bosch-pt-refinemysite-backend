/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractNewsTest
import com.bosch.pt.csm.cloud.projectmanagement.common.getDetails
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.FM
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserCompactedNewsTest : AbstractNewsTest() {

  @BeforeEach
  fun initialize() {
    eventStreamGenerator
        .submitParticipantG3(asReference = "compacted-user-participant") {
          it.user = compactedUserIdentifier
          it.company = getByReference("company")
          it.role = FM
        }
        .submitTask(asReference = "task-for-compacted-user") {
          it.assignee = getByReference("compacted-user-participant")
          it.auditingInformationBuilder.createdBy = compactedUserIdentifier
          it.auditingInformationBuilder.lastModifiedBy = compactedUserIdentifier
        }
        .submitTopicG2(asReference = "topic-for-compacted-user") {
          it.task = getByReference("task-for-compacted-user")
          it.auditingInformationBuilder.createdBy = compactedUserIdentifier
          it.auditingInformationBuilder.lastModifiedBy = compactedUserIdentifier
        }
        .submitParticipantG3(
            asReference = "compacted-user-participant",
            eventType = ParticipantEventEnumAvro.DEACTIVATED) {
          it.user = compactedUserIdentifier
          it.role = FM
        }
  }

  @Test
  fun `no news exist for compacted users`() {
    val employeeCompactedUser = context["employee-for-compacted-user"] as EmployeeAggregateAvro
    assertThat(controller.getDetails(employeeCompactedUser, taskAggregateIdentifier)).isEmpty()
  }
}
