/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.facade.rest

import com.bosch.pt.csm.cloud.common.SmartSiteMockKTest
import com.bosch.pt.csm.cloud.job.job.api.JobIdentifier
import com.bosch.pt.csm.cloud.job.job.api.JsonSerializedObject
import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import com.bosch.pt.csm.cloud.job.job.command.snapshot.JobState
import com.bosch.pt.csm.cloud.job.job.query.JobProjection
import com.bosch.pt.csm.cloud.job.job.query.JobResource
import io.mockk.every
import io.mockk.impl.annotations.MockK
import jakarta.servlet.http.HttpServletRequest
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@SmartSiteMockKTest
class JobListResourceTest {

  @MockK(relaxed = true) lateinit var requestAttributes: ServletRequestAttributes

  @MockK(relaxed = true) lateinit var httpServletRequest: HttpServletRequest

  @BeforeEach
  fun init() {
    every { requestAttributes.request } returns httpServletRequest
    RequestContextHolder.setRequestAttributes(requestAttributes)
  }

  @Test
  fun `job list requested via old url with old urls in DB`() {
    every { httpServletRequest.requestURI } returns "/api/v1/jobs"

    val projection =
        JobProjection(
            JobIdentifier("20f9a05b-f16e-4ead-8ec9-b43ba9855a28"),
            UserIdentifier("ee76916d-d30c-4b63-b714-7a187e055102"),
            "JOB_TYPE",
            JobState.COMPLETED,
            JsonSerializedObject("JobContext", """{"contextKey":"someValue"}"""),
            LocalDateTime.parse("2022-02-15T14:06:43.848"),
            LocalDateTime.parse("2022-02-15T14:08:07.384"),
            JsonSerializedObject(
                "JobResult",
                """{"url":"https://sandbox1.bosch-refinemysite.com/api/v5/projects/..."}"""))

    val resource = assertPageAndReturn(projection)
    assertThat(resource.result)
        .contains("https://sandbox1.bosch-refinemysite.com/api/v5/projects/...")
  }

  @Test
  fun `job list requested via old url with new urls in DB`() {
    every { httpServletRequest.requestURI } returns "/api/v1/jobs"

    val projection =
        JobProjection(
            JobIdentifier("20f9a05b-f16e-4ead-8ec9-b43ba9855a28"),
            UserIdentifier("ee76916d-d30c-4b63-b714-7a187e055102"),
            "JOB_TYPE",
            JobState.COMPLETED,
            JsonSerializedObject("JobContext", """{"contextKey":"someValue"}"""),
            LocalDateTime.parse("2022-02-15T14:06:43.848"),
            LocalDateTime.parse("2022-02-15T14:08:07.384"),
            JsonSerializedObject(
                "JobResult",
                """{"url":"https://sandbox1-api.bosch-refinemysite.com/internal/v5/projects/..."}"""))

    val resource = assertPageAndReturn(projection)
    assertThat(resource.result)
        .contains("https://sandbox1.bosch-refinemysite.com/api/v5/projects/...")
  }

  @Test
  fun `job list requested via new url with old urls in DB`() {
    every { httpServletRequest.requestURI } returns "/internal/v1/jobs"

    val projection =
        JobProjection(
            JobIdentifier("20f9a05b-f16e-4ead-8ec9-b43ba9855a28"),
            UserIdentifier("ee76916d-d30c-4b63-b714-7a187e055102"),
            "JOB_TYPE",
            JobState.COMPLETED,
            JsonSerializedObject("JobContext", """{"contextKey":"someValue"}"""),
            LocalDateTime.parse("2022-02-15T14:06:43.848"),
            LocalDateTime.parse("2022-02-15T14:08:07.384"),
            JsonSerializedObject(
                "JobResult",
                """{"url":"https://sandbox1.bosch-refinemysite.com/api/v5/projects/..."}"""))

    val resource = assertPageAndReturn(projection)
    assertThat(resource.result)
        .contains("https://sandbox1-api.bosch-refinemysite.com/internal/v5/projects/...")
  }

  @Test
  fun `job list requested via new url with new urls in DB`() {
    every { httpServletRequest.requestURI } returns "/internal/v1/jobs"

    val projection =
        JobProjection(
            JobIdentifier("20f9a05b-f16e-4ead-8ec9-b43ba9855a28"),
            UserIdentifier("ee76916d-d30c-4b63-b714-7a187e055102"),
            "JOB_TYPE",
            JobState.COMPLETED,
            JsonSerializedObject("JobContext", """{"contextKey":"someValue"}"""),
            LocalDateTime.parse("2022-02-15T14:06:43.848"),
            LocalDateTime.parse("2022-02-15T14:08:07.384"),
            JsonSerializedObject(
                "JobResult",
                """{"url":"https://sandbox1-api.bosch-refinemysite.com/internal/v5/projects/..."}"""))

    val resource = assertPageAndReturn(projection)
    assertThat(resource.result)
        .contains("https://sandbox1-api.bosch-refinemysite.com/internal/v5/projects/...")
  }

