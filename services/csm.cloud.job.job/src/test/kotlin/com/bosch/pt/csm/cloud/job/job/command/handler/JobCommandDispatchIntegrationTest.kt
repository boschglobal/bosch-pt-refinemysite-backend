/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.command.handler

import com.bosch.pt.csm.cloud.job.application.config.LoggerConfiguration
import com.bosch.pt.csm.cloud.job.application.config.MongoConfiguration
import com.bosch.pt.csm.cloud.job.application.config.MongoIndexConfiguration
import com.bosch.pt.csm.cloud.job.job.api.CompleteJobCommand
import com.bosch.pt.csm.cloud.job.job.api.EnqueueJobCommand
import com.bosch.pt.csm.cloud.job.job.api.FailJobCommand
import com.bosch.pt.csm.cloud.job.job.api.JobCommand
import com.bosch.pt.csm.cloud.job.job.api.JobCompletedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobEvent
import com.bosch.pt.csm.cloud.job.job.api.JobFailedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobIdentifier
import com.bosch.pt.csm.cloud.job.job.api.JobQueuedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobRejectedEvent
import com.bosch.pt.csm.cloud.job.job.api.JobResultReadEvent
import com.bosch.pt.csm.cloud.job.job.api.JobStartedEvent
import com.bosch.pt.csm.cloud.job.job.api.JsonSerializedObject
import com.bosch.pt.csm.cloud.job.job.api.MarkJobResultReadCommand
import com.bosch.pt.csm.cloud.job.job.api.StartJobCommand
import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import com.bosch.pt.csm.cloud.job.job.command.JobEventPublisher
import com.bosch.pt.csm.cloud.job.job.command.handler.exception.OnlyCompletedJobsCanBeMarkedAsReadException
import com.bosch.pt.csm.cloud.job.job.command.snapshot.JobSnapshotStore
import com.bosch.pt.csm.cloud.job.user.query.security.JobServiceUserDetails
import com.ninjasquad.springmockk.MockkBean
import io.mockk.called
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifySequence
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatNoException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Import
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.context.TestSecurityContextHolder.setAuthentication
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@DataMongoTest
@Import(JobCommandDispatchIntegrationTestConfiguration::class)
class JobCommandDispatchIntegrationTest {

  @MockkBean lateinit var clock: Clock
  @MockkBean lateinit var jobEventPublisher: JobEventPublisher

  @Autowired lateinit var jobSnapshotStore: JobSnapshotStore

  @Autowired lateinit var jobCommandDispatcher: JobCommandDispatcher

  val now: LocalDateTime = LocalDateTime.parse("2022-03-16T09:14:11.419")

  @Nested
  inner class `On EnqueueJobCommand` {

    val enqueueJobCommand =
        EnqueueJobCommand(
            "EXPORT_CALENDAR_PDF",
            JobIdentifier("229d6224-f332-4b76-b38e-830d7ea4737a"),
            UserIdentifier("253b90dd-9feb-bb07-35cb-7c55a2c37d58"),
            JsonSerializedObject("ExportCalendarContext", "{}"),
            JsonSerializedObject("ExportCalendarCommand", "{}"))

    @Test
    fun `publishes JobQueuedEvent`() {
      whenDelivering(enqueueJobCommand)

      thenExpect(
          JobQueuedEvent(
              now,
              JobIdentifier("229d6224-f332-4b76-b38e-830d7ea4737a"),
              1L,
              "EXPORT_CALENDAR_PDF",
              UserIdentifier("253b90dd-9feb-bb07-35cb-7c55a2c37d58"),
              JsonSerializedObject("ExportCalendarContext", "{}"),
              JsonSerializedObject("ExportCalendarCommand", "{}")))
    }

    @Test
    fun `drops duplicate command`() {
      val jobIdentifier = randomJobIdentifier()
      given(aJobQueuedEvent(jobIdentifier))

      whenDelivering(enqueueJobCommand.copy(jobIdentifier = jobIdentifier))

      thenNothingHappens()
    }

    @Test
    fun `rejects Jobs that would exceed a User's limit of active Jobs`(
        @Value("\${custom.job.max-active-per-user}") maxActiveJobsPerUser: Int
    ) {
      val maliciousUser = randomUserIdentifier()
      val jobToReject = randomJobIdentifier()
      given(
          generateSequence<JobEvent> { aJobQueuedEvent(userIdentifier = maliciousUser) }
              .take(maxActiveJobsPerUser)
              .toList())

      whenDelivering(
          enqueueJobCommand.copy(userIdentifier = maliciousUser, jobIdentifier = jobToReject))

      thenExpect(
          JobRejectedEvent(
              now,
              jobToReject,
              1L,
              "EXPORT_CALENDAR_PDF",
              maliciousUser,
              JsonSerializedObject("ExportCalendarContext", "{}")))
    }
  }

