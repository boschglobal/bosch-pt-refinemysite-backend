/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest

import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMessageAttachment
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.attachment.boundary.BlobStoreService
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets
import com.bosch.pt.iot.smartsite.project.message.facade.rest.MessageController.Companion.PATH_VARIABLE_MESSAGE_ID
import com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.MessageAttachmentController.Companion.ATTACHMENTS_BY_MESSAGE_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.MessageAttachmentController.Companion.ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.MessageAttachmentController.Companion.ATTACHMENT_BY_MESSAGE_ID_AND_ATTACHMENT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.MessageAttachmentController.Companion.FULL_ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.MessageAttachmentController.Companion.ORIGINAL_ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.MessageAttachmentController.Companion.PATH_VARIABLE_ATTACHMENT_ID
import com.bosch.pt.iot.smartsite.project.messageattachment.facade.rest.MessageAttachmentController.Companion.PREVIEW_ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.TaskAttachmentResource.Companion.LINK_DATA
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.TaskAttachmentResource.Companion.LINK_ORIGINAL
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.TaskAttachmentResource.Companion.LINK_PREVIEW
import com.bosch.pt.iot.smartsite.project.taskattachment.model.TaskAttachment
import com.bosch.pt.iot.smartsite.test.RandomData.multiPartFile
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import java.net.URI
import java.util.UUID.randomUUID
import org.apache.tika.mime.MimeTypes
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
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
import org.springframework.restdocs.request.RequestDocumentation.requestParts
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("Document need for message attachment api")
@EnableAllKafkaListeners
class MessageAttachmentApiDocumentationTest : AbstractApiDocumentationTestV2() {

  @MockkBean lateinit var blobStoreService: BlobStoreService

  private val attachmentResponseFields =
      listOf(
          *ApiDocumentationSnippets.ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
          fieldWithPath("id").description("ID of the attachment").type(STRING),
          fieldWithPath("captureDate")
              .description("Date when the image was captured (if the attachment is an image)")
              .optional()
              .type(STRING),
          fieldWithPath("fileName").description("Name of the file").type(STRING),
          fieldWithPath("fileSize").description("Size of the file").type(NUMBER),
          fieldWithPath("imageHeight")
              .description("Height of the image (if the attachment is an image)")
              .type(NUMBER),
          fieldWithPath("imageWidth")
              .description("Width of the image (if the attachment is an image)")
              .type(NUMBER),
          fieldWithPath("messageId")
              .description("ID of the message the attachment belongs to")
              .type(STRING),
          fieldWithPath("topicId")
              .description("ID of the topic the attachment belongs to")
              .type(STRING),
          fieldWithPath("version").description("ID of the attachment").type(NUMBER),
          fieldWithPath("taskId")
              .description("ID of the task the attachment belongs to")
              .type(STRING),
          subsectionWithPath("_links").ignored())

  private val attachmentResponseSnippet =
      responseFields().andWithPrefix("attachments[]", attachmentResponseFields)

  private val file = multiPartFile()