  @Test
  fun `job list requested via new url with new urls in DB for prod`() {
    every { httpServletRequest.requestURI } returns "/internal/v1/jobs"

    val projection =
      JobProjection(
        JobIdentifier("20f9a05b-f16e-4ead-8ec9-b43ba9855a28"),
        UserIdentifier("ee76916d-d30c-4b63-b714-7a187e055102"),
        "JOB_TYPE",
        JobState.COMPLETED,
        JsonSerializedObject("JobContext", """{"contextKey":"someValue"}"""),
        LocalDateTime.parse("2022-02-15T14:06:43.848"),
        LocalDateTime.parse("2022-02-15T14:08:07.384"),
        JsonSerializedObject(
          "JobResult",
          """{"url":"https://api.bosch-refinemysite.com/internal/v5/projects/..."}"""))

    val resource = assertPageAndReturn(projection)
    assertThat(resource.result)
      .contains("https://api.bosch-refinemysite.com/internal/v5/projects/...")
  }

  @Test
  fun `job list requested via new url with old urls in DB for prod`() {
    every { httpServletRequest.requestURI } returns "/internal/v1/jobs"

    val projection =
        JobProjection(
            JobIdentifier("20f9a05b-f16e-4ead-8ec9-b43ba9855a28"),
            UserIdentifier("ee76916d-d30c-4b63-b714-7a187e055102"),
            "JOB_TYPE",
            JobState.COMPLETED,
            JsonSerializedObject("JobContext", """{"contextKey":"someValue"}"""),
            LocalDateTime.parse("2022-02-15T14:06:43.848"),
            LocalDateTime.parse("2022-02-15T14:08:07.384"),
            JsonSerializedObject(
                "JobResult",
                """{"url":"https://app.bosch-refinemysite.com/api/v5/projects/..."}"""))

    val resource = assertPageAndReturn(projection)
    assertThat(resource.result)
        .contains("https://api.bosch-refinemysite.com/internal/v5/projects/...")
  }

  @Test
  fun `job list requested via old url with old urls in DB for prod`() {
    every { httpServletRequest.requestURI } returns "/api/v1/jobs"

    val projection =
      JobProjection(
        JobIdentifier("20f9a05b-f16e-4ead-8ec9-b43ba9855a28"),
        UserIdentifier("ee76916d-d30c-4b63-b714-7a187e055102"),
        "JOB_TYPE",
        JobState.COMPLETED,
        JsonSerializedObject("JobContext", """{"contextKey":"someValue"}"""),
        LocalDateTime.parse("2022-02-15T14:06:43.848"),
        LocalDateTime.parse("2022-02-15T14:08:07.384"),
        JsonSerializedObject(
          "JobResult",
          """{"url":"https://app.bosch-refinemysite.com/api/v5/projects/..."}"""))

    val resource = assertPageAndReturn(projection)
    assertThat(resource.result)
      .contains("https://app.bosch-refinemysite.com/api/v5/projects/...")
  }

  @Test
  fun `job list requested via old url with new urls in DB for prod`() {
    every { httpServletRequest.requestURI } returns "/api/v1/jobs"

    val projection =
        JobProjection(
            JobIdentifier("20f9a05b-f16e-4ead-8ec9-b43ba9855a28"),
            UserIdentifier("ee76916d-d30c-4b63-b714-7a187e055102"),
            "JOB_TYPE",
            JobState.COMPLETED,
            JsonSerializedObject("JobContext", """{"contextKey":"someValue"}"""),
            LocalDateTime.parse("2022-02-15T14:06:43.848"),
            LocalDateTime.parse("2022-02-15T14:08:07.384"),
            JsonSerializedObject(
                "JobResult",
                """{"url":"https://api.bosch-refinemysite.com/internal/v5/projects/..."}"""))

    val resource = assertPageAndReturn(projection)
    assertThat(resource.result).contains("https://app.bosch-refinemysite.com/api/v5/projects/...")
  }

  private fun assertPageAndReturn(projection: JobProjection): JobResource {
    val page = PageImpl(listOf(projection))
    val resource = JobListResource(page, null)
    assertThat(resource.items).hasSize(1)

    return resource.items.first()
  }
}
