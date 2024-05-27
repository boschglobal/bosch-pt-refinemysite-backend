/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.message.message.getTopicIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ABSTRACT_RESOURCE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER_DESCRIPTOR
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.LOCATION_HEADER_DESCRIPTOR
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.hasIdentifierAndVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isCreatedBy
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isLastModifiedBy
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isSlice
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.MESSAGE
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.TASK
import com.bosch.pt.iot.smartsite.project.message.domain.asMessageId
import com.bosch.pt.iot.smartsite.project.message.facade.rest.MessageController.Companion.PATH_VARIABLE_MESSAGE_ID
import com.bosch.pt.iot.smartsite.project.message.facade.rest.MessageController.Companion.PATH_VARIABLE_TOPIC_ID
import com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.request.CreateMessageResource
import com.bosch.pt.iot.smartsite.project.message.shared.model.Message
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.request.CreateTopicResource
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.matchesRegex
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.OBJECT
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class MessageApiDocumentationTest : AbstractApiDocumentationTestV2() {

  private val creatorUser by lazy { repositories.findUser(getIdentifier("userCsm1"))!! }
  private val testUser by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }
  private val topic by lazy { repositories.findTopic(getIdentifier("topic").asTopicId())!! }
  private val message by lazy {
    repositories.findMessage(getIdentifier("message1").asMessageId())!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitMessage(asReference = "message1") { it.content = "content" }
        .submitMessageAttachment(asReference = "attachment1")
        .submitMessageAttachment(asReference = "attachment2")
        .submitMessage(asReference = "message2")
        .submitMessage(asReference = "message3")
        .submitMessage(asReference = "message4")
        .submitMessage(asReference = "message5")
        .submitTask(asReference = "otherTask")
        .submitTopicG2(asReference = "otherTopic")
        .submitMessage(asReference = "otherMessage1")
        .submitMessageAttachment(asReference = "otherAttachment")
        .submitMessage(asReference = "otherMessage2")

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document create a message`() {
    val resource = CreateMessageResource("content")

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf("/projects/tasks/topics/{topicId}/messages"), topic.identifier),
                resource))
        .andExpectAll(
            status().isCreated,
            header().string(ETAG_HEADER, "\"0\""),
            header()
                .string(
                    LOCATION,
                    matchesRegex(
                        ".*${latestVersionOf("/projects/tasks/topics/${topic.identifier}/messages")}" +
                            "/[-a-z0-9]{36}$")),
            *hasIdentifierAndVersion(),
            *isCreatedBy(testUser),
            *isLastModifiedBy(testUser),
            jsonPath("$.content").value("content"),
            jsonPath("$.topicId").value(topic.identifier.toString()),
            jsonPath("$._links.delete.href").exists())
        .andDo(
            document(
                "messages/document-create-message",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(MESSAGE_CREATE_PATH_PARAMETER_DESCRIPTOR),
                requestFields(MESSAGE_CREATE_REQUEST_FIELD_DESCRIPTORS),
                responseHeaders(ETAG_HEADER_DESCRIPTOR, LOCATION_HEADER_DESCRIPTOR),
                responseFields(MESSAGE_RESPONSE_FIELD_DESCRIPTORS),
                links(DELETE_LINK_DESCRIPTOR)))

    projectEventStoreUtils
        .verifyContainsAndGet(MessageEventAvro::class.java, CREATED, 1, true)
        .also { verifyCreatedAggregate(it[0].aggregate, resource, topic.identifier) }
  }

  @Test
  fun `verify and document create a message with identifier`() {
    val identifier = randomUUID()
    val resource = CreateMessageResource("content")

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf("/projects/tasks/topics/{topicId}/messages/{messageId}"),
                    topic.identifier,
                    identifier),
                resource))
        .andExpectAll(
            status().isCreated,
            header().string(ETAG_HEADER, "\"0\""),
            header()
                .string(
                    LOCATION,
                    matchesRegex(
                        ".*${latestVersionOf("/projects/tasks/topics/${topic.identifier}/messages")}/$identifier$")),
            *hasIdentifierAndVersion(identifier),
            *isCreatedBy(testUser),
            *isLastModifiedBy(testUser),
            jsonPath("$.content").value("content"),
            jsonPath("$.topicId").value(topic.identifier.toString()),
            jsonPath("$._links.delete.href").exists())
        .andDo(
            document(
                "messages/document-create-message-with-identifier",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(MESSAGE_CREATE_PATH_PARAMETER_DESCRIPTOR),
                requestFields(MESSAGE_CREATE_REQUEST_FIELD_DESCRIPTORS),
                responseHeaders(ETAG_HEADER_DESCRIPTOR, LOCATION_HEADER_DESCRIPTOR),
                responseFields(MESSAGE_RESPONSE_FIELD_DESCRIPTORS),
                links(DELETE_LINK_DESCRIPTOR)))

    projectEventStoreUtils
        .verifyContainsAndGet(MessageEventAvro::class.java, CREATED, 1, true)
        .also { verifyCreatedAggregate(it[0].aggregate, resource, topic.identifier) }
  }

  @Test
  fun `verify and document find one message`() {
    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf("/projects/tasks/topics/messages/{messageId}"),
                    message.identifier)))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"0\""),
            *hasIdentifierAndVersion(message.identifier.identifier),
            *isCreatedBy(creatorUser),
            *isLastModifiedBy(creatorUser),
            jsonPath("$.content").value("content"),
            jsonPath("$.topicId").value(topic.identifier.toString()),
            jsonPath("$._links.delete.href").exists(),
            jsonPath("$._embedded.attachments").exists(),
            jsonPath("$._embedded.attachments.attachments").exists(),
            jsonPath("$._embedded.attachments.attachments.length()").value(2))
        .andDo(
            document(
                "messages/document-find-message",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(MESSAGE_PATH_PARAMETER_DESCRIPTOR),
                responseHeaders(ETAG_HEADER_DESCRIPTOR),
                responseFields(MESSAGE_RESPONSE_FIELD_DESCRIPTORS),
                links(DELETE_LINK_DESCRIPTOR)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document find all messages by topic using before and limit`() {
    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf("/projects/tasks/topics/{topicId}/messages"), topic.identifier)
                    .param("limit", "2")
                    .param("before", getIdentifier("message4").toString())))
        .andExpectAll(
            status().isOk,
            jsonPath("$.messages").exists(),
            jsonPath("$.messages.length()").value(2),
            jsonPath("$.messages[0].id").value(getIdentifier("message3").toString()),
            jsonPath("$.messages[1].id").value(getIdentifier("message2").toString()),
            jsonPath("$._links.prev.href").exists())
        .andDo(
            document(
                "messages/document-find-all-messages",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(MESSAGE_SEARCH_PATH_PARAMETER_DESCRIPTOR),
                MESSAGE_RESPONSE_FIELDS,
                links(PREVIOUS_LINK_DESCRIPTOR)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document find all messages in batch by task`() {
    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf("/projects/tasks/topics/messages"))
                    .param("size", "3")
                    .param("page", "1")
                    .param("identifierType", TASK),
                BatchRequestResource(setOf(getIdentifier("task"), getIdentifier("otherTask")))))
        .andExpectAll(
            status().isOk,
            jsonPath("$.messages").exists(),
            jsonPath("$.messages.length()").value(3),
            *isSlice(pageNumber = 1, pageSize = 3))
        .andDo(
            document(
                "messages/document-find-all-messages-for-task-identifiers",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                buildIdentifierTypeAndPagingRequestParameter(TASK, MESSAGE),
                requestFields(BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS),
                MESSAGE_WITH_PAGE_RESPONSE_FIELDS,
                links(PREVIOUS_LINK_DESCRIPTOR, NEXT_LINK_DESCRIPTOR)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document to delete a message`() {
    mockMvc
        .perform(
            requestBuilder(
                delete(
                    latestVersionOf("/projects/tasks/topics/messages/{messageId}"),
                    message.identifier)))
        .andExpect(status().isNoContent)
        .andDo(
            document(
                "messages/document-delete-message-with-identifier",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(MESSAGE_PATH_PARAMETER_DESCRIPTOR)))

    projectEventStoreUtils
        .verifyContainsAndGet(MessageEventAvro::class.java, DELETED, 1, true)
        .also { verifyDeletedAggregate(it[0].aggregate, message, topic.identifier) }
  }

  private fun verifyCreatedAggregate(
      aggregate: MessageAggregateAvro,
      resource: CreateMessageResource,
      topicIdentifier: TopicId
  ) =
      with(aggregate) {
        validateCreatedAggregateAuditInfoAndAggregateIdentifier(
            this, ProjectmanagementAggregateTypeEnum.MESSAGE, testUser)
        assertThat(getTopicIdentifier()).isEqualTo(topicIdentifier.identifier)
        assertThat(content).isEqualTo(resource.content)
      }

  private fun verifyDeletedAggregate(
      aggregate: MessageAggregateAvro,
      message: Message,
      topicIdentifier: TopicId
  ) =
      with(aggregate) {
        validateDeletedAggregateAuditInfoAndAggregateIdentifier(
            this, message, ProjectmanagementAggregateTypeEnum.MESSAGE, testUser)
        assertThat(getTopicIdentifier()).isEqualTo(topicIdentifier.identifier)
        assertThat(content).isEqualTo(message.content)
      }

  companion object {

    private val MESSAGE_PATH_PARAMETER_DESCRIPTOR =
        listOf(parameterWithName(PATH_VARIABLE_MESSAGE_ID).description("ID of the message"))

    private val MESSAGE_CREATE_PATH_PARAMETER_DESCRIPTOR =
        listOf(
            parameterWithName(PATH_VARIABLE_TOPIC_ID)
                .description("ID of the topic the message belongs to"),
            parameterWithName("messageId")
                .description(
                    "ID of the message to be created. Is optional and will be auto generated if omitted")
                .optional())

    private val MESSAGE_SEARCH_PATH_PARAMETER_DESCRIPTOR =
        listOf(
            parameterWithName(PATH_VARIABLE_TOPIC_ID)
                .description("ID of the topic the messages belong to"),
            parameterWithName("before")
                .description("ID of the message that will serve as an offset")
                .optional(),
            parameterWithName("limit")
                .description("The maximum number of messages to return in the response")
                .optional())

    private val MESSAGE_CREATE_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            ConstrainedFields(CreateTopicResource::class.java)
                .withPath("content")
                .description("User defined content")
                .type(STRING))

    private val MESSAGE_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
            fieldWithPath("topicId")
                .description("ID of the parent topic of this message.")
                .type(STRING),
            fieldWithPath("content").description("Content of the message").type(STRING),
            subsectionWithPath("_links").ignored(),
            subsectionWithPath("_embedded")
                .description("Embedded resources")
                .type(OBJECT)
                .optional())

    private val MESSAGE_RESPONSE_FIELDS =
        responseFields(
                fieldWithPath("messages[]").description("List of messages").type(ARRAY),
                subsectionWithPath("_links").ignored())
            .andWithPrefix("messages[].", MESSAGE_RESPONSE_FIELD_DESCRIPTORS)

    private val MESSAGE_WITH_PAGE_RESPONSE_FIELDS =
        responseFields(
                fieldWithPath("messages[]").description("List of messages").type(ARRAY),
                fieldWithPath("pageNumber").description("Number of this page"),
                fieldWithPath("pageSize").description("Size of this page"),
                subsectionWithPath("_links").ignored())
            .andWithPrefix("messages[].", MESSAGE_RESPONSE_FIELD_DESCRIPTORS)
  }
}
