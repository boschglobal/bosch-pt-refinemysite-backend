/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.facade.rest

import com.bosch.pt.csm.cloud.common.command.messages.DeleteCommandAvro
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.messages.CommandMessageKeyAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.topic.message.getTaskIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.DEESCALATED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventEnumAvro.ESCALATED
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventG2Avro
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum
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
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.TASK
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.TOPIC
import com.bosch.pt.iot.smartsite.common.kafka.messaging.impl.CommandSendingServiceTestDouble
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.topic.domain.asTopicId
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.TopicController.Companion.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.TopicController.Companion.PATH_VARIABLE_TOPIC_ID
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.request.CreateTopicResource
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.CRITICAL
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.UNCRITICAL
import java.util.UUID
import org.apache.avro.specific.SpecificRecord
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.matchesRegex
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.hypermedia.LinkDescriptor
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.NUMBER
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
class TopicApiDocumentationTest : AbstractApiDocumentationTestV2() {

  @Autowired private lateinit var commandSendingService: CommandSendingServiceTestDouble

  private val creatorUser by lazy { repositories.findUser(getIdentifier("userCsm1"))!! }
  private val testUser by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }
  private val task by lazy { repositories.findTaskWithDetails(getIdentifier("task").asTaskId())!! }
  private val topic by lazy { repositories.findTopic(getIdentifier("topic").asTopicId())!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitTopicG2(asReference = "topic") { it.description = "Topic description" }
        .submitTopicAttachment(asReference = "attachment1")
        .submitTopicAttachment(asReference = "attachment2")
        .submitMessage(asReference = "message")
        .submitTopicG2(asReference = "topic2")
        .submitTopicG2(asReference = "topic3")
        .submitTopicG2(asReference = "topic4")
        .submitTopicG2(asReference = "topic5")
        .submitTask(asReference = "otherTask")
        .submitTopicG2(asReference = "criticalTopic") {
          it.criticality = TopicCriticalityEnumAvro.CRITICAL
        }
        .submitTopicAttachment(asReference = "otherAttachment")
        .submitMessage(asReference = "otherMessage1")
        .submitTopicG2(asReference = "uncriticalTopic") {
          it.criticality = TopicCriticalityEnumAvro.UNCRITICAL
        }
        .submitTopicAttachment(asReference = "otherAttachment2")
        .submitMessage(asReference = "otherMessage2")

    setAuthentication(getIdentifier("userCsm2"))
    commandSendingService.clearRecords()
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document create topic`() {
    val resource = CreateTopicResource("Topic description", CRITICAL)

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf("/projects/tasks/{taskId}/topics"), task.identifier),
                resource))
        .andExpectAll(
            status().isCreated,
            header().string(ETAG_HEADER, "\"0\""),
            header()
                .string(
                    LOCATION,
                    matchesRegex(
                        ".*${latestVersionOf("/projects/tasks/${task.identifier}/topics")}/[-a-z0-9]{36}$")),
            *hasIdentifierAndVersion(),
            *isCreatedBy(testUser),
            *isLastModifiedBy(testUser),
            jsonPath("$.description").value("Topic description"),
            jsonPath("$.criticality").value(CRITICAL.name),
            jsonPath("$.taskId").value(task.identifier.toString()),
            jsonPath("$.messages").value(0),
            jsonPath("$._links.deescalate.href").exists(),
            jsonPath("$._links.messages.href").exists(),
            jsonPath("$._links.createMessage.href").exists(),
            jsonPath("$._links.delete.href").exists())
        .andDo(
            document(
                "topics/document-create-topic",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(TOPIC_CREATE_PATH_PARAMETER_DESCRIPTOR),
                requestFields(TOPIC_CREATE_REQUEST_FIELD_DESCRIPTORS),
                responseHeaders(ETAG_HEADER_DESCRIPTOR, LOCATION_HEADER_DESCRIPTOR),
                responseFields(TOPIC_RESPONSE_FIELD_DESCRIPTORS),
                links(CRITICAL_TOPIC_LINK_DESCRIPTORS)))

    projectEventStoreUtils
        .verifyContainsAndGet(TopicEventG2Avro::class.java, CREATED, 1, true)
        .also { verifyCreatedAggregate(it[0].aggregate, resource, task.identifier.toUuid()) }
  }

  @Test
  fun `verify and document create topic with identifier`() {
    val identifier = UUID.randomUUID()
    val resource = CreateTopicResource("Topic description", CRITICAL)

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf("/projects/tasks/{taskId}/topics/{topicId}"),
                    task.identifier,
                    identifier),
                resource))
        .andExpectAll(
            status().isCreated,
            header().string(ETAG_HEADER, "\"0\""),
            header()
                .string(
                    LOCATION,
                    matchesRegex(
                        ".*${latestVersionOf("/projects/tasks/${task.identifier}/topics")}/$identifier\$")),
            *hasIdentifierAndVersion(identifier),
            *isCreatedBy(testUser),
            *isLastModifiedBy(testUser),
            jsonPath("$.description").value("Topic description"),
            jsonPath("$.criticality").value(CRITICAL.name),
            jsonPath("$.taskId").value(task.identifier.toString()),
            jsonPath("$.messages").value(0),
            jsonPath("$._links.deescalate.href").exists(),
            jsonPath("$._links.messages.href").exists(),
            jsonPath("$._links.createMessage.href").exists(),
            jsonPath("$._links.delete.href").exists())
        .andDo(
            document(
                "topics/document-create-topic-with-identifier",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(TOPIC_CREATE_PATH_PARAMETER_DESCRIPTOR),
                requestFields(TOPIC_CREATE_REQUEST_FIELD_DESCRIPTORS),
                responseHeaders(ETAG_HEADER_DESCRIPTOR, LOCATION_HEADER_DESCRIPTOR),
                responseFields(TOPIC_RESPONSE_FIELD_DESCRIPTORS),
                links(CRITICAL_TOPIC_LINK_DESCRIPTORS)))

    projectEventStoreUtils
        .verifyContainsAndGet(TopicEventG2Avro::class.java, CREATED, 1, true)
        .also { verifyCreatedAggregate(it[0].aggregate, resource, task.identifier.toUuid()) }
  }

  @Test
  fun `verify and document find topic`() {
    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf("/projects/tasks/topics/{topicId}"), topic.identifier)))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"0\""),
            *hasIdentifierAndVersion(topic.identifier.toUuid()),
            *isCreatedBy(creatorUser),
            *isLastModifiedBy(creatorUser),
            jsonPath("$.description").value(topic.description),
            jsonPath("$.criticality").value(UNCRITICAL.name),
            jsonPath("$.taskId").value(task.identifier.toString()),
            jsonPath("$.messages").value(1),
            jsonPath("$._links.escalate.href").exists(),
            jsonPath("$._links.messages.href").exists(),
            jsonPath("$._links.createMessage.href").exists(),
            jsonPath("$._links.delete.href").exists(),
            jsonPath("$._embedded.attachments").exists(),
            jsonPath("$._embedded.attachments.attachments").exists(),
            jsonPath("$._embedded.attachments.attachments.length()").value(2))
        .andDo(
            document(
                "topics/document-find-topic",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(TOPIC_PATH_PARAMETER_DESCRIPTOR),
                responseHeaders(ETAG_HEADER_DESCRIPTOR),
                responseFields(TOPIC_RESPONSE_FIELD_DESCRIPTORS),
                links(UNCRITICAL_TOPIC_LINK_DESCRIPTORS)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document find all topics by task using before and limit`() {
    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf("/projects/tasks/{taskId}/topics"), task.identifier)
                    .param("limit", "2")
                    .param("before", getIdentifier("topic4").toString())))
        .andExpectAll(
            status().isOk,
            jsonPath("$.topics").exists(),
            jsonPath("$.topics.length()").value(2),
            jsonPath("$.topics[0].id").value(getIdentifier("topic3").toString()),
            jsonPath("$.topics[1].id").value(getIdentifier("topic2").toString()),
            jsonPath("$._links.prev.href").exists())
        .andDo(
            document(
                "topics/document-find-all-topics",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(TOPIC_SEARCH_PATH_PARAMETER_DESCRIPTOR),
                TOPIC_RESPONSE_FIELDS,
                links(PREVIOUS_LINK_DESCRIPTOR)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document find all topics in batch by task`() {
    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf("/projects/tasks/topics"))
                    .param("size", "3")
                    .param("page", "1")
                    .param("identifierType", TASK),
                BatchRequestResource(setOf(getIdentifier("task"), getIdentifier("otherTask")))))
        .andExpectAll(
            status().isOk,
            jsonPath("$.topics").exists(),
            jsonPath("$.topics.length()").value(3),
            *isSlice(pageNumber = 1, pageSize = 3))
        .andDo(
            document(
                "topics/document-find-all-topics-for-task-identifiers",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                buildIdentifierTypeAndPagingRequestParameter(TASK, TOPIC),
                requestFields(BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS),
                TOPIC_WITH_PAGE_RESPONSE_FIELDS,
                links(PREVIOUS_LINK_DESCRIPTOR, NEXT_LINK_DESCRIPTOR)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document escalate topic`() {
    val uncriticalTopic = repositories.findTopic(getIdentifier("uncriticalTopic").asTopicId())!!

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf("/projects/tasks/topics/{topicId}/escalate"),
                    uncriticalTopic.identifier)))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"1\""),
            *hasIdentifierAndVersion(uncriticalTopic.identifier.toUuid(), 1),
            *isCreatedBy(creatorUser),
            *isLastModifiedBy(testUser),
            jsonPath("$.description").value(uncriticalTopic.description),
            jsonPath("$.criticality").value(CRITICAL.name),
            jsonPath("$.taskId").value(getIdentifier("otherTask").toString()),
            jsonPath("$.messages").value(1),
            jsonPath("$._links.deescalate.href").exists(),
            jsonPath("$._links.messages.href").exists(),
            jsonPath("$._links.createMessage.href").exists(),
            jsonPath("$._links.delete.href").exists(),
            jsonPath("$._embedded.attachments").exists(),
            jsonPath("$._embedded.attachments.attachments").exists(),
            jsonPath("$._embedded.attachments.attachments.length()").value(1))
        .andDo(
            document(
                "topics/document-escalate-topic",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(TOPIC_PATH_PARAMETER_DESCRIPTOR),
                responseHeaders(ETAG_HEADER_DESCRIPTOR),
                responseFields(TOPIC_RESPONSE_FIELD_DESCRIPTORS),
                links(CRITICAL_TOPIC_LINK_DESCRIPTORS)))

    val updatedUncriticalTopic =
        repositories.findTopic(getIdentifier("uncriticalTopic").asTopicId())!!
    projectEventStoreUtils
        .verifyContainsAndGet(TopicEventG2Avro::class.java, ESCALATED, 1, true)
        .also {
          verifyUpdatedAggregate(
              it[0].aggregate, updatedUncriticalTopic, getIdentifier("otherTask"))
        }
  }

  @Test
  fun `verify and document deescalate topic`() {
    val criticalTopic = repositories.findTopic(getIdentifier("criticalTopic").asTopicId())!!

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf("/projects/tasks/topics/{topicId}/deescalate"),
                    criticalTopic.identifier)))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"1\""),
            *hasIdentifierAndVersion(criticalTopic.identifier.toUuid(), 1),
            *isCreatedBy(creatorUser),
            *isLastModifiedBy(testUser),
            jsonPath("$.description").value(criticalTopic.description),
            jsonPath("$.criticality").value(UNCRITICAL.name),
            jsonPath("$.taskId").value(getIdentifier("otherTask").toString()),
            jsonPath("$.messages").value(1),
            jsonPath("$._links.escalate.href").exists(),
            jsonPath("$._links.messages.href").exists(),
            jsonPath("$._links.createMessage.href").exists(),
            jsonPath("$._links.delete.href").exists(),
            jsonPath("$._embedded.attachments").exists(),
            jsonPath("$._embedded.attachments.attachments").exists(),
            jsonPath("$._embedded.attachments.attachments.length()").value(1))
        .andDo(
            document(
                "topics/document-deescalate-topic",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(TOPIC_PATH_PARAMETER_DESCRIPTOR),
                responseHeaders(ETAG_HEADER_DESCRIPTOR),
                responseFields(TOPIC_RESPONSE_FIELD_DESCRIPTORS),
                links(UNCRITICAL_TOPIC_LINK_DESCRIPTORS)))

    val updatedCriticalTopic = repositories.findTopic(getIdentifier("criticalTopic").asTopicId())!!
    projectEventStoreUtils
        .verifyContainsAndGet(TopicEventG2Avro::class.java, DEESCALATED, 1, true)
        .also {
          verifyUpdatedAggregate(it[0].aggregate, updatedCriticalTopic, getIdentifier("otherTask"))
        }
  }

  @Test
  fun `verify and document delete a topic`() {
    mockMvc
        .perform(
            requestBuilder(
                delete(latestVersionOf("/projects/tasks/topics/{topicId}"), topic.identifier)))
        .andExpect(status().isNoContent)
        .andDo(
            document(
                "topics/document-delete-topic-with-identifier",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(TOPIC_PATH_PARAMETER_DESCRIPTOR)))

    // The deleted topic operation is asynchronous, therefore we just check that it was mark to be
    // deleted and that the delete command is correct
    val deletedTopic = repositories.findTopic(getIdentifier("topic").asTopicId())!!
    assertThat(deletedTopic.isDeleted()).isTrue

    commandSendingService.capturedRecords.also {
      assertThat(it).hasSize(1)
      it.first().run {
        verifyCommandKey(key, getIdentifier("project"))
        verifyCommandValue(value, deletedTopic)
      }
    }
  }

  private fun verifyCreatedAggregate(
      aggregate: TopicAggregateG2Avro,
      resource: CreateTopicResource,
      taskIdentifiers: UUID
  ) =
      with(aggregate) {
        validateCreatedAggregateAuditInfoAndAggregateIdentifier(
            this, ProjectmanagementAggregateTypeEnum.TOPIC, testUser)
        assertThat(getTaskIdentifier()).isEqualTo(taskIdentifiers)
        assertThat(description).isEqualTo(resource.description)
        assertThat(criticality.name).isEqualTo(resource.criticality.name)
      }

  private fun verifyUpdatedAggregate(
      aggregate: TopicAggregateG2Avro,
      topic: Topic,
      taskIdentifiers: UUID
  ) =
      with(aggregate) {
        validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
            this, topic, ProjectmanagementAggregateTypeEnum.TOPIC)
        assertThat(getTaskIdentifier()).isEqualTo(taskIdentifiers)
        assertThat(description).isEqualTo(topic.description)
        assertThat(criticality.name).isEqualTo(topic.criticality.name)
      }

  private fun verifyCommandKey(key: SpecificRecord, projectIdentifier: UUID) {
    assertThat(key).isInstanceOf(CommandMessageKeyAvro::class.java)
    with(key as CommandMessageKeyAvro) {
      assertThat(partitioningIdentifier).isEqualTo(projectIdentifier.toString())
    }
  }

  private fun verifyCommandValue(value: SpecificRecord, topic: Topic) {
    assertThat(value).isInstanceOf(DeleteCommandAvro::class.java)
    with(value as DeleteCommandAvro) {
      with(aggregateIdentifier) {
        assertThat(identifier).isEqualTo(topic.identifier.toString())
        assertThat(version).isEqualTo(topic.version)
        assertThat(type).isEqualTo(ProjectmanagementAggregateTypeEnum.TOPIC.value)
      }
      with(userIdentifier) {
        assertThat(identifier).isEqualTo(testUser.identifier.toString())
        assertThat(version).isEqualTo(testUser.version)
        assertThat(type).isEqualTo(UsermanagementAggregateTypeEnum.USER.value)
      }
    }
  }

  companion object {

    private val TOPIC_PATH_PARAMETER_DESCRIPTOR =
        listOf(parameterWithName(PATH_VARIABLE_TOPIC_ID).description("ID of the topic"))

    private val TOPIC_CREATE_PATH_PARAMETER_DESCRIPTOR =
        listOf(
            parameterWithName(PATH_VARIABLE_TASK_ID)
                .description("ID of the task the topic belongs to"),
            parameterWithName(PATH_VARIABLE_TOPIC_ID)
                .description(
                    "ID of the topic to be created. Is optional and will be auto generated if omitted")
                .optional())

    private val TOPIC_SEARCH_PATH_PARAMETER_DESCRIPTOR =
        listOf(
            parameterWithName(PATH_VARIABLE_TASK_ID)
                .description("ID of the task the topic belong to"),
            parameterWithName("before")
                .description("ID of the topic that will serve as an offset")
                .optional(),
            parameterWithName("limit")
                .description("The maximum number of topics to return in the response")
                .optional())

    private var TOPIC_CREATE_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            ConstrainedFields(CreateTopicResource::class.java)
                .withPath("description")
                .description("User defined description")
                .type(STRING),
            ConstrainedFields(CreateTopicResource::class.java)
                .withPath("criticality")
                .description("Criticality for the topic")
                .type(STRING))

    private val TOPIC_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
            fieldWithPath("taskId")
                .description("ID of the parent task of this topic.")
                .type(STRING),
            fieldWithPath("criticality").description("Criticality of the topic").type(STRING),
            fieldWithPath("description").description("Free description of the topic").type(STRING),
            fieldWithPath("messages").description("Number of messages of the topic").type(NUMBER),
            subsectionWithPath("_links").ignored(),
            subsectionWithPath("_embedded")
                .description("Embedded resources")
                .type(OBJECT)
                .optional())

    private val TOPIC_RESPONSE_FIELDS =
        responseFields(
                fieldWithPath("topics[]").description("List of topics").type(ARRAY),
                subsectionWithPath("_links").ignored())
            .andWithPrefix("topics[].", TOPIC_RESPONSE_FIELD_DESCRIPTORS)

    private val TOPIC_WITH_PAGE_RESPONSE_FIELDS =
        responseFields(
                fieldWithPath("topics[]").description("List of topics").type(ARRAY),
                fieldWithPath("pageNumber").description("Number of this page"),
                fieldWithPath("pageSize").description("Size of this page"),
                subsectionWithPath("_links").ignored())
            .andWithPrefix("topics[].", TOPIC_RESPONSE_FIELD_DESCRIPTORS)

    private val LINK_ESCALATE_DESCRIPTOR: LinkDescriptor =
        linkWithRel("escalate")
            .description("Link to change the topic resource to criticality 'CRITICAL'")
    private val LINK_DEESCALATE_DESCRIPTOR: LinkDescriptor =
        linkWithRel("deescalate")
            .description("Link to change the topic resource to criticality 'UNCRITICAL'")
    private val LINK_MESSAGE_DESCRIPTOR: LinkDescriptor =
        linkWithRel("messages").description("Link to the message resources of the topic")
    private val LINK_CREATE_MESSAGE_DESCRIPTOR: LinkDescriptor =
        linkWithRel("createMessage").description("Link to create a message resources of the topic")

    private val CRITICAL_TOPIC_LINK_DESCRIPTORS =
        listOf(
            DELETE_LINK_DESCRIPTOR,
            LINK_DEESCALATE_DESCRIPTOR,
            LINK_MESSAGE_DESCRIPTOR,
            LINK_CREATE_MESSAGE_DESCRIPTOR)

    private val UNCRITICAL_TOPIC_LINK_DESCRIPTORS =
        listOf(
            DELETE_LINK_DESCRIPTOR,
            LINK_ESCALATE_DESCRIPTOR,
            LINK_MESSAGE_DESCRIPTOR,
            LINK_CREATE_MESSAGE_DESCRIPTOR)
  }
}