  private val testUser by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }
  private val messageIdentifier by lazy { getIdentifier("message") }
  private val messageAttachmentIdentifier by lazy { getIdentifier("messageAttachment") }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData().submitMessageAttachment()

    setAuthentication(testUser.identifier!!)

    every { blobStoreService.saveImage(any(), any(), any()) }
        .answers {
          val data = arg<ByteArray>(0)
          val blobMetadata = arg<BlobMetadata>(2)
          Blob(randomUUID().toString(), data, blobMetadata, MimeTypes.OCTET_STREAM)
        }
  }

  @Test
  @DisplayName("saving a new message attachment with given file name")
  fun verifyAndDocumentSave() {

    val messageCreator = repositories.findUser(getIdentifier("userCsm1"))!!
    setAuthentication(messageCreator.identifier!!)

    val attachmentId = randomUUID()

    mockMvc
        .perform(
            multipart(
                    latestVersionOf(ATTACHMENT_BY_MESSAGE_ID_AND_ATTACHMENT_ID_ENDPOINT),
                    messageIdentifier,
                    attachmentId)
                .file(file)
                .part(MockPart("zoneOffset", "+1".toByteArray()))
                .accept(HAL_JSON_VALUE))
        .andExpect(status().isCreated)
        .andExpect(header().string(LOCATION, notNullValue()))
        .andDo(
            document(
                "messages/document-save-message-attachment",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestParts(
                    partWithName("file").description("The file to upload"),
                    partWithName("zoneOffset").description("Time zone offset"),
                ),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_MESSAGE_ID)
                        .description("ID of the message where to attach the file"),
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
  }

  @Test
  @DisplayName("getting a message attachment by identifier")
  fun verifyAndDocumentFindOneByIdentifier() {
    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf(ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT),
                    messageAttachmentIdentifier)))
        .andExpect(status().isOk)
        .andExpect(content().contentType(HAL_JSON_VALUE))
        .andDo(
            document(
                "messages/document-get-message-attachment",
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
  }

  @Test
  @DisplayName("getting all message attachments of a message by message identifier")
  fun verifyAndDocumentFindAll() {
    eventStreamGenerator.submitMessageAttachment("messageAttachment2")

    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf(ATTACHMENTS_BY_MESSAGE_ID_ENDPOINT), messageIdentifier)))
        .andExpect(status().isOk)
        .andExpect(content().contentType(HAL_JSON_VALUE))
        .andExpect(jsonPath("$.attachments").isArray)
        .andExpect(jsonPath("$.attachments.length()").value(2))
        .andDo(
            document(
                "messages/document-get-message-attachments",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_MESSAGE_ID).description("ID of the message")),
                attachmentResponseSnippet))
  }

  @Test
  @DisplayName("getting a preview of message attachment by identifier")
  fun verifyAndDocumentDownloadAttachmentPreview() {

    val attachment: TaskAttachment = mockk()
    every { attachment.isSmallAvailable() }.returns(true)

    val blobSecureUrl =
        URI.create(
                "https://path-to-blob-storage/csm/project/image/small/$messageIdentifier/$messageAttachmentIdentifier")
            .toURL()

    every { blobStoreService.generateSignedUrlForImage(any(), any()) }.returns(blobSecureUrl)

    mockMvc
        .perform(
            requestBuilder(
                get(
                        latestVersionOf(PREVIEW_ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT),
                        messageAttachmentIdentifier)
                    .header(ACCEPT_LANGUAGE, "en")))
        .andExpect(status().isFound)
        .andExpect(header().string(LOCATION, `is`(blobSecureUrl.toString())))
        .andDo(
            document(
                "messages/document-get-message-attachment-preview",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_ATTACHMENT_ID)
                        .description("ID of the attachment to download a preview for."))))
  }

  @Test
  @DisplayName("getting a data of message attachment by identifier")
  fun verifyAndDocumentDownloadAttachmentData() {

    val attachment: TaskAttachment = mockk()
    every { attachment.isFullAvailable() }.returns(true)

    val blobSecureUrl =
        URI.create(
                "https://path-to-blob-storage/csm/project/image/original/" +
                    "$messageIdentifier/$messageAttachmentIdentifier")
            .toURL()

    every { blobStoreService.generateSignedUrlForImage(any(), any()) }.returns(blobSecureUrl)

    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf(FULL_ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT),
                    messageAttachmentIdentifier)))
        .andExpect(status().isFound)
        .andExpect(header().string(LOCATION, `is`(blobSecureUrl.toString())))
        .andDo(
            document(
                "messages/document-get-message-attachment-data",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_ATTACHMENT_ID)
                        .description("ID of the attachment to download."))))
  }

  @Test
  @DisplayName("getting a data of message attachment by identifier")
  fun verifyAndDocumentDownloadAttachmentOriginal() {

    val blobSecureUrl =
        URI.create(
                "https://path-to-blob-storage/csm/project/image/original/" +
                    "$messageIdentifier/$messageAttachmentIdentifier")
            .toURL()

    every { blobStoreService.generateSignedUrlForImage(any(), any()) }.returns(blobSecureUrl)

    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf(ORIGINAL_ATTACHMENT_BY_ATTACHMENT_ID_ENDPOINT),
                    messageAttachmentIdentifier)))
        .andExpect(status().isFound)
        .andExpect(header().string(LOCATION, `is`(blobSecureUrl.toString())))
        .andDo(
            document(
                "messages/document-get-message-attachment-original",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_ATTACHMENT_ID)
                        .description("ID of the attachment to download."))))
  }
}
