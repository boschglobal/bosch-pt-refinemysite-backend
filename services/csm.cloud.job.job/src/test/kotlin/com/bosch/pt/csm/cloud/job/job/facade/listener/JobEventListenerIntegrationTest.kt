/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.facade.listener

import com.bosch.pt.csm.cloud.job.application.config.KafkaConsumerConfiguration
import com.bosch.pt.csm.cloud.job.application.config.KafkaProducerAvroConfiguration
import com.bosch.pt.csm.cloud.job.application.config.KafkaTopicInitializationConfiguration
import com.bosch.pt.csm.cloud.job.application.config.LoggerConfiguration
import com.bosch.pt.csm.cloud.job.extensions.KafkaTestExtension
import com.bosch.pt.csm.cloud.job.job.api.JobCompletedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobFailedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobIdentifier
import com.bosch.pt.csm.cloud.job.job.api.JobQueuedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobRejectedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobResultReadEvent
import com.bosch.pt.csm.cloud.job.job.api.JobStartedEvent
import com.bosch.pt.csm.cloud.job.job.api.JsonSerializedObject
import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import com.bosch.pt.csm.cloud.job.job.command.JobEventPublisher
import com.bosch.pt.csm.cloud.job.job.query.JobProjector
import com.ninjasquad.springmockk.MockkBean
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import java.time.LocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.EnableTransactionManagement

@ExtendWith(KafkaTestExtension::class)
@ActiveProfiles("local")
@SpringBootTest(classes = [JobEventListenerIntegrationTestConfiguration::class])
@DirtiesContext
class JobEventListenerIntegrationTest {

  @MockkBean lateinit var jobProjector: JobProjector

  @Autowired lateinit var jobEventPublisher: JobEventPublisher

  val timestamp = LocalDateTime.parse("2022-03-09T11:33:05.613")
  val jobIdentifier = JobIdentifier("1c2c99dc-b275-4e06-a7e6-d4c29822f085")
  val userIdentifier = UserIdentifier("42ad444b-0139-4f25-b10b-5b341a549f8c")

  @BeforeEach
  fun setupMock() {
    every { jobProjector.handle(any()) } just runs
  }

  @Test
  fun `publishes and listens for JobQueuedEvent`() {
    val jobQueuedEvent =
        JobQueuedEvent(
            timestamp,
            jobIdentifier,
            1,
            "CALENDAR_EXPORT_CSV",
            userIdentifier,
            JsonSerializedObject("CalendarExportContext", "{}"),
            JsonSerializedObject("CalendarExportCommand", "{}"))

    jobEventPublisher.publish(jobQueuedEvent)

    verify(timeout = 30_000) { jobProjector.handle(jobQueuedEvent) }
  }

  @Test
  fun `publishes and listens for JobStartedEvent`() {
    val jobStartedEvent = JobStartedEvent(timestamp, jobIdentifier, 2)

    jobEventPublisher.publish(jobStartedEvent)

    verify(timeout = 30_000) { jobProjector.handle(jobStartedEvent) }
  }

  @Test
  fun `publishes and listens for JobCompletedEvent`() {
    val jobCompletedEvent =
        JobCompletedEvent(
            timestamp, jobIdentifier, 3, JsonSerializedObject("CalendarExportResult", "{}"))

    jobEventPublisher.publish(jobCompletedEvent)

    verify(timeout = 30_000) { jobProjector.handle(jobCompletedEvent) }
  }

  @Test
  fun `publishes and listens for JobFailedEvent`() {
    val jobFailedEvent = JobFailedEvent(timestamp, jobIdentifier, 3)

    jobEventPublisher.publish(jobFailedEvent)

    verify(timeout = 30_000) { jobProjector.handle(jobFailedEvent) }
  }

  @Test
  fun `publishes and listens for JobRejectedEvent`() {
    val jobRejectedEvent =
        JobRejectedEvent(
            timestamp,
            jobIdentifier,
            3,
            "CALENDAR_EXPORT_CSV",
            userIdentifier,
            JsonSerializedObject("CalendarExportContext", "{}"))

    jobEventPublisher.publish(jobRejectedEvent)

    verify(timeout = 30_000) { jobProjector.handle(jobRejectedEvent) }
  }

  @Test
  fun `publishes and listens for JobResultReadEvent`() {
    val jobResultReadEvent = JobResultReadEvent(timestamp, jobIdentifier, 3)

    jobEventPublisher.publish(jobResultReadEvent)

    verify(timeout = 30_000) { jobProjector.handle(jobResultReadEvent) }
  }
}

@Import(
    JobEventPublisher::class,
    JobEventListener::class,
    KafkaTopicInitializationConfiguration::class,
    KafkaProducerAvroConfiguration::class,
    KafkaConsumerConfiguration::class,
    SimpleMeterRegistry::class,
    LoggerConfiguration::class)
@ImportAutoConfiguration(KafkaAutoConfiguration::class)
@EnableTransactionManagement
private class JobEventListenerIntegrationTestConfiguration
