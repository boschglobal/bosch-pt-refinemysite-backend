/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.facade.rest

import com.bosch.pt.csm.cloud.common.security.CustomWebSecurityAutoConfiguration
import com.bosch.pt.csm.cloud.job.application.config.LoggerConfiguration
import com.bosch.pt.csm.cloud.job.application.config.MessageSourceConfiguration
import com.bosch.pt.csm.cloud.job.application.security.WebSecurityConfiguration
import com.bosch.pt.csm.cloud.job.job.api.JobIdentifier
import com.bosch.pt.csm.cloud.job.job.api.JsonSerializedObject
import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import com.bosch.pt.csm.cloud.job.job.command.handler.JobCommandDispatcher
import com.bosch.pt.csm.cloud.job.job.command.handler.exception.JobNotFoundException
import com.bosch.pt.csm.cloud.job.job.command.snapshot.JobState
import com.bosch.pt.csm.cloud.job.job.query.JobProjection
import com.bosch.pt.csm.cloud.job.job.query.JobProjectionRepository
import com.bosch.pt.csm.cloud.job.job.shared.JobList
import com.bosch.pt.csm.cloud.job.job.shared.JobListRepository
import com.bosch.pt.csm.cloud.job.user.query.ExternalUserIdentifier
import com.bosch.pt.csm.cloud.job.user.query.UserProjection
import com.bosch.pt.csm.cloud.job.user.query.security.JobServiceUserDetails
import com.bosch.pt.csm.cloud.job.user.query.security.JobServiceUserDetailsService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import java.time.LocalDateTime
import java.util.Locale
import java.util.Optional
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.snippet.Snippet
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.context.TestSecurityContextHolder.setAuthentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest
@AutoConfigureRestDocs
@Import(JobApiDocumentationTestConfiguration::class)
class JobApiDocumentationTest {

  @MockkBean lateinit var jobProjectionRepository: JobProjectionRepository
  @MockkBean lateinit var jobCommandDispatcher: JobCommandDispatcher
  @MockkBean lateinit var jobListRepository: JobListRepository

  @Autowired lateinit var mvc: MockMvc

  @Nested
  inner class `Getting Jobs` {

    @Test
    fun `lists all Jobs of the current User`() {
      val user = createAuthenticatedUser()
      val job =
          JobProjection(
              JobIdentifier("20f9a05b-f16e-4ead-8ec9-b43ba9855a28"),
              user.userIdentifier,
              "JOB_TYPE",
              JobState.COMPLETED,
              JsonSerializedObject("JobContext", """{"contextKey":"someValue"}"""),
              LocalDateTime.parse("2022-02-15T14:06:43.848"),
              LocalDateTime.parse("2022-02-15T14:08:07.384"),
              JsonSerializedObject("JobResult", """{"resultKey":"resultValue"}"""))
      every { jobProjectionRepository.findByUserIdentifier(any(), any()) } returns
          PageImpl(listOf(job))
      every { jobListRepository.findById(user.userIdentifier) } returns
          Optional.of(JobList(user.userIdentifier, LocalDateTime.parse("2022-02-15T08:01:14.987")))
      mvc.perform(get("/v1/jobs"))
          .andExpect(status().isOk)
          .andExpect(
              content()
                  .json(
                      """
                        {
                          "lastSeen": "2022-02-15T08:01:14.987Z",
                          "items": [{
                                  "id": "20f9a05b-f16e-4ead-8ec9-b43ba9855a28",
                                  "type": "JOB_TYPE",
                                  "status": "COMPLETED",
                                  "createdDate": "2022-02-15T14:06:43.848Z",
                                  "lastModifiedDate": "2022-02-15T14:08:07.384Z",
                                  "context": {"contextKey": "someValue"},
                                  "result": {"resultKey": "resultValue"},
                                  "read": false
                              }]
                        }
                        """
                          .trimIndent()))
          .andDo(
              document(
                  "list",
                  responseFields(
                      fieldWithPath("lastSeen")
                          .description(
                              "Most recent moment in which the current User opened their Job list (ISO timestamp)"),
                      fieldWithPath("items[].id").description("Unique identifier of the Job"),
                      fieldWithPath("items[].type")
                          .description(
                              "Type of Job as agreed between the service creating " +
                                  "the Job and the consumers (pass-through field)"),
                      fieldWithPath("items[].status")
                          .description(
                              "Current status of the Job (QUEUED, RUNNING, COMPLETED, FAILED, REJECTED)"),
                      fieldWithPath("items[].createdDate").description("Creation timestamp"),
                      fieldWithPath("items[].lastModifiedDate")
                          .description("Modification timestamp"),
                      fieldWithPath("items[].context")
                          .description(
                              "Context data provided by the creating service (pass-through field)")
                          .optional(),
                      subsectionWithPath("items[].context.*").ignored(),
                      fieldWithPath("items[].result")
                          .description(
                              "Result data provided by the service completing " +
                                  "the Job (only available for COMPLETED Jobs, pass-through field)")
                          .optional(),
                      subsectionWithPath("items[].result.*").ignored(),
                      fieldWithPath("items[].read")
                          .description(
                              "Flag indicating that the Job result was acknowledged by the User")
                          .optional(),
                      fieldWithPath("pageNumber").ignored(),
                      fieldWithPath("pageSize").ignored(),
                      fieldWithPath("totalPages").ignored(),
                      fieldWithPath("totalElements").ignored())))
    }

    @Test
    fun `responds with 401 - UNAUTHORIZED if user is not authenticated`() {

      mvc.perform(get("/v1/jobs"))
          .andExpect(status().isUnauthorized)
          .andDo(document("list-without-authentication"))
    }
  }

