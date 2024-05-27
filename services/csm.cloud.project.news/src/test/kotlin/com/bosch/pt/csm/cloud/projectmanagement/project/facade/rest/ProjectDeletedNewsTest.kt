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
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.COMPANY
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.EMPLOYEE
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.MESSAGE
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.MESSAGE_ATTACHMENT
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.TASK_ATTACHMENT
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.TASK_SCHEDULE
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.TOPIC
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.TOPIC_ATTACHMENT
import com.bosch.pt.csm.cloud.projectmanagement.common.model.AggregateType.USER
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectRelation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskSchedule
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ProjectDeletedNewsTest : AbstractNewsTest() {

  private lateinit var taskSchedule: AggregateIdentifierAvro
  private lateinit var taskAttachment: AggregateIdentifierAvro
  private lateinit var topic: AggregateIdentifierAvro
  private lateinit var topicAttachment: AggregateIdentifierAvro
  private lateinit var message: AggregateIdentifierAvro
  private lateinit var messageAttachment: AggregateIdentifierAvro

  @BeforeEach
  fun initialize() {
    controller.delete(taskAggregateIdentifier, employeeCsm1, employeeCr1, employeeFm)
    eventStreamGenerator
        .submitTaskSchedule()
        .submitTaskAttachment()
        .submitTopicG2()
        .submitTopicAttachment()
        .submitMessage()
        .submitMessageAttachment()

    taskSchedule = getByReference("taskSchedule")
    taskAttachment = getByReference("taskAttachment")
    topic = getByReference("topic")
    topicAttachment = getByReference("topicAttachment")
    message = getByReference("message")
    messageAttachment = getByReference("messageAttachment")
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
            buildNews(taskSchedule, taskAggregateIdentifier, taskAggregateIdentifier),
            buildNews(taskAttachment, taskAggregateIdentifier, taskAggregateIdentifier),
            buildNews(topic, taskAggregateIdentifier, taskAggregateIdentifier),
            buildNews(topicAttachment, topic, taskAggregateIdentifier),
            buildNews(message, topic, taskAggregateIdentifier),
            buildNews(messageAttachment, message, taskAggregateIdentifier))

    // Check that object relations exist
    val taskObjectIdentifier = ObjectIdentifier(taskAggregateIdentifier)
    val topicRelations =
        objectRelationRepository.findAllByLeftTypeAndRight(TOPIC, taskObjectIdentifier)
    assertThat(topicRelations).isNotEmpty
    assertThat(
            objectRelationRepository.findAllByLeftTypeAndRightIn(
                TOPIC_ATTACHMENT, getIds(topicRelations)))
        .isNotEmpty
    val messageRelations =
        objectRelationRepository.findAllByLeftTypeAndRightIn(MESSAGE, getIds(topicRelations))
    assertThat(messageRelations).isNotEmpty
    assertThat(
            objectRelationRepository.findAllByLeftTypeAndRightIn(
                MESSAGE_ATTACHMENT, getIds(messageRelations)))
        .isNotEmpty
    assertThat(
            objectRelationRepository.findAllByLeftTypeAndRight(
                TASK_ATTACHMENT, taskObjectIdentifier))
        .isNotEmpty
    assertThat(
            objectRelationRepository.findAllByLeftTypeAndRight(TASK_SCHEDULE, taskObjectIdentifier))
        .isNotEmpty
    assertThat(objectRelationRepository.findOneByLeftAndRightType(taskObjectIdentifier, USER))
        .isNotNull
    assertThat(objectRelationRepository.findOneByLeftAndRightType(taskObjectIdentifier, COMPANY))
        .isNotNull
    assertThat(objectRelationRepository.findOneByLeftAndRightType(taskObjectIdentifier, PROJECT))
        .isNotNull
    assertThat(
            objectRelationRepository.findAllByLeftTypeAndRight(
                PARTICIPANT, ObjectIdentifier(projectAggregateIdentifier)))
        .hasSize(7)
    assertThat(
            objectRelationRepository.findAllByLeftInAndRightType(
                listOf(
                    ObjectIdentifier(participantCr1.getAggregateIdentifier()),
                    ObjectIdentifier(participantCr2.getAggregateIdentifier()),
                    ObjectIdentifier(participantCsm1.getAggregateIdentifier()),
                    ObjectIdentifier(participantCsm2.getAggregateIdentifier()),
                    ObjectIdentifier(participantFm.getAggregateIdentifier())),
                USER))
        .hasSize(5)
    assertThat(
            objectRelationRepository.findAllByLeftInAndRightType(
                listOf(
                    ObjectIdentifier(participantCr1.getAggregateIdentifier()),
                    ObjectIdentifier(participantCr2.getAggregateIdentifier()),
                    ObjectIdentifier(participantCsm1.getAggregateIdentifier()),
                    ObjectIdentifier(participantCsm2.getAggregateIdentifier()),
                    ObjectIdentifier(participantFm.getAggregateIdentifier())),
                COMPANY))
        .hasSize(5)

    // Process delete event
    eventStreamGenerator.submitProject(eventType = ProjectEventEnumAvro.DELETED)

    // Ensure that all news are deleted
    assertThat(controller.getDetails(employee, taskAggregateIdentifier)).isEmpty()

    // Ensure that all object relations are deleted
    assertThat(objectRelationRepository.findAllByLeftTypeAndRight(TOPIC, taskObjectIdentifier))
        .isEmpty()
    assertThat(
            objectRelationRepository.findAllByLeftTypeAndRightIn(
                TOPIC_ATTACHMENT, getIds(topicRelations)))
        .isEmpty()
    assertThat(
            objectRelationRepository.findAllByLeftTypeAndRightIn(MESSAGE, getIds(topicRelations)))
        .isEmpty()
    assertThat(
            objectRelationRepository.findAllByLeftTypeAndRightIn(
                MESSAGE_ATTACHMENT, getIds(messageRelations)))
        .isEmpty()
    assertThat(
            objectRelationRepository.findAllByLeftTypeAndRight(
                TASK_ATTACHMENT, taskObjectIdentifier))
        .isEmpty()
    assertThat(
            objectRelationRepository.findAllByLeftTypeAndRight(TASK_SCHEDULE, taskObjectIdentifier))
        .isEmpty()
    assertThat(objectRelationRepository.findOneByLeftAndRightType(taskObjectIdentifier, USER))
        .isNull()
    assertThat(objectRelationRepository.findOneByLeftAndRightType(taskObjectIdentifier, COMPANY))
        .isNull()
    assertThat(objectRelationRepository.findOneByLeftAndRightType(taskObjectIdentifier, PROJECT))
        .isNull()
    assertThat(
            objectRelationRepository.findAllByLeftTypeAndRight(
                PARTICIPANT, ObjectIdentifier(projectAggregateIdentifier)))
        .isEmpty()
    assertThat(
            objectRelationRepository.findAllByLeftInAndRightType(
                listOf(
                    ObjectIdentifier(participantCr1.getAggregateIdentifier()),
                    ObjectIdentifier(participantCsm1.getAggregateIdentifier()),
                    ObjectIdentifier(participantFm.getAggregateIdentifier())),
                EMPLOYEE))
        .isEmpty()
  }

  private fun getIds(relations: List<ObjectRelation>): List<ObjectIdentifier> =
      relations.map { checkNotNull(it.left) }
}
