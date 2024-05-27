/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest

import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TOPICATTACHMENT
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicAttachment
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAttachmentEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topicattachment.message.getTopicIdentifier
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.attachment.boundary.BlobStoreService
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ABSTRACT_RESOURCE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.hasIdentifierAndVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isCreatedBy
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isLastModifiedBy
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.TaskAttachmentResource.Companion.LINK_DATA
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.TaskAttachmentResource.Companion.LINK_ORIGINAL
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.TaskAttachmentResource.Companion.LINK_PREVIEW
import com.bosch.pt.iot.smartsite.project.taskattachment.model.TaskAttachment
import com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.TopicAttachmentController.Companion.ATTACHMENTS_BY_TOPIC_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.TopicAttachmentController.Companion.ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.TopicAttachmentController.Companion.ATTACHMENT_BY_TOPIC_ID_AND_ATTACHMENT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.TopicAttachmentController.Companion.DOWNLOAD_FULL_BY_ATTACHMENT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.TopicAttachmentController.Companion.DOWNLOAD_ORIGINAL_BY_ATTACHMENT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.TopicAttachmentController.Companion.DOWNLOAD_PREVIEW_BY_ATTACHMENT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.TopicAttachmentController.Companion.PATH_VARIABLE_ATTACHMENT_ID
import com.bosch.pt.iot.smartsite.project.topicattachment.facade.rest.TopicAttachmentController.Companion.PATH_VARIABLE_TOPIC_ID
import com.bosch.pt.iot.smartsite.test.RandomData.multiPartFile
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import java.net.URI
import java.util.UUID.randomUUID
import org.apache.tika.mime.MimeTypes.OCTET_STREAM
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.mock.web.MockPart
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.halLinks
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.NUMBER
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
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

@DisplayName("Document need for topic attachment api")
@EnableAllKafkaListeners
class TopicAttachmentApiDocumentationTest : AbstractApiDocumentationTestV2() {

  @MockkBean lateinit var blobStoreService: BlobStoreService

