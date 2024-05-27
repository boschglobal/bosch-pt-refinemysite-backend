/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.query

import com.bosch.pt.csm.cloud.job.application.config.LoggerConfiguration
import com.bosch.pt.csm.cloud.job.event.integration.EventService
import com.bosch.pt.csm.cloud.job.job.api.JobCompletedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobEvent
import com.bosch.pt.csm.cloud.job.job.api.JobFailedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobIdentifier
import com.bosch.pt.csm.cloud.job.job.api.JobQueuedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobRejectedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobResultReadEvent
import com.bosch.pt.csm.cloud.job.job.api.JobStartedEvent
import com.bosch.pt.csm.cloud.job.job.api.JsonSerializedObject
import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import com.bosch.pt.csm.cloud.job.job.command.snapshot.JobState
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatNoException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@DataMongoTest
@Import(JobProjectionIntegrationTestConfiguration::class)
class JobProjectionIntegrationTest {

  @MockkBean lateinit var eventService: EventService

  @Autowired lateinit var jobProjectionRepository: JobProjectionRepository

  @Autowired lateinit var jobProjector: JobProjector

  val jobIdentifier = randomJobIdentifier()
  val userIdentifier = randomUserIdentifier()
  val now = LocalDateTime.parse("2022-03-16T14:18:32.214")

  @Nested
  inner class `Given a REJECTED Job` {
    val events =
        listOf(
            JobRejectedEvent(
                now,
                jobIdentifier,
                1L,
                "EXPORT_CALENDAR_PDF",
                userIdentifier,
                JsonSerializedObject(
                    "CalendarExportContext", """{"contextField":"contextValue"}""")))

    @Test
    fun `updates projection`() {
      given(events)

      assertThat(currentJobProjection())
          .isEqualTo(
              JobProjection(
                  jobIdentifier,
                  userIdentifier,
                  "EXPORT_CALENDAR_PDF",
                  JobState.REJECTED,
                  JsonSerializedObject(
                      "CalendarExportContext", """{"contextField":"contextValue"}"""),
                  now,
                  now))
    }

    @Test
    fun `pushes update to User via Event service`() {
      given(events)

      assertThat(lastPushedJobUpdate.captured)
          .isEqualTo(
              JobResource(
                  id = jobIdentifier.value,
                  type = "EXPORT_CALENDAR_PDF",
                  status = "REJECTED",
                  createdDate = Instant.parse("2022-03-16T14:18:32.214Z"),
                  lastModifiedDate = Instant.parse("2022-03-16T14:18:32.214Z"),
                  context = """{"contextField":"contextValue"}""",
                  result = null,
                  read = false))
    }
  }

  @Nested
  inner class `Given a QUEUED Job` {

    val events = listOf(aJobQueuedEvent(jobIdentifier, userIdentifier))

    @Test
    fun `updates projection`() {
      given(events)

      assertThat(currentJobProjection())
          .isEqualTo(
              JobProjection(
                  jobIdentifier,
                  userIdentifier,
                  "EXPORT_CALENDAR_PDF",
                  JobState.QUEUED,
                  JsonSerializedObject(
                      "CalendarExportContext", """{"contextField":"contextValue"}"""),
                  now,
                  now))
    }

    @Test
    fun `pushes update to User via Event service`() {
      given(events)

      assertThat(lastPushedJobUpdate.captured)
          .isEqualTo(
              JobResource(
                  id = jobIdentifier.value,
                  type = "EXPORT_CALENDAR_PDF",
                  status = "QUEUED",
                  createdDate = now.toInstant(UTC),
                  lastModifiedDate = now.toInstant(UTC),
                  context = """{"contextField":"contextValue"}""",
                  result = null,
                  read = false))
    }
  }

