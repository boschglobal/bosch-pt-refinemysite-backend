/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECTPICTURE
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectPicture
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.attachment.boundary.BlobStoreService
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest.ProjectPictureController.Companion.PATH_VARIABLE_PICTURE_ID
import com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest.ProjectPictureController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest.ProjectPictureController.Companion.PATH_VARIABLE_SIZE
import com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest.ProjectPictureController.Companion.PICTURE_BY_PROJECT_ID_AND_PICTURE_ID_AND_SIZE_ENDPOINT
import com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest.ProjectPictureController.Companion.PICTURE_BY_PROJECT_ID_AND_PICTURE_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest.ProjectPictureController.Companion.PICTURE_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.test.RandomData.multiPartFile
import com.bosch.pt.iot.smartsite.util.getIdentifier
import com.google.common.net.HttpHeaders.LOCATION
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.net.URI
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.mock.web.MockMultipartFile
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.partWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.requestParts
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class ProjectPictureApiDocumentationTest : AbstractApiDocumentationTestV2() {

  @MockkBean(relaxed = true) lateinit var blobStoreService: BlobStoreService

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }
  private val projectAggregate by lazy { get<ProjectAggregateAvro>("project")!! }
  private val userCsm2 by lazy {
    repositories.userRepository.findOneByIdentifier(getIdentifier("userCsm2"))!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication("userCsm2")
    projectEventStoreUtils.reset()
  }

  @Test
  fun verifyAndDocumentSaveProjectPicture() {
    val file = MockMultipartFile("file", multiPartFile().bytes)

    mockMvc
        .perform(
            multipart(latestVersionOf(PICTURE_BY_PROJECT_ID_ENDPOINT), projectIdentifier)
                .file(file)
                .accept(HAL_JSON_VALUE))
        .andExpect(status().isCreated)
        .andExpect(jsonPath("projectReference.id").value(projectIdentifier.toString()))
        .andExpect(header().string(LOCATION, notNullValue()))
        .andDo(
            document(
                "projects/document-save-project-picture-without-identifier",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project")),
                requestParts(partWithName("file").description("Project picture file")),
                PROJECT_PICTURE_FIELDS,
                PICTURE_DATA_LINKS))

    val projectPicture =
        repositories.projectPictureRepository.findOneByProjectIdentifier(projectIdentifier)!!

    projectEventStoreUtils
        .verifyContainsAndGet(ProjectPictureEventAvro::class.java, CREATED, true)
        .getAggregate()
        .also {
          validateCreatedAggregateAuditInfoAndAggregateIdentifier(it, PROJECTPICTURE, userCsm2)
          assertThat(it.getProject())
              .isEqualByComparingTo(projectAggregate.getAggregateIdentifier())
          assertThat(it.getFileSize()).isEqualTo(projectPicture.fileSize)
          assertThat(it.getHeight()).isEqualTo(projectPicture.height)
          assertThat(it.getWidth()).isEqualTo(projectPicture.width)
          assertThat(it.getFullAvailable()).isFalse
          assertThat(it.getSmallAvailable()).isFalse
        }
  }

  @Test
  fun verifyAndDocumentSaveProjectPictureWithIdentifier() {
    val projectPictureIdentifier = randomUUID()
    val file = MockMultipartFile("file", multiPartFile().bytes)

    mockMvc
        .perform(
            multipart(
                    latestVersionOf(PICTURE_BY_PROJECT_ID_AND_PICTURE_ID_ENDPOINT),
                    projectIdentifier,
                    projectPictureIdentifier)
                .file(file)
                .accept(HAL_JSON_VALUE))
        .andExpect(status().isCreated)
        .andExpect(header().string(LOCATION, notNullValue()))
        .andExpect(jsonPath("id").value(projectPictureIdentifier.toString()))
        .andDo(
            document(
                "projects/document-save-project-picture-with-identifier",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project"),
                    parameterWithName(PATH_VARIABLE_PICTURE_ID)
                        .description("ID of the project picture")),
                requestParts(partWithName("file").description("Project picture file")),
                PROJECT_PICTURE_FIELDS,
                PICTURE_DATA_LINKS))

    projectEventStoreUtils
        .verifyContainsAndGet(ProjectPictureEventAvro::class.java, CREATED, true)
        .getAggregate()
        .also { assertThat(it.getIdentifier()).isEqualTo(projectPictureIdentifier) }
  }

  @Test
  fun verifySaveProjectPictureWithBlankFilename() {
    val file = multiPartFile()

    mockMvc
        .perform(
            multipart(latestVersionOf(PICTURE_BY_PROJECT_ID_ENDPOINT), projectIdentifier)
                .file("file", file.bytes)
                .accept(HAL_JSON_VALUE))
        .andExpect(status().isCreated)
        .andExpect(jsonPath("projectReference.id").value(projectIdentifier.toString()))
        .andExpect(header().string(LOCATION, notNullValue()))

    projectEventStoreUtils.verifyContains(ProjectPictureEventAvro::class.java, CREATED, 1)
  }

  @Test
  fun verifyAndDocumentFindProjectPictureMetaData() {
    eventStreamGenerator.submitProjectPicture()

    mockMvc
        .perform(
            get(latestVersionOf(PICTURE_BY_PROJECT_ID_ENDPOINT), projectIdentifier)
                .accept(HAL_JSON_VALUE))
        .andExpect(status().isOk)
        .andDo(
            document(
                "projects/document-find-project-picture-meta-data",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project")),
                PROJECT_PICTURE_FIELDS,
                PICTURE_DATA_LINKS))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun verifyAndDocumentFindProjectPicture() {
    eventStreamGenerator.submitProjectPicture()
    val projectPictureIdentifier = getIdentifier("projectPicture")

    val blobSecureUrl =
        URI.create(
                "https://path-to-blob-storage/csm/project/image/original/" +
                    projectIdentifier +
                    "/" +
                    projectPictureIdentifier)
            .toURL()

    every { blobStoreService.generateSignedUrlForImage(any(), any()) } returns blobSecureUrl

    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf(PICTURE_BY_PROJECT_ID_AND_PICTURE_ID_AND_SIZE_ENDPOINT),
                    projectIdentifier,
                    projectPictureIdentifier,
                    "full")))
        .andExpect(status().isFound)
        .andExpect(header().string(LOCATION, `is`(blobSecureUrl.toString())))
        .andDo(
            document(
                "projects/document-find-project-picture",
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project"),
                    parameterWithName(PATH_VARIABLE_PICTURE_ID)
                        .description("ID of the project picture"),
                    parameterWithName(PATH_VARIABLE_SIZE)
                        .description(
                            "Size of the project picture. Either \"small\" or \"full\"."))))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun verifyAndDocumentDeleteProjectPicture() {
    eventStreamGenerator.submitProjectPicture()

    val projectPicture =
        repositories.projectPictureRepository.findOneByProjectIdentifier(projectIdentifier)!!

    mockMvc
        .perform(delete(latestVersionOf(PICTURE_BY_PROJECT_ID_ENDPOINT), projectIdentifier))
        .andExpect(status().isNoContent)
        .andDo(
            document(
                "projects/document-delete-project-picture",
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID)
                        .description("ID of the project whose picture should be deleted."))))

    projectEventStoreUtils
        .verifyContainsAndGet(ProjectPictureEventAvro::class.java, DELETED, true)
        .getAggregate()
        .also {
          validateDeletedAggregateAuditInfoAndAggregateIdentifier(it, projectPicture, userCsm2)
        }
  }

  @Test
  fun verifyDeleteProjectPictureWithProjectPictureId() {
    eventStreamGenerator.submitProjectPicture()
    val projectPictureIdentifier = getIdentifier("projectPicture")

    val projectPicture =
        repositories.projectPictureRepository.findOneByProjectIdentifier(projectIdentifier)!!

    mockMvc
        .perform(
            delete(
                latestVersionOf(PICTURE_BY_PROJECT_ID_AND_PICTURE_ID_ENDPOINT),
                projectIdentifier,
                projectPictureIdentifier))
        .andExpect(status().isNoContent)

    projectEventStoreUtils
        .verifyContainsAndGet(ProjectPictureEventAvro::class.java, DELETED, true)
        .getAggregate()
        .also {
          validateDeletedAggregateAuditInfoAndAggregateIdentifier(it, projectPicture, userCsm2)
        }
  }

  companion object {

    private val PROJECT_PICTURE_FIELDS =
        responseFields(
            fieldWithPath("id").description("ID of the project picture"),
            fieldWithPath("width").description("Width of the image in pixels"),
            fieldWithPath("height").description("Height of the image in pixels"),
            fieldWithPath("fileSize").description("Size of image in bytes"),
            fieldWithPath("createdDate").description("Date of creation"),
            fieldWithPath("lastModifiedDate").description("Date of last modification"),
            fieldWithPath("createdBy.displayName").description("Name of creator"),
            fieldWithPath("createdBy.id").description("Id of creator"),
            fieldWithPath("lastModifiedBy.displayName").description("Name of last modifier"),
            fieldWithPath("lastModifiedBy.id").description("Id of last modifier"),
            fieldWithPath("version").description("Version of projectpicture"),
            fieldWithPath("projectReference.displayName")
                .description("Name of the associated project"),
            fieldWithPath("projectReference.id").description("ID of the associated project"),
            subsectionWithPath("_links").ignored())

    private val PICTURE_DATA_LINKS =
        links(
            linkWithRel("small").description("Link to small resolution"),
            linkWithRel("full").description("Link to full resolution"),
            linkWithRel("delete").description("Link to delete the picture"))
  }
}