  private var attachmentResponseFields =
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
          fieldWithPath("topicId")
              .description("ID of the topic the attachment belongs to")
              .type(STRING),
          fieldWithPath("version").description("Version of the topic the attachment").type(NUMBER),
          fieldWithPath("messageId")
              .description("ID of the message the attachment belongs to")
              .type(STRING)
              .optional(),
          fieldWithPath("taskId").description("ID of the task the topic belongs to").type(STRING),
          subsectionWithPath("_links").ignored())

  private var attachmentResponseSnippet =
      responseFields().andWithPrefix("attachments[]", attachmentResponseFields)

  private var file = multiPartFile()

  private val testUser by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }
  private val topicIdentifier by lazy { getIdentifier("topic") }
  private val topicAttachmentIdentifier by lazy { getIdentifier("topicAttachment") }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData().submitTopicAttachment()

    setAuthentication(testUser.identifier!!)

    every { blobStoreService.saveImage(any(), any(), any()) }
        .answers {
          val data = arg<ByteArray>(0)
          val blobMetadata = arg<BlobMetadata>(2)
          Blob(randomUUID().toString(), data, blobMetadata, OCTET_STREAM)
        }
  }

  @Test
  @DisplayName("saving a new topic attachment with given file name")
  @Throws(Exception::class)
  fun verifyAndDocumentSave() {
    val topicCreator = repositories.findUser(getIdentifier("userCsm1"))!!
    setAuthentication(topicCreator.identifier!!)

    val attachmentId = randomUUID()

    mockMvc
        .perform(
            multipart(
                    latestVersionOf(ATTACHMENT_BY_TOPIC_ID_AND_ATTACHMENT_ID_ENDPOINT),
                    topicIdentifier,
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
                    "/projects/tasks/topics/attachments/$attachmentId$"
                  )
                }")),
            *hasIdentifierAndVersion(attachmentId),
            *isCreatedBy(topicCreator),
            *isLastModifiedBy(topicCreator),
        )
        .andExpect(header().string(LOCATION, notNullValue()))
        .andDo(
            document(
                "topics/document-save-topic-attachment",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestParts(
                    partWithName("file").description("The file to upload"),
                    partWithName("zoneOffset").description("Time zone offset"),
                ),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TOPIC_ID)
                        .description("ID of the topic where to attach the file"),
                    parameterWithName(PATH_VARIABLE_ATTACHMENT_ID)
                        .description("ID for the attachment (Optional)")
                        .optional()),
                links(
                    halLinks(),
                    linkWithRel(LINK_DATA).description("Link to download the attachment"),
                    linkWithRel(LINK_PREVIEW)
                        .description("Link to download a preview image of the attachment"),
                    linkWithRel(LINK_ORIGINAL)
                        .description("Link to download the original image of the attachment")),
                responseFields(attachmentResponseFields)))

    val topicAttachment = repositories.findTopicAttachment(attachmentId)!!

    projectEventStoreUtils
        .verifyContainsAndGet(
            TopicAttachmentEventAvro::class.java, TopicAttachmentEventEnumAvro.CREATED)
        .getAggregate()
        .also { aggregate ->
          validateCreatedAggregateAuditInfoAndAggregateIdentifier(
              aggregate, TOPICATTACHMENT, topicCreator)
          assertThat(aggregate.getTopicIdentifier()).isEqualTo(topicIdentifier)
          assertThat(aggregate.attachment.fileName).isEqualTo(file.originalFilename)
          assertThat(aggregate.attachment.fileSize).isEqualTo(file.size)
          assertThat(aggregate.attachment.fullAvailable)
              .isEqualTo(topicAttachment.isFullAvailable())
          assertThat(aggregate.attachment.smallAvailable)
              .isEqualTo(topicAttachment.isSmallAvailable())
          // TODO capture date is null, is this exptected?
          // assertThat(aggregate.getAttachment().getCaptureDate())
          //     .isEqualTo(topicAttachment.captureDate?.time)
          assertThat(aggregate.attachment.height).isEqualTo(topicAttachment.imageHeight)
          assertThat(aggregate.attachment.width).isEqualTo(topicAttachment.imageWidth)
        }
  }

  @Test
  @DisplayName("getting a topic attachment by identifier")
  @Throws(Exception::class)
  fun verifyAndDocumentFindOneByIdentifier() {
    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf(ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT),
                    topicAttachmentIdentifier)))
        .andExpect(status().isOk)
        .andExpect(content().contentType(HAL_JSON_VALUE))
        .andDo(
            document(
                "topics/document-get-topic-attachment",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_ATTACHMENT_ID)
                        .description("ID of the attachment")),
                links(
                    halLinks(),
                    linkWithRel(LINK_DATA).description("Link to download the attachment"),
                    linkWithRel(LINK_PREVIEW)
                        .description("Link to download a preview image of the attachment"),
                    linkWithRel(LINK_ORIGINAL)
                        .description("Link to download the original image of the attachment")),
                responseFields(attachmentResponseFields)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  @DisplayName("getting all attachments by topic identifier")
  @Throws(Exception::class)
  fun verifyAndDocumentFindAll() {
    eventStreamGenerator.submitTopicAttachment("topicAttachment2")
    projectEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf(ATTACHMENTS_BY_TOPIC_ID_ENDPOINT), topicIdentifier.toString())
                    .param("includeChildren", "true")))
        .andExpect(status().isOk)
        .andExpect(content().contentType(HAL_JSON_VALUE))
        .andExpect(jsonPath("$.attachments").isArray)
        .andExpect(jsonPath("$.attachments.length()").value(2))
        .andDo(
            document(
                "topics/document-get-topic-attachments",
                preprocessResponse(prettyPrint()),
                queryParameters(
                    parameterWithName("includeChildren")
                        .description(
                            "Boolean indicating if message attachments should be included in the response")),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_TOPIC_ID).description("ID of the topic")),
                attachmentResponseSnippet))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  @DisplayName("downloading a preview of topic attachment by identifier")
  @Throws(Exception::class)
  fun verifyAndDocumentDownloadAttachmentPreview() {

    val attachment: TaskAttachment = mockk()
    every { attachment.isSmallAvailable() }.returns(true)

    val blobSecureUrl =
        URI.create(
                "https://path-to-blob-storage/csm/project/image/small/$topicIdentifier/$topicAttachmentIdentifier")
            .toURL()

    every { blobStoreService.generateSignedUrlForImage(any(), any()) }.returns(blobSecureUrl)

    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf(DOWNLOAD_PREVIEW_BY_ATTACHMENT_ID_ENDPOINT),
                    topicAttachmentIdentifier)))
        .andExpect(status().isFound)
        .andExpect(header().string(LOCATION, `is`(blobSecureUrl.toString())))
        .andDo(
            document(
                "topics/document-get-topic-attachment-preview",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_ATTACHMENT_ID)
                        .description("ID of the attachment to download a preview for."))))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  @DisplayName("downloading a data of topic attachment by identifier")
  @Throws(Exception::class)
  fun verifyAndDocumentDownloadAttachmentData() {

    val attachment: TaskAttachment = mockk()
    every { attachment.isFullAvailable() }.returns(true)

    val blobSecureUrl =
        URI.create(
                "https://path-to-blob-storage/csm/project/image/original/$topicIdentifier/$topicAttachmentIdentifier")
            .toURL()

    every { blobStoreService.generateSignedUrlForImage(any(), any()) }.returns(blobSecureUrl)

    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf(DOWNLOAD_FULL_BY_ATTACHMENT_ID_ENDPOINT),
                    topicAttachmentIdentifier)))
        .andExpect(status().isFound)
        .andExpect(header().string(LOCATION, `is`(blobSecureUrl.toString())))
        .andDo(
            document(
                "topics/document-get-topic-attachment-data",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_ATTACHMENT_ID)
                        .description("ID of the attachment to download."))))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  @DisplayName("downloading a original of topic attachment by identifier")
  @Throws(Exception::class)
  fun verifyAndDocumentDownloadAttachmentOriginal() {

    val blobSecureUrl =
        URI.create(
                "https://path-to-blob-storage/csm/project/image/original/$topicIdentifier/$topicAttachmentIdentifier")
            .toURL()

    every { blobStoreService.generateSignedUrlForImage(any(), any()) }.returns(blobSecureUrl)

    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf(DOWNLOAD_ORIGINAL_BY_ATTACHMENT_ID_ENDPOINT),
                    topicAttachmentIdentifier)))
        .andExpect(status().isFound)
        .andExpect(header().string(LOCATION, `is`(blobSecureUrl.toString())))
        .andDo(
            document(
                "topics/document-get-topic-attachment-original",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_ATTACHMENT_ID)
                        .description("ID of the attachment to download."))))

    projectEventStoreUtils.verifyEmpty()
  }
}