  @Test
  fun `updates the last-seen timestamp of the Job list for a User`() {
    createAuthenticatedUser()
    every { jobListRepository.save(any()) } returns null
    mvc.perform(
            post("/v1/jobs/seen")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"lastSeen":"2022-02-15T14:06:43.848Z"}"""))
        .andExpect(status().isAccepted)
        .andDo(
            document(
                "update-last-seen",
                requestFields(
                    fieldWithPath("lastSeen")
                        .description(
                            "An ISO timestamp of the moment the user saw the list of Jobs."))))
  }

  @Nested
  inner class `Marking a Job result as read` {

    @Test
    fun `marks a single Job result as read`() {
      createAuthenticatedUser()
      val jobIdentifier = JobIdentifier("20f9a05b-f16e-4ead-8ec9-b43ba9855a28")
      every { jobCommandDispatcher.dispatch(any()) } just runs
      mvc.perform(post("/v1/jobs/{jobIdentifier}/read", jobIdentifier.value))
          .andExpect(status().isAccepted)
          .andDo(
              document(
                  "mark-as-read",
                  pathParameters(
                      parameterWithName("jobIdentifier")
                          .description("Identifier of the Job to mark as read."))))
    }

    @Test
    fun `responds with 403 - FORBIDDEN if marking the Job result of a different user as read`() {
      createAuthenticatedUser()
      val jobIdentifier = JobIdentifier("20f9a05b-f16e-4ead-8ec9-b43ba9855a28")
      every { jobCommandDispatcher.dispatch(any()) } throws AccessDeniedException("unauthorized")
      mvc.perform(post("/v1/jobs/{jobIdentifier}/read", jobIdentifier.value))
          .andExpect(status().isForbidden)
          .andDo(document("mark-as-read-unauthorized"))
    }

    @Test
    fun `fails with localized error message`() {
      createAuthenticatedUser()
      val jobIdentifier = JobIdentifier("20f9a05b-f16e-4ead-8ec9-b43ba9855a28")
      every { jobCommandDispatcher.dispatch(any()) } throws JobNotFoundException(jobIdentifier)
      mvc.perform(
              post("/v1/jobs/{jobIdentifier}/read", jobIdentifier.value)
                  .header("Accept-Language", "pt"))
          .andExpect(status().isNotFound)
          .andExpect(
              content()
                  .json("""{"message":"O Job solicitado não foi encontrado.","traceId":"0"}"""))
          .andDo(document("mark-as-read-unknown-job"))
    }
  }

  private fun createAuthenticatedUser(): UserProjection {
    val user =
        UserProjection(
            UserIdentifier("42ad444b-0139-4f25-b10b-5b341a549f8c"),
            ExternalUserIdentifier("n/a"),
            Locale.UK)
    setAuthentication(
        UsernamePasswordAuthenticationToken(
            JobServiceUserDetails(user.userIdentifier, user.locale), "n/a", emptyList()))
    return user
  }

  private fun document(identifier: String, vararg snippets: Snippet) =
      MockMvcRestDocumentation.document(
          "jobs/$identifier",
          preprocessRequest(prettyPrint()),
          preprocessResponse(prettyPrint()),
          *snippets)
}

@Import(
    WebSecurityConfiguration::class,
    MessageSourceConfiguration::class,
    LoggerConfiguration::class,
)
@ImportAutoConfiguration(CustomWebSecurityAutoConfiguration::class)
private class JobApiDocumentationTestConfiguration {
  @MockkBean lateinit var jobServiceUserDetailsService: JobServiceUserDetailsService
}