  @Nested
  inner class `On StartJobCommand` {

    @Test
    fun `publishes JobStartedEvent`() {
      val jobIdentifier = randomJobIdentifier()
      given(aJobQueuedEvent(jobIdentifier))

      whenDelivering(StartJobCommand(jobIdentifier))

      thenExpect(JobStartedEvent(now, jobIdentifier, 2L))
    }

    @Test
    fun `drops duplicate command`() {
      val jobIdentifier = randomJobIdentifier()
      given(aJobQueuedEvent(jobIdentifier), JobStartedEvent(now, jobIdentifier, 2L))

      whenDelivering(StartJobCommand(jobIdentifier))

      thenNothingHappens()
    }
  }

  @Nested
  inner class `On CompleteJobCommand` {

    @Test
    fun `publishes JobCompletedEvent`() {
      val jobIdentifier = randomJobIdentifier()
      given(aJobQueuedEvent(jobIdentifier), JobStartedEvent(now, jobIdentifier, 2L))

      whenDelivering(
          CompleteJobCommand(jobIdentifier, JsonSerializedObject("ExportCalendarResult", "{}")))

      thenExpect(
          JobCompletedEvent(
              now, jobIdentifier, 3L, JsonSerializedObject("ExportCalendarResult", "{}")))
    }

    @Test
    fun `drops duplicate command`() {
      val jobIdentifier = randomJobIdentifier()
      given(
          aJobQueuedEvent(jobIdentifier),
          JobStartedEvent(now, jobIdentifier, 2L),
          aJobCompletedEvent(jobIdentifier))

      whenDelivering(
          CompleteJobCommand(jobIdentifier, JsonSerializedObject("ExportCalendarResult", "{}")))

      thenNothingHappens()
    }
  }

  @Nested
  inner class `On FailJobCommand` {

    @Test
    fun `publishes JobFailedEvent`() {
      val jobIdentifier = randomJobIdentifier()
      given(aJobQueuedEvent(jobIdentifier), JobStartedEvent(now, jobIdentifier, 2L))

      whenDelivering(FailJobCommand(jobIdentifier))

      thenExpect(JobFailedEvent(now, jobIdentifier, 3L))
    }

    @Test
    fun `drops duplicate command`() {
      val jobIdentifier = randomJobIdentifier()
      given(
          aJobQueuedEvent(jobIdentifier),
          JobStartedEvent(now, jobIdentifier, 2L),
          JobFailedEvent(now, jobIdentifier, 3L))

      whenDelivering(FailJobCommand(jobIdentifier))

      thenNothingHappens()
    }
  }

  @Nested
  inner class `On MarkJobResultReadCommand` {
    @Test
    fun `publishes JobResultReadEvent`() {
      val user = authenticatedUserWith(randomUserIdentifier())
      val jobIdentifier = randomJobIdentifier()
      given(
          aJobQueuedEvent(jobIdentifier = jobIdentifier, userIdentifier = user),
          JobStartedEvent(now, jobIdentifier, 2L),
          aJobCompletedEvent(jobIdentifier))

      whenDelivering(MarkJobResultReadCommand(jobIdentifier))

      thenExpect(JobResultReadEvent(now, jobIdentifier, 4L))
    }

    @Test
    fun `denies access to Job of another User`() {
      val anotherUser = randomUserIdentifier()
      val jobIdentifier = randomJobIdentifier()
      given(
          aJobQueuedEvent(jobIdentifier = jobIdentifier, userIdentifier = anotherUser),
          JobStartedEvent(now, jobIdentifier, 2L),
          aJobCompletedEvent(jobIdentifier))
      authenticatedUserWith(randomUserIdentifier())

      assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
        whenDelivering(MarkJobResultReadCommand(jobIdentifier))
      }
      thenNothingHappens()
    }

