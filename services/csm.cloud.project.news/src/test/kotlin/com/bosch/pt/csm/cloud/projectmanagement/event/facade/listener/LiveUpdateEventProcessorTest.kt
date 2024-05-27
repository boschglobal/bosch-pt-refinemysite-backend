/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.event.facade.listener

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.common.model.key.EventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.application.config.ProcessStateOnlyProperties
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASKSCHEDULE
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.listener.AbstractEventProcessor
import com.bosch.pt.csm.cloud.projectmanagement.event.model.Event
import com.bosch.pt.csm.cloud.projectmanagement.event.model.LiveUpdateEvent
import com.bosch.pt.csm.cloud.projectmanagement.event.model.ObjectIdentifierWithVersion
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.project.model.ParticipantMapping
import com.bosch.pt.csm.cloud.projectmanagement.project.repository.ParticipantMappingRepository
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskScheduleEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USERPICTURE
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserPictureEventEnumAvro
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID.randomUUID
import java.util.concurrent.CompletableFuture
import org.apache.avro.specific.SpecificRecordBase
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult

@SmartSiteMockKTest
class LiveUpdateEventProcessorTest {

  @RelaxedMockK private lateinit var participantMappingRepository: ParticipantMappingRepository

  @RelaxedMockK private lateinit var kafkaTemplate: KafkaTemplate<String, Event>

  @RelaxedMockK private lateinit var processStateOnlyProperties: ProcessStateOnlyProperties

  private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

  private lateinit var liveUpdateEventProcessor: LiveUpdateEventProcessor

  @BeforeEach
  fun init() {
    liveUpdateEventProcessor =
        LiveUpdateEventProcessor(participantMappingRepository, kafkaTemplate, objectMapper, "topic")
            .apply {
              AbstractEventProcessor::class
                  .java
                  .getDeclaredField("processStateOnlyProperties")
                  .apply { isAccessible = true }
                  .set(this, processStateOnlyProperties)
            }
  }

  @Test
  fun `process state only for project event`() {
    // Mock
    every { processStateOnlyProperties.isEnabled } returns true
    every { processStateOnlyProperties.untilDate } returns LocalDate.now().plusDays(1).toString()

    // Prepare parameters
    val key = mockk<EventMessageKey>(relaxed = true)
    val value = mockk<SpecificRecordBase>(relaxed = true)

    // Invoke test method
    liveUpdateEventProcessor.process(key, value, LocalDateTime.now())

    // Check that no mock is invoked
    verify { processStateOnlyProperties.isEnabled }
    verify { processStateOnlyProperties.untilDate }
    confirmVerified(processStateOnlyProperties, participantMappingRepository, kafkaTemplate)
  }

  @Test
  fun `process state only for user event`() {
    // Mock
    every { processStateOnlyProperties.isEnabled } returns true
    every { processStateOnlyProperties.untilDate } returns LocalDate.now().plusDays(1).toString()

    // Prepare parameters
    val key = mockk<EventMessageKey>(relaxed = true)
    val value = mockk<SpecificRecordBase>(relaxed = true)

    // Invoke test method
    liveUpdateEventProcessor.processUserEvents(key, value, LocalDateTime.now())

    // Check that no mock is invoked
    verify { processStateOnlyProperties.isEnabled }
    verify { processStateOnlyProperties.untilDate }
    confirmVerified(processStateOnlyProperties, participantMappingRepository, kafkaTemplate)
  }

  @Suppress("UNCHECKED_CAST")
  @Test
  fun `process project event`() {
    val projectId = randomUUID()
    val companyId = randomUUID()
    val userId = randomUUID()
    val taskScheduleId = randomUUID()

    // Mock
    every { processStateOnlyProperties.isEnabled } returns false

    every { participantMappingRepository.findAllByProjectIdentifier(any()) } returns
        listOf(ParticipantMapping(projectId, companyId, CSM.name, userId))

    every { kafkaTemplate.send(any() as ProducerRecord<String, Event>) } answers
        {
          val record = it.invocation.args[0] as ProducerRecord<String, Event>

          // Check key
          assertThat(record.key()).isEqualTo(projectId.toString())

          // Check value
          val value = record.value()
          assertThat(value.eventType).isEqualTo("update")
          assertThat(value.receivers).isEqualTo(setOf(userId))

          val event = objectMapper.readValue(value.message, LiveUpdateEvent::class.java)
          assertThat(event.root).isEqualTo(ObjectIdentifier(PROJECT.value, projectId))
          assertThat(event.objectIdentifier)
              .isEqualTo(ObjectIdentifierWithVersion(TASKSCHEDULE.value, taskScheduleId, 0L))
          assertThat(event.event).isEqualTo(TaskScheduleEventEnumAvro.UPDATED.name)

          mockk<CompletableFuture<SendResult<String, Event>>>(relaxed = true).also {
            every { it.get() } returns mockk<SendResult<String, Event>>()
          }
        }

    val value = mockk<SpecificRecordBase>(relaxed = true)
    every { value.get(any() as String) } returns TaskScheduleEventEnumAvro.UPDATED.name

    val key =
        AggregateEventMessageKey(
            AggregateIdentifier(TASKSCHEDULE.name, taskScheduleId, 0L), projectId)

    // Invoke
    liveUpdateEventProcessor.process(key, value, LocalDateTime.now())

    // Check invocations on mocks
    verify { processStateOnlyProperties.isEnabled }
    verify { participantMappingRepository.findAllByProjectIdentifier(any()) }
    verify { kafkaTemplate.send(any() as ProducerRecord<String, Event>) }
    confirmVerified(processStateOnlyProperties, participantMappingRepository, kafkaTemplate)
  }

  @Suppress("UNCHECKED_CAST")
  @Test
  fun `process user event`() {
    val userId = randomUUID()
    val profilePictureId = randomUUID()

    // Mock
    every { processStateOnlyProperties.isEnabled } returns false

    every { kafkaTemplate.send(any() as ProducerRecord<String, Event>) } answers
        {
          val record = it.invocation.args[0] as ProducerRecord<String, Event>

          // Check key
          assertThat(record.key()).isEqualTo(userId.toString())

          // Check value
          val value = record.value()
          assertThat(value.eventType).isEqualTo("update")
          assertThat(value.receivers).isEqualTo(setOf(userId))

          val event = objectMapper.readValue(value.message, LiveUpdateEvent::class.java)
          assertThat(event.root).isEqualTo(ObjectIdentifier(USER.value, userId))
          assertThat(event.objectIdentifier)
              .isEqualTo(ObjectIdentifierWithVersion(USERPICTURE.value, profilePictureId, 0L))
          assertThat(event.event).isEqualTo(UserPictureEventEnumAvro.UPDATED.name)

          mockk<CompletableFuture<SendResult<String, Event>>>(relaxed = true).also {
            every { it.get() } returns mockk<SendResult<String, Event>>()
          }
        }

    val value = mockk<SpecificRecordBase>(relaxed = true)
    every { value.get(any() as String) } returns UserPictureEventEnumAvro.UPDATED.name

    val key =
        AggregateEventMessageKey(
            AggregateIdentifier(USERPICTURE.name, profilePictureId, 0L), userId)

    // Invoke
    liveUpdateEventProcessor.processUserEvents(key, value, LocalDateTime.now())

    // Check invocations on mocks
    verify { processStateOnlyProperties.isEnabled }
    verify { kafkaTemplate.send(any() as ProducerRecord<String, Event>) }
    confirmVerified(processStateOnlyProperties, participantMappingRepository, kafkaTemplate)
  }
}
