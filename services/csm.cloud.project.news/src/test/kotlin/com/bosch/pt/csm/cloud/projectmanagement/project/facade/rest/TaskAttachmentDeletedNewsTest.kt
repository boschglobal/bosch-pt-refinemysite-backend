/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.facade.rest

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractNewsTest
import com.bosch.pt.csm.cloud.projectmanagement.common.delete
import com.bosch.pt.csm.cloud.projectmanagement.common.getDetails
import com.bosch.pt.csm.cloud.projectmanagement.common.matchesNewsInAnyOrder
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.TASK
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@SmartSiteSpringBootTest
class TaskAttachmentDeletedNewsTest : AbstractNewsTest() {

  private lateinit var taskAttachmentAggregateIdentifier: AggregateIdentifierAvro

  @BeforeEach
  fun initialize() {
    controller.delete(taskAggregateIdentifier, employeeCsm1, employeeCr1, employeeFm)
    eventStreamGenerator.submitTaskAttachment()
    taskAttachmentAggregateIdentifier = getByReference("taskAttachment")
  }

  @MethodSource(employeesCsmCrFm)
  @ParameterizedTest(name = "for {0}")
  fun `should be gone`(
      @Suppress("UNUSED_PARAMETER") role: String,
      employee: () -> EmployeeAggregateAvro
  ) {

    // Ensure that news are generated as expected
    assertThat(controller.getDetails(employee, taskAggregateIdentifier))
        .matchesNewsInAnyOrder(
            buildNews(taskAggregateIdentifier, taskAggregateIdentifier, taskAggregateIdentifier),
            buildNews(
                taskAttachmentAggregateIdentifier,
                taskAggregateIdentifier,
                taskAggregateIdentifier))

    // Check that object relations exist
    assertThat(
            objectRelationRepository.findOneByLeftAndRightType(
                ObjectIdentifier(taskAttachmentAggregateIdentifier), TASK))
        .isNotNull

    // Process delete event
    eventStreamGenerator.submitTaskAttachment(eventType = TaskAttachmentEventEnumAvro.DELETED)

    // Ensure that all object relations are deleted
    assertThat(
            objectRelationRepository.findOneByLeftAndRightType(
                ObjectIdentifier(taskAttachmentAggregateIdentifier), TASK))
        .isNull()
  }
}