    @Test
    fun `throws when marking a not COMPLETED Job as read`() {
      val user = authenticatedUserWith(randomUserIdentifier())
      val jobIdentifier = randomJobIdentifier()
      given(
          aJobQueuedEvent(jobIdentifier = jobIdentifier, userIdentifier = user),
          JobStartedEvent(now, jobIdentifier, 2L))

      assertThatExceptionOfType(OnlyCompletedJobsCanBeMarkedAsReadException::class.java)
          .isThrownBy { whenDelivering(MarkJobResultReadCommand(jobIdentifier)) }
      thenNothingHappens()
    }

    private fun authenticatedUserWith(userIdentifier: UserIdentifier) =
        userIdentifier.also {
          setAuthentication(
              UsernamePasswordAuthenticationToken(
                  JobServiceUserDetails(it, Locale.UK), "n/a", emptyList()))
        }
  }

  @Test
  fun `drops and ignores commands that would lead to invalid state transitions`() {
    val jobIdentifier = randomJobIdentifier()
    given(
        aJobQueuedEvent(jobIdentifier),
        JobStartedEvent(now, jobIdentifier, 2L),
        aJobCompletedEvent(jobIdentifier))

    assertThatNoException().isThrownBy { whenDelivering(StartJobCommand(jobIdentifier)) }
    thenNothingHappens()
  }

  private fun given(vararg events: JobEvent) {
    given(events.toList())
  }

  private fun given(events: List<JobEvent>) {
    events.forEach { jobSnapshotStore.update(it) }
  }

  private fun whenDelivering(command: JobCommand) {
    jobCommandDispatcher.dispatch(command)
  }

  private fun thenNothingHappens() {
    verify { jobEventPublisher wasNot called }
  }

  private fun thenExpect(vararg events: JobEvent) {
    verifySequence { events.forEach { jobEventPublisher.publish(it) } }
  }

  private fun aJobQueuedEvent(
      jobIdentifier: JobIdentifier = randomJobIdentifier(),
      userIdentifier: UserIdentifier = randomUserIdentifier()
  ) =
      JobQueuedEvent(
          now,
          jobIdentifier,
          1L,
          "EXPORT_CALENDAR_PDF",
          userIdentifier,
          JsonSerializedObject("ExportCalendarContext", "{}"),
          JsonSerializedObject("ExportCalendarCommand", "{}"))

  private fun aJobCompletedEvent(jobIdentifier: JobIdentifier) =
      JobCompletedEvent(now, jobIdentifier, 3L, JsonSerializedObject("ExportCalendarResult", "{}"))

  private fun randomUserIdentifier() = UserIdentifier(UUID.randomUUID().toString())

  private fun randomJobIdentifier() = JobIdentifier(UUID.randomUUID().toString())

  @BeforeEach
  fun setupMocks() {
    every { clock.instant() } returns now.toInstant(ZoneOffset.UTC)
    every { clock.zone } returns ZoneId.of("UTC")
    every { jobEventPublisher.publish(any()) } just runs
  }
}

@Import(
    JobCommandDispatcher::class,
    EnqueueJobCommandHandler::class,
    StartJobCommandHandler::class,
    CompleteJobCommandHandler::class,
    FailJobCommandHandler::class,
    MarkJobResultReadCommandHandler::class,
    JobSnapshotStore::class,
    MongoConfiguration::class,
    MongoIndexConfiguration::class,
    LoggerConfiguration::class)
private class JobCommandDispatchIntegrationTestConfiguration
