/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest

import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessage
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTaskAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicAttachment
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.taskattachment.message.getTaskIdentifier
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.attachment.boundary.BlobStoreService
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ABSTRACT_RESOURCE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.hasIdentifierAndVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isCreatedBy
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isLastModifiedBy
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.TASK
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.TASKATTACHMENT
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.TaskAttachmentController.Companion.ATTACHMENTS_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.TaskAttachmentController.Companion.ATTACHMENTS_ENDPOINT
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.TaskAttachmentController.Companion.ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.TaskAttachmentController.Companion.ATTACHMENT_BY_TASK_ID_AND_ATTACHMENT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.TaskAttachmentController.Companion.DOWNLOAD_FULL_BY_ATTACHMENT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.TaskAttachmentController.Companion.DOWNLOAD_ORIGINAL_BY_ATTACHMENT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.TaskAttachmentController.Companion.DOWNLOAD_PREVIEW_BY_ATTACHMENT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.TaskAttachmentController.Companion.PATH_VARIABLE_ATTACHMENT_ID
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.TaskAttachmentController.Companion.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.TaskAttachmentResource.Companion.LINK_DATA
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.TaskAttachmentResource.Companion.LINK_DELETE
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.TaskAttachmentResource.Companion.LINK_ORIGINAL
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.TaskAttachmentResource.Companion.LINK_PREVIEW
import com.bosch.pt.iot.smartsite.test.RandomData.multiPartFile
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.net.URI
import java.util.UUID.randomUUID
import org.apache.tika.mime.MimeTypes.OCTET_STREAM
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.mock.web.MockMultipartFile
import org.springframework.mock.web.MockPart
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.halLinks
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.NUMBER
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.partWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.restdocs.request.RequestDocumentation.requestParts
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Document need for task attachment api")
@EnableAllKafkaListeners
class TaskAttachmentApiDocumentationTest : AbstractApiDocumentationTestV2() {

  @MockkBean(relaxed = true) private lateinit var blobStoreService: BlobStoreService

  private val taskAttachmentSearchRequestParametersSnippet =
      buildIdentifierTypeAndPagingRequestParameter(TASK, TASKATTACHMENT)

  private val attachmentResponseFields =
      listOf(
          *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
          fieldWithPath("captureDate")
              .description("Date when the image was captured (if the attachment is an image)")
              .type(STRING)
              .optional(),
          fieldWithPath("fileName").description("Name of the file").type(STRING),
          fieldWithPath("fileSize").description("Size of the file").type(NUMBER),
          fieldWithPath("imageHeight")
              .description("Height of the image (if the attachment is an image)")
              .type(NUMBER),
          fieldWithPath("imageWidth")
              .description("Width of the image (if the attachment is an image)")
              .type(NUMBER),
          fieldWithPath("taskId")
              .description("ID of the task the attachment belongs to")
              .type(STRING),
          fieldWithPath("topicId")
              .description("ID of the topic the attachment belongs to")
              .type(STRING)
              .optional(),
          fieldWithPath("messageId")
              .description("ID of the message the attachment belongs to")
              .type(STRING)
              .optional(),
          subsectionWithPath("_links").ignored())

  private val attachmentResponseSnippet =
      responseFields()
          .andWithPrefix("attachments[]", attachmentResponseFields)
          .and(subsectionWithPath("_links").ignored().optional())

  private val attachmentSliceResponseSnippet =
      responseFields(
              fieldWithPath("pageNumber").description("Number of this page"),
              fieldWithPath("pageSize").description("Size of this page"))
          .andWithPrefix("attachments[]", attachmentResponseFields)
          .and(subsectionWithPath("_links").ignored())

  private val file: MockMultipartFile = multiPartFile()

