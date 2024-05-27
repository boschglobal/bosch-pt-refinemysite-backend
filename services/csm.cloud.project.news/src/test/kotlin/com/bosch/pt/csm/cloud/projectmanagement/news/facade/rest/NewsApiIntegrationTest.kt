/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.common.security.CustomTrustedJwtIssuersProperties
import com.bosch.pt.csm.cloud.common.security.CustomWebSecurityAutoConfiguration
import com.bosch.pt.csm.cloud.common.security.DefaultCustomUserAuthenticationConverter
import com.bosch.pt.csm.cloud.projectmanagement.application.config.WebMvcConfiguration
import com.bosch.pt.csm.cloud.projectmanagement.application.config.WebSecurityConfiguration
import com.bosch.pt.csm.cloud.projectmanagement.application.security.UserDetailsServiceImpl
import com.bosch.pt.csm.cloud.projectmanagement.application.security.WithMockSmartSiteUser
import com.bosch.pt.csm.cloud.projectmanagement.news.boundary.NewsService
import com.bosch.pt.csm.cloud.projectmanagement.news.boundary.ObjectRelationService
import com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.NewsController.Companion.NEWS_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.NewsController.Companion.NEWS_BY_TASK_ID_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.NewsController.Companion.NEWS_SEARCH_ENDPOINT
import com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.NewsController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.NewsController.Companion.PATH_VARIABLE_TASK_ID
import com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.resource.factory.NewsListResourceFactory
import com.bosch.pt.csm.cloud.projectmanagement.news.facade.rest.resource.factory.NewsResourceFactory
import com.bosch.pt.csm.cloud.projectmanagement.news.model.News
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.user.boundary.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.time.Instant
import java.util.Locale
import java.util.UUID
import java.util.UUID.randomUUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.security.oauth2.jwt.JwtDecoderFactory
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ExtendWith(SpringExtension::class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets/task-news")
@WithMockSmartSiteUser
@Import(
    CustomWebSecurityAutoConfiguration::class,
    NewsResourceFactory::class,
    NewsListResourceFactory::class,
    WebMvcConfiguration::class,
    WebSecurityConfiguration::class)
@WebMvcTest(controllers = [NewsController::class])
internal class NewsApiIntegrationTest {

  private val newsListResponseFields =
      responseFields(
          fieldWithPath("items[]").description("List of news"),
          fieldWithPath("items[].root.type")
              .description("The type of the root object. Is always TASK at the moment."),
          fieldWithPath("items[].root.identifier")
              .description("The identifier of the root object."),
          fieldWithPath("items[].parent.type")
              .description("The type of the parent object. Can be TASK, TOPIC or MESSAGE."),
          fieldWithPath("items[].parent.identifier")
              .description("The identifier of the parent object"),
          fieldWithPath("items[].context.type")
              .description(
                  "The type of the context object. Can be TASK, TOPIC, " +
                      "MESSAGE, TASKATTACHMENT, TOPICATTACHMENT or MESSAGEATTACHMENT"),
          fieldWithPath("items[].context.identifier")
              .description("The identifier of the context object"),
          fieldWithPath("items[].createdDate")
              .description("Date of the first event for which this news has been created."),
          fieldWithPath("items[].lastModifiedDate")
              .description("Date of the most recent event for which this news has been updated."),
          subsectionWithPath("items[]._links").ignored(),
          subsectionWithPath("_links").ignored())

  @MockkBean(relaxed = true) private lateinit var newsService: NewsService

  @MockkBean(relaxed = true) private lateinit var objectRelationService: ObjectRelationService

  @Suppress("Unused", "UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var userDetailsService: UserDetailsServiceImpl

  @Suppress("Unused", "UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var userService: UserService

  @Suppress("Unused", "UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var trustedJwtIssuerProperties: CustomTrustedJwtIssuersProperties

  @Suppress("Unused", "UnusedPrivateMember")
  @MockkBean(name = "defaultCustomUserAuthenticationConverter")
  private lateinit var customUserAuthenticationConverter: DefaultCustomUserAuthenticationConverter

  @Suppress("Unused", "UnusedPrivateMember")
  @MockkBean(relaxed = true)
  private lateinit var jwtDecoderFactory: JwtDecoderFactory<String>

  @Autowired private lateinit var mockMvc: MockMvc

  @Autowired private lateinit var apiVersionProperties: ApiVersionProperties

  private fun latestVersionOf(path: String): String {
    return "/v" + apiVersionProperties.version.max + path
  }

  @Test
  fun verifyAndDocumentFindAllNewsForUserAndTask() {
    val taskIdentifier = randomUUID()
    val topicIdentifier = randomUUID()
    val messageIdentifier = randomUUID()

    val newsForTask = createNewsForTask(taskIdentifier, NOW, NOW)
    val newsForTopic = createNewsForTopic(taskIdentifier, topicIdentifier)
    val newsForComment = createNewsForMessage(taskIdentifier, topicIdentifier, messageIdentifier)

    every { newsService.findAllByUserIdentifierAndRootObject(any(), any()) } returns
        listOf(newsForTask, newsForTopic, newsForComment)

    mockMvc
        .perform(
            get(latestVersionOf(NEWS_BY_TASK_ID_ENDPOINT), taskIdentifier)
                .locale(DEFAULT_LOCALE)
                .header(ACCEPT_HEADER, HAL_JSON_VALUE)
                .header(ACCEPT_LANGUAGE_HEADER, ACCEPT_LANGUAGE_VALUE_EN)
                .header(ACCEPT_LANGUAGE_HEADER, DEFAULT_LANGUAGE))
        .andExpect(status().isOk)
        .andDo(
            document(
                "document-get-task-news",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID)
                        .description("ID of the task the news belong to")),
                newsListResponseFields))
  }

  @Test
  fun verifyAndDocumentDeleteAllNewsForUserAndTask() {
    val taskIdentifier = randomUUID()

    mockMvc
        .perform(
            delete(latestVersionOf(NEWS_BY_TASK_ID_ENDPOINT), taskIdentifier)
                .locale(DEFAULT_LOCALE)
                .header(ACCEPT_HEADER, HAL_JSON_VALUE)
                .header(ACCEPT_LANGUAGE_HEADER, ACCEPT_LANGUAGE_VALUE_EN)
                .header(ACCEPT_LANGUAGE_HEADER, DEFAULT_LANGUAGE)
                .contentType(APPLICATION_JSON_VALUE))
        .andExpect(status().isNoContent)
        .andDo(
            document(
                "document-delete-task-news",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID)
                        .description("ID of the task the news belong to"))))
  }

  @Test
  fun verifyAndDocumentDeleteAllNewsForUserAndProject() {
    val projectIdentifier = randomUUID()

    every { objectRelationService.findTaskIdentifiers(projectIdentifier) } returns listOf()

    mockMvc
        .perform(
            delete(latestVersionOf(NEWS_BY_PROJECT_ID_ENDPOINT), projectIdentifier)
                .locale(DEFAULT_LOCALE)
                .header(ACCEPT_HEADER, HAL_JSON_VALUE)
                .header(ACCEPT_LANGUAGE_HEADER, ACCEPT_LANGUAGE_VALUE_EN)
                .header(ACCEPT_LANGUAGE_HEADER, DEFAULT_LANGUAGE)
                .contentType(APPLICATION_JSON_VALUE))
        .andExpect(status().isNoContent)
        .andDo(
            document(
                "document-delete-project-news",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID)
                        .description("ID of the project the news belong to"))))
  }

  @Test
  @Throws(Exception::class)
  fun verifyAndDocumentFindNewsForUserAndListOfTasks() {
    val task1Identifier = randomUUID()
    val task2Identifier = randomUUID()
    val task3Identifier = randomUUID()

    val listOfRequestedTasks = listOf<UUID>(task1Identifier, task2Identifier, task3Identifier)

    val now = Instant.now()
    val newsForTask1 = createNewsForTask(task1Identifier, now, now)
    val newsForTask2 = createNewsForTask(task2Identifier, now, now)
    val newsForTask3 = createNewsForTask(task3Identifier, now, now)

    every { newsService.findAllByUserIdentifierAndContextObjectsIn(any(), any()) } returns
        listOf(newsForTask1, newsForTask2, newsForTask3)

    mockMvc
        .perform(
            post(latestVersionOf(NEWS_SEARCH_ENDPOINT))
                .locale(DEFAULT_LOCALE)
                .header(ACCEPT_HEADER, HAL_JSON_VALUE)
                .header(ACCEPT_LANGUAGE_HEADER, ACCEPT_LANGUAGE_VALUE_EN)
                .header(ACCEPT_LANGUAGE_HEADER, DEFAULT_LANGUAGE)
                .contentType(APPLICATION_JSON_VALUE)
                .content(ObjectMapper().writeValueAsString(listOfRequestedTasks)))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.items.length()").value(3))
        .andDo(
            document(
                "document-search-task-news",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                newsListResponseFields))
  }

  private fun createNewsForTask(
      taskIdentifier: UUID,
      createdAt: Instant,
      updatedAt: Instant
  ): News =
      News(
          ObjectIdentifier("TASK", taskIdentifier),
          ObjectIdentifier("TASK", taskIdentifier),
          ObjectIdentifier("TASK", taskIdentifier),
          USER_IDENTIFIER,
          createdAt,
          updatedAt)

  private fun createNewsForTopic(taskIdentifier: UUID, topicIdentifier: UUID): News =
      News(
          ObjectIdentifier("TASK", taskIdentifier),
          ObjectIdentifier("TASK", taskIdentifier),
          ObjectIdentifier("TOPIC", topicIdentifier),
          USER_IDENTIFIER,
          NOW,
          NOW)

  private fun createNewsForMessage(
      taskIdentifier: UUID,
      topicIdentifier: UUID,
      messageIdentifier: UUID
  ): News =
      News(
          ObjectIdentifier("TASK", taskIdentifier),
          ObjectIdentifier("TOPIC", topicIdentifier),
          ObjectIdentifier("MESSAGE", messageIdentifier),
          USER_IDENTIFIER,
          NOW,
          NOW)

  companion object {
    private const val ACCEPT_HEADER = HttpHeaders.ACCEPT
    private const val ACCEPT_LANGUAGE_HEADER = HttpHeaders.ACCEPT_LANGUAGE
    private const val ACCEPT_LANGUAGE_VALUE_EN = "en"
    private const val DEFAULT_LANGUAGE = "en"
    private val DEFAULT_LOCALE = Locale.ENGLISH
    private val USER_IDENTIFIER = randomUUID()
    private val NOW = Instant.now()
  }
}