  @Nested
  inner class `Given a STARTED Job` {

    val events =
        listOf(
            aJobQueuedEvent(
                jobIdentifier,
                userIdentifier,
            ),
            JobStartedEvent(now, jobIdentifier, 2L))

    @Test
    fun `updates projection`() {
      given(events)

      assertThat(currentJobProjection())
          .isEqualTo(
              JobProjection(
                  jobIdentifier,
                  userIdentifier,
                  "EXPORT_CALENDAR_PDF",
                  JobState.RUNNING,
                  JsonSerializedObject(
                      "CalendarExportContext", """{"contextField":"contextValue"}"""),
                  now,
                  now))
    }

    @Test
    fun `pushes update to User via Event service`() {
      given(events)

      assertThat(lastPushedJobUpdate.captured)
          .isEqualTo(
              JobResource(
                  id = jobIdentifier.value,
                  type = "EXPORT_CALENDAR_PDF",
                  status = "RUNNING",
                  createdDate = now.toInstant(UTC),
                  lastModifiedDate = now.toInstant(UTC),
                  context = """{"contextField":"contextValue"}""",
                  result = null,
                  read = false))
    }
  }

  @Nested
  inner class `Given a COMPLETED Job` {

    val events =
        listOf(
            aJobQueuedEvent(
                jobIdentifier,
                userIdentifier,
            ),
            JobStartedEvent(now, jobIdentifier, 2L),
            JobCompletedEvent(
                now,
                jobIdentifier,
                3L,
                JsonSerializedObject("CalendarExportResult", """{"resultField":"resultValue"}""")))

    @Test
    fun `updates projection`() {
      given(events)

      val expectedJobProjection =
          JobProjection(
              jobIdentifier,
              userIdentifier,
              "EXPORT_CALENDAR_PDF",
              JobState.COMPLETED,
              JsonSerializedObject("CalendarExportContext", """{"contextField":"contextValue"}"""),
              now,
              now,
              JsonSerializedObject("CalendarExportResult", """{"resultField":"resultValue"}"""))
      assertThat(jobProjectionRepository.findById(expectedJobProjection.jobIdentifier).get())
          .isEqualTo(expectedJobProjection)
    }

    @Test
    fun `pushes update to User via Event service`() {
      given(events)

      assertThat(lastPushedJobUpdate.captured)
          .isEqualTo(
              JobResource(
                  id = jobIdentifier.value,
                  type = "EXPORT_CALENDAR_PDF",
                  status = "COMPLETED",
                  createdDate = now.toInstant(UTC),
                  lastModifiedDate = now.toInstant(UTC),
                  context = """{"contextField":"contextValue"}""",
                  result = """{"resultField":"resultValue"}""",
                  read = false))
    }
  }

  @Nested
  inner class `Given a FAILED Job` {

    val events =
        listOf(
            aJobQueuedEvent(jobIdentifier, userIdentifier),
            JobStartedEvent(now, jobIdentifier, 2L),
            JobFailedEvent(now, jobIdentifier, 3L))

    @Test
    fun `updates projection`() {
      given(events)

      assertThat(currentJobProjection())
          .isEqualTo(
              JobProjection(
                  jobIdentifier,
                  userIdentifier,
                  "EXPORT_CALENDAR_PDF",
                  JobState.FAILED,
                  JsonSerializedObject(
                      "CalendarExportContext", """{"contextField":"contextValue"}"""),
                  now,
                  now))
    }

    @Test
    fun `pushes update to User via Event service`() {
      given(events)

      assertThat(lastPushedJobUpdate.captured)
          .isEqualTo(
              JobResource(
                  id = jobIdentifier.value,
                  type = "EXPORT_CALENDAR_PDF",
                  status = "FAILED",
                  createdDate = now.toInstant(UTC),
                  lastModifiedDate = now.toInstant(UTC),
                  context = """{"contextField":"contextValue"}""",
                  result = null,
                  read = false))
    }
  }