  private val testUser by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }
  private val taskIdentifier by lazy { getIdentifier("task") }
  private val taskAttachmentIdentifier by lazy { getIdentifier("taskAttachment") }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData().submitTaskAttachment()

    setAuthentication(testUser.identifier!!)

    every { blobStoreService.saveImage(any(), any(), any()) }
        .answers {
          val data = arg<ByteArray>(0)
          val blobMetadata = arg<BlobMetadata>(2)
          Blob(randomUUID().toString(), data, blobMetadata, OCTET_STREAM)
        }
  }

  @Test
  @Throws(Exception::class)
  fun `saving a new task attachment with given file name`() {
    val attachmentId = randomUUID()

    mockMvc
        .perform(
            multipart(
                    latestVersionOf(ATTACHMENT_BY_TASK_ID_AND_ATTACHMENT_ID_ENDPOINT),
                    taskIdentifier,
                    attachmentId)
                .file(file)
                .part(MockPart("zoneOffset", "+1".toByteArray()))
                .accept(HAL_JSON_VALUE))
        .andExpectAll(
            status().isCreated,
            header()
                .string(
                    LOCATION,
                    Matchers.matchesRegex(
                        ".*${
                latestVersionOf(
                  "/projects/tasks/attachments/$attachmentId$"
                )
              }")),
            *hasIdentifierAndVersion(attachmentId),
            *isCreatedBy(testUser),
            *isLastModifiedBy(testUser),
        )
        .andDo(
            document(
                "tasks/document-save-task-attachment",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestParts(
                    partWithName("file").description("File to upload"),
                    partWithName("zoneOffset").description("Time zone offset"),
                ),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID)
                        .description("ID of the task where to attach the file"),
                    parameterWithName(PATH_VARIABLE_ATTACHMENT_ID)
                        .description("ID of the attachment (Optional)")
                        .optional()),
                links(
                    halLinks(),
                    linkWithRel(LINK_DATA).description("Link to download the attachment"),
                    linkWithRel(LINK_DELETE).description("Link to delete the attachment"),
                    linkWithRel(LINK_PREVIEW)
                        .description("Link to download a preview image of the attachment"),
                    linkWithRel(LINK_ORIGINAL)
                        .description("Link to download the original image of the attachment")),
                responseFields(attachmentResponseFields)))

    val taskAttachment = repositories.findTaskAttachment(attachmentId)!!

    projectEventStoreUtils
        .verifyContainsAndGet(
            TaskAttachmentEventAvro::class.java, TaskAttachmentEventEnumAvro.CREATED)
        .getAggregate()
        .also { aggregate ->
          validateCreatedAggregateAuditInfoAndAggregateIdentifier(
              aggregate, ProjectmanagementAggregateTypeEnum.TASKATTACHMENT, testUser)
          assertThat(aggregate.getTaskIdentifier()).isEqualTo(taskIdentifier)
          assertThat(aggregate.attachment.fileName).isEqualTo(file.originalFilename)
          assertThat(aggregate.attachment.fileSize).isEqualTo(file.size)
          assertThat(aggregate.attachment.fullAvailable).isEqualTo(taskAttachment.isFullAvailable())
          assertThat(aggregate.attachment.smallAvailable)
              .isEqualTo(taskAttachment.isSmallAvailable())
          if (taskAttachment.captureDate?.time != null) {
            assertThat(aggregate.attachment.captureDate).isEqualTo(taskAttachment.captureDate?.time)
          }
          assertThat(aggregate.attachment.height).isEqualTo(taskAttachment.imageHeight)
          assertThat(aggregate.attachment.width).isEqualTo(taskAttachment.imageWidth)
        }
  }

  @Test
  @Throws(Exception::class)
  fun `getting a task attachment by identifier`() {
    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf(ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT),
                    taskAttachmentIdentifier)))
        .andExpect(status().isOk)
        .andExpect(content().contentType(HAL_JSON_VALUE))
        .andDo(
            document(
                "tasks/document-get-task-attachment",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_ATTACHMENT_ID)
                        .description("ID of the attachment")),
                links(
                    halLinks(),
                    linkWithRel(LINK_DATA).description("Link to download the attachment"),
                    linkWithRel(LINK_DELETE).description("Link to delete the attachment"),
                    linkWithRel(LINK_PREVIEW)
                        .description("Link to download a preview image of the attachment"),
                    linkWithRel(LINK_ORIGINAL)
                        .description("Link to download the original image of the attachment")),
                responseFields(attachmentResponseFields)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  @Throws(Exception::class)
  fun `getting all attachments by task identifier`() {
    eventStreamGenerator
        .submitTask("newTask")
        .submitTaskAttachment(randomString())
        .submitTaskAttachment(randomString())
        .submitTaskAttachment(randomString())
    projectEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                get(
                        latestVersionOf(ATTACHMENTS_BY_TASK_ID_ENDPOINT),
                        getIdentifier("newTask").toString())
                    .param("includeChildren", "true")))
        .andExpect(status().isOk)
        .andExpect(content().contentType(HAL_JSON_VALUE))
        .andExpect(jsonPath("$.attachments").isArray)
        .andExpect(jsonPath("$.attachments.length()").value(3))
        .andDo(
            document(
                "tasks/document-get-task-attachments",
                preprocessResponse(prettyPrint()),
                queryParameters(
                    parameterWithName("includeChildren")
                        .description(
                            "Boolean indicating if topic and message attachments should be included in the response")),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TASK_ID).description("ID of the task")),
                attachmentResponseSnippet))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  @Throws(Exception::class)
  fun `getting all attachments of a set of task identifiers`() {
    eventStreamGenerator
        .submitTask("task1")
        .submitTopicG2(randomString())
        .submitMessage(randomString())
        .submitTaskAttachment(randomString())
        .submitTopicAttachment(randomString())
        .submitMessageAttachment(randomString())
        .submitTask("task2")
        .submitTopicG2(randomString())
        .submitMessage(randomString())
        .submitTaskAttachment(randomString())
        .submitTopicAttachment(randomString())
        .submitMessageAttachment(randomString())
    projectEventStoreUtils.reset()

    val batchRequestResource =
        BatchRequestResource(setOf(getIdentifier("task1"), getIdentifier("task2")))

    mockMvc
        .perform(
            requestBuilder(post(latestVersionOf(ATTACHMENTS_ENDPOINT)), batchRequestResource)
                .param("size", "5")
                .param("page", "0"))
        .andExpect(status().isOk)
        .andExpect(content().contentType(HAL_JSON_VALUE))
        .andExpect(jsonPath("$.attachments").isArray)
        .andExpect(jsonPath("$.attachments.length()").value(5))
        .andDo(
            document(
                "tasks/document-get-task-attachments-of-tasks",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS),
                taskAttachmentSearchRequestParametersSnippet,
                attachmentSliceResponseSnippet,
                links(NEXT_LINK_DESCRIPTOR)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  @Throws(Exception::class)
  fun `deleting of task attachment by identifier`() {
    val taskAttachment = repositories.findTaskAttachment(taskAttachmentIdentifier)!!

    mockMvc
        .perform(
            requestBuilder(
                delete(
                    latestVersionOf(ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT),
                    taskAttachmentIdentifier)))
        .andExpect(status().isNoContent)
        .andDo(
            document(
                "tasks/document-delete-task-attachment",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_ATTACHMENT_ID)
                        .description("ID of the attachment to delete."))))

    projectEventStoreUtils
        .verifyContainsAndGet(
            TaskAttachmentEventAvro::class.java, TaskAttachmentEventEnumAvro.DELETED)
        .getAggregate()
        .also { aggregate ->
          validateDeletedAggregateAuditInfoAndAggregateIdentifier(
              aggregate, taskAttachment, testUser)
        }
  }

  @Test
  @Throws(Exception::class)
  fun `downloading a preview of task attachment by identifier`() {
    val blobSecureUrl =
        URI.create(
                "https://path-to-blob-storage/csm/project/image/small/$taskIdentifier/$taskAttachmentIdentifier")
            .toURL()
    every { blobStoreService.generateSignedUrlForImage(any(), any()) } returns blobSecureUrl

    mockMvc
        .perform(
            get(
                    latestVersionOf(DOWNLOAD_PREVIEW_BY_ATTACHMENT_ID_ENDPOINT),
                    taskAttachmentIdentifier)
                .header(ACCEPT_LANGUAGE, "en"))
        .andExpect(status().isFound)
        .andExpect(header().string(LOCATION, `is`(blobSecureUrl.toString())))
        .andDo(
            document(
                "tasks/document-get-task-attachment-preview",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_ATTACHMENT_ID)
                        .description("ID of the attachment to download a preview for."))))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  @Throws(Exception::class)
  fun `downloading a data of task attachment by identifier`() {
    val blobSecureUrl =
        URI.create(
                "https://path-to-blob-storage/csm/project/image/fullhd/$taskIdentifier/$taskAttachmentIdentifier")
            .toURL()
    every { blobStoreService.generateSignedUrlForImage(any(), any()) } returns blobSecureUrl

    mockMvc
        .perform(
            get(latestVersionOf(DOWNLOAD_FULL_BY_ATTACHMENT_ID_ENDPOINT), taskAttachmentIdentifier)
                .header(ACCEPT_LANGUAGE, "en"))
        .andExpect(status().isFound)
        .andExpect(header().string(LOCATION, `is`(blobSecureUrl.toString())))
        .andDo(
            document(
                "tasks/document-get-task-attachment-data",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_ATTACHMENT_ID)
                        .description("ID of the attachment to download."))))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  @Throws(Exception::class)
  fun `downloading a original of task attachment by identifier`() {
    val blobSecureUrl =
        URI.create(
                "https://path-to-blob-storage/csm/project/image/original/$taskIdentifier/$taskAttachmentIdentifier")
            .toURL()
    every { blobStoreService.generateSignedUrlForImage(any(), any()) } returns blobSecureUrl

    mockMvc
        .perform(
            get(
                    latestVersionOf(DOWNLOAD_ORIGINAL_BY_ATTACHMENT_ID_ENDPOINT),
                    taskAttachmentIdentifier)
                .header(ACCEPT_LANGUAGE, "en"))
        .andExpect(status().isFound)
        .andExpect(header().string(LOCATION, `is`(blobSecureUrl.toString())))
        .andDo(
            document(
                "tasks/document-get-task-attachment-original",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_ATTACHMENT_ID)
                        .description("ID of the attachment to download."))))

    projectEventStoreUtils.verifyEmpty()
  }
}
