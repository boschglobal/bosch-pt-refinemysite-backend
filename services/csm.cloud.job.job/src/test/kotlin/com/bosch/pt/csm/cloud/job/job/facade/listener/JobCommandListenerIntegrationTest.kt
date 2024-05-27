/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.facade.listener

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.CommandMessageKeyAvro
import com.bosch.pt.csm.cloud.job.application.config.KafkaConsumerConfiguration
import com.bosch.pt.csm.cloud.job.application.config.KafkaProducerAvroConfiguration
import com.bosch.pt.csm.cloud.job.application.config.KafkaTopicInitializationConfiguration
import com.bosch.pt.csm.cloud.job.application.config.LoggerConfiguration
import com.bosch.pt.csm.cloud.job.common.JobAggregateTypeEnum.JOB
import com.bosch.pt.csm.cloud.job.extensions.KafkaTestExtension
import com.bosch.pt.csm.cloud.job.job.api.CompleteJobCommand
import com.bosch.pt.csm.cloud.job.job.api.EnqueueJobCommand
import com.bosch.pt.csm.cloud.job.job.api.FailJobCommand
import com.bosch.pt.csm.cloud.job.job.api.JobIdentifier
import com.bosch.pt.csm.cloud.job.job.api.JsonSerializedObject
import com.bosch.pt.csm.cloud.job.job.api.StartJobCommand
import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import com.bosch.pt.csm.cloud.job.job.command.handler.JobCommandDispatcher
import com.bosch.pt.csm.cloud.job.messages.CompleteJobCommandAvro
import com.bosch.pt.csm.cloud.job.messages.EnqueueJobCommandAvro
import com.bosch.pt.csm.cloud.job.messages.FailJobCommandAvro
import com.bosch.pt.csm.cloud.job.messages.JsonSerializedObjectAvro
import com.bosch.pt.csm.cloud.job.messages.StartJobCommandAvro
import com.ninjasquad.springmockk.MockkBean
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.apache.avro.specific.SpecificRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

@ExtendWith(KafkaTestExtension::class)
@ActiveProfiles("local")
@SpringBootTest(classes = [JobCommandListenerIntegrationTestConfiguration::class])
@DirtiesContext
class JobCommandListenerIntegrationTest {

  @MockkBean lateinit var jobCommandDispatcher: JobCommandDispatcher

  @Value("\${custom.kafka.bindings.job-command.kafkaTopic}") private lateinit var topic: String

  @Autowired
  @Qualifier("avro")
  lateinit var kafkaTemplate: KafkaTemplate<SpecificRecord, SpecificRecord>

  @BeforeEach
  fun setupMock() {
    every { jobCommandDispatcher.dispatch(any()) } just runs
  }

  @Test
  fun `listens for EnqueueJobCommand`() {
    val enqueueJobCommand =
        EnqueueJobCommand(
            "CALENDAR_EXPORT_CSV",
            JobIdentifier("b2d67a00-9d9e-4829-bf60-b19e9784b52c"),
            UserIdentifier("d631cec4-84a2-47a5-ae5c-1eb32386b9fb"),
            JsonSerializedObject("CalendarExportContext", "{}"),
            JsonSerializedObject("CalendarExportCommand", "{}"))

    send(enqueueJobCommand)

    verify(timeout = 30_000) { jobCommandDispatcher.dispatch(enqueueJobCommand) }
  }

  @Test
  fun `listens for StartJobCommand`() {
    val startJobCommand = StartJobCommand(JobIdentifier("b2d67a00-9d9e-4829-bf60-b19e9784b52c"))

    send(startJobCommand)

    verify(timeout = 30_000) { jobCommandDispatcher.dispatch(startJobCommand) }
  }

  @Test
  fun `listens for CompleteJobCommand`() {
    val completeJobCommand =
        CompleteJobCommand(
            JobIdentifier("b2d67a00-9d9e-4829-bf60-b19e9784b52c"),
            JsonSerializedObject("CalendarExportResult", "{}"))

    send(completeJobCommand)

    verify(timeout = 30_000) { jobCommandDispatcher.dispatch(completeJobCommand) }
  }