  @Nested
  inner class `Given a Job with a result marked as read` {

    val anHourEarlier = now.minusHours(1)
    val events =
        listOf(
            aJobQueuedEvent(jobIdentifier, userIdentifier, anHourEarlier),
            JobStartedEvent(anHourEarlier, jobIdentifier, 2L),
            JobCompletedEvent(
                anHourEarlier,
                jobIdentifier,
                3L,
                JsonSerializedObject("CalendarExportResult", """{"resultField":"resultValue"}""")),
            JobResultReadEvent(now, jobIdentifier, 4L))

    @Test
    fun `updates projection`() {
      given(events)

      assertThat(currentJobProjection())
          .isEqualTo(
              JobProjection(
                  jobIdentifier,
                  userIdentifier,
                  "EXPORT_CALENDAR_PDF",
                  JobState.COMPLETED,
                  JsonSerializedObject(
                      "CalendarExportContext", """{"contextField":"contextValue"}"""),
                  anHourEarlier,
                  anHourEarlier,
                  JsonSerializedObject("CalendarExportResult", """{"resultField":"resultValue"}"""),
                  true))
    }

    @Test
    fun `pushes update to User via Event service`() {
      given(events)

      assertThat(lastPushedJobUpdate.captured)
          .isEqualTo(
              JobResource(
                  id = jobIdentifier.value,
                  type = "EXPORT_CALENDAR_PDF",
                  status = "COMPLETED",
                  createdDate = anHourEarlier.toInstant(UTC),
                  lastModifiedDate = anHourEarlier.toInstant(UTC),
                  context = """{"contextField":"contextValue"}""",
                  result = """{"resultField":"resultValue"}""",
                  read = true))
    }

    @Test
    fun `does not update field lastModifiedAt on JobResultReadEvent`() {
      given(events)

      assertThat(currentJobProjection().lastModifiedDate).isEqualTo(anHourEarlier)
    }
  }

  @Nested
  inner class `Given an unknown Job` {

    @Test
    fun `ignores JobStartedEvent for unknown Job`() {
      val events = listOf(JobStartedEvent(now, jobIdentifier, 2L))

      assertThatNoException().isThrownBy { given(events) }
    }

    @Test
    fun `ignores JobCompletedEvent for unknown Job`() {
      val events =
          listOf(
              JobCompletedEvent(
                  now,
                  jobIdentifier,
                  3L,
                  JsonSerializedObject(
                      "CalendarExportResult", """{"resultField":"resultValue"}""")))

      assertThatNoException().isThrownBy { given(events) }
    }

    @Test
    fun `ignores JobFailedEvent for unknown Job`() {
      val events = listOf(JobFailedEvent(now, jobIdentifier, 3L))

      assertThatNoException().isThrownBy { given(events) }
    }

    @Test
    fun `ignores JobResultReadEvent for unknown Job`() {
      val events = listOf(JobResultReadEvent(now, jobIdentifier, 4L))

      assertThatNoException().isThrownBy { given(events) }
    }
  }

  private fun given(events: List<JobEvent>) {
    events.forEach { jobProjector.handle(it) }
  }

  private fun currentJobProjection() = jobProjectionRepository.findById(jobIdentifier).get()

  val lastPushedJobUpdate = slot<JobResource>()

  @BeforeEach
  fun setupMocks() {
    every { eventService.send(userIdentifier, capture(lastPushedJobUpdate)) } just runs
  }

  private fun aJobQueuedEvent(
      jobIdentifier: JobIdentifier,
      userIdentifier: UserIdentifier,
      timestamp: LocalDateTime = now
  ) =
      JobQueuedEvent(
          timestamp,
          jobIdentifier,
          1L,
          "EXPORT_CALENDAR_PDF",
          userIdentifier,
          JsonSerializedObject("CalendarExportContext", """{"contextField":"contextValue"}"""),
          JsonSerializedObject("CalendarExportCommand", """{"commandField":"commandValue"}"""))

  private fun randomJobIdentifier() = JobIdentifier(UUID.randomUUID().toString())

  private fun randomUserIdentifier() = UserIdentifier(UUID.randomUUID().toString())
}

@Import(JobProjector::class, LoggerConfiguration::class)
private class JobProjectionIntegrationTestConfiguration
