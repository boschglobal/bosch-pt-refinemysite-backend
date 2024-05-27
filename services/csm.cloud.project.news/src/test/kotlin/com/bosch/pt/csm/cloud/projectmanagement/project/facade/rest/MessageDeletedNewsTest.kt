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
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractNewsTest
import com.bosch.pt.csm.cloud.projectmanagement.common.delete
import com.bosch.pt.csm.cloud.projectmanagement.common.getDetails
import com.bosch.pt.csm.cloud.projectmanagement.common.matchesNewsInAnyOrder
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.MESSAGE_ATTACHMENT
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.TOPIC
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class MessageDeletedNewsTest : AbstractNewsTest() {

  private lateinit var topic: AggregateIdentifierAvro
  private lateinit var message: AggregateIdentifierAvro
  private lateinit var messageAttachment: AggregateIdentifierAvro

  @BeforeEach
  fun initialize() {
    controller.delete(taskAggregateIdentifier, employeeCsm1, employeeCr1, employeeFm)

    eventStreamGenerator.submitTopicG2().submitMessage().submitMessageAttachment()

    topic = getByReference("topic")
    message = getByReference("message")
    messageAttachment = getByReference("messageAttachment")
  }

  @MethodSource(employeesCsmCrFm)
  @ParameterizedTest(name = "for {0}")
  fun `the news should remain`(
      @Suppress("UNUSED_PARAMETER") role: String,
      employee: () -> EmployeeAggregateAvro
  ) {
    // Ensure that news are generated as expected
    assertThat(controller.getDetails(employee, taskAggregateIdentifier))
        .matchesNewsInAnyOrder(
            buildNews(taskAggregateIdentifier, taskAggregateIdentifier, taskAggregateIdentifier),
            buildNews(topic, taskAggregateIdentifier, taskAggregateIdentifier),
            buildNews(message, topic, taskAggregateIdentifier),
            buildNews(messageAttachment, message, taskAggregateIdentifier))

    // Process delete event
    eventStreamGenerator.submitMessage(eventType = MessageEventEnumAvro.DELETED)

    // Ensure that news of the task and topic are still there, but news of message and message
    // attachment are gone
    assertThat(controller.getDetails(employee, taskAggregateIdentifier))
        .matchesNewsInAnyOrder(
            buildNews(taskAggregateIdentifier, taskAggregateIdentifier, taskAggregateIdentifier),
            buildNews(topic, taskAggregateIdentifier, taskAggregateIdentifier))
  }

  @Test
  fun `object relations should be gone`() {

    // Check that object relations exist
    val messageObjectIdentifier = ObjectIdentifier(message)
    assertThat(objectRelationRepository.findOneByLeftAndRightType(messageObjectIdentifier, TOPIC))
        .isNotNull

    assertThat(
            objectRelationRepository.findAllByLeftTypeAndRight(
                MESSAGE_ATTACHMENT, messageObjectIdentifier))
        .isNotEmpty

    // Process delete event
    eventStreamGenerator.submitMessage(eventType = MessageEventEnumAvro.DELETED)

    // Ensure that message related relations are deleted
    assertThat(objectRelationRepository.findOneByLeftAndRightType(messageObjectIdentifier, TOPIC))
        .isNull()

    assertThat(
            objectRelationRepository.findAllByLeftTypeAndRight(
                MESSAGE_ATTACHMENT, messageObjectIdentifier))
        .isEmpty()
  }
}