  @Test
  fun `listens for FailJobCommand`() {
    val failJobCommand = FailJobCommand(JobIdentifier("b2d67a00-9d9e-4829-bf60-b19e9784b52c"))

    send(failJobCommand)

    verify(timeout = 30_000) { jobCommandDispatcher.dispatch(failJobCommand) }
  }

  private fun send(enqueueJobCommand: EnqueueJobCommand) {
    kafkaTemplate.executeInTransaction {
      kafkaTemplate.send(
          topic,
          commandMessageKeyAvro(enqueueJobCommand.jobIdentifier),
          EnqueueJobCommandAvro.newBuilder()
              .apply {
                aggregateIdentifierBuilder =
                    aggregateIdentifierBuilder(enqueueJobCommand.jobIdentifier)
                jobType = enqueueJobCommand.jobType
                userIdentifier = enqueueJobCommand.userIdentifier.value
                jsonSerializedContextBuilder =
                    JsonSerializedObjectAvro.newBuilder().apply {
                      type = enqueueJobCommand.serializedContext.type
                      json = enqueueJobCommand.serializedContext.json
                    }
                jsonSerializedCommandBuilder =
                    JsonSerializedObjectAvro.newBuilder().apply {
                      type = enqueueJobCommand.serializedCommand.type
                      json = enqueueJobCommand.serializedCommand.json
                    }
              }
              .build())
    }
  }

  private fun send(startJobCommand: StartJobCommand) {
    kafkaTemplate.executeInTransaction {
      it.send(
          topic,
          commandMessageKeyAvro(startJobCommand.jobIdentifier),
          StartJobCommandAvro.newBuilder()
              .apply {
                aggregateIdentifierBuilder =
                    aggregateIdentifierBuilder(startJobCommand.jobIdentifier)
              }
              .build())
    }
  }

  private fun send(completeJobCommand: CompleteJobCommand) {
    kafkaTemplate.executeInTransaction {
      it.send(
          topic,
          commandMessageKeyAvro(completeJobCommand.jobIdentifier),
          CompleteJobCommandAvro.newBuilder()
              .apply {
                aggregateIdentifierBuilder =
                    aggregateIdentifierBuilder(completeJobCommand.jobIdentifier)
                serializedResultBuilder =
                    JsonSerializedObjectAvro.newBuilder().apply {
                      type = completeJobCommand.serializedResult.type
                      json = completeJobCommand.serializedResult.json
                    }
              }
              .build())
    }
  }

  private fun send(failJobCommand: FailJobCommand) {
    kafkaTemplate.executeInTransaction {
      it.send(
          topic,
          commandMessageKeyAvro(failJobCommand.jobIdentifier),
          FailJobCommandAvro.newBuilder()
              .apply {
                aggregateIdentifierBuilder =
                    aggregateIdentifierBuilder(failJobCommand.jobIdentifier)
              }
              .build())
    }
  }

  private fun commandMessageKeyAvro(jobIdentifier: JobIdentifier) =
      CommandMessageKeyAvro.newBuilder()
          .apply { partitioningIdentifier = jobIdentifier.value }
          .build()

  private fun aggregateIdentifierBuilder(jobIdentifier: JobIdentifier) =
      AggregateIdentifierAvro.newBuilder().apply {
        type = JOB.name
        identifier = jobIdentifier.value
        version = 0
      }
}

@Import(
    JobCommandListener::class,
    KafkaTopicInitializationConfiguration::class,
    KafkaProducerAvroConfiguration::class,
    KafkaConsumerConfiguration::class,
    SimpleMeterRegistry::class,
    LoggerConfiguration::class)
@ImportAutoConfiguration(KafkaAutoConfiguration::class)
private class JobCommandListenerIntegrationTestConfiguration
