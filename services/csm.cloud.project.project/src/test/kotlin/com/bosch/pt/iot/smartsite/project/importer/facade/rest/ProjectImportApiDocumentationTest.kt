/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.facade.rest

import com.bosch.pt.csm.cloud.common.blob.model.Blob
import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.job.messages.EnqueueJobCommandAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.common.kafka.messaging.impl.CommandSendingServiceTestDouble
import com.bosch.pt.iot.smartsite.common.test.FileSource
import com.bosch.pt.iot.smartsite.job.integration.JobJsonSerializer
import com.bosch.pt.iot.smartsite.project.importer.api.ProjectImportCommand
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumnType
import com.bosch.pt.iot.smartsite.project.importer.facade.job.dto.ProjectImportJobContext
import com.bosch.pt.iot.smartsite.project.importer.facade.job.dto.ProjectImportJobType.PROJECT_IMPORT
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.ProjectImportController.Companion.ANALYZE_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.ProjectImportController.Companion.IMPORT_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.ProjectImportController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.ProjectImportController.Companion.UPLOAD_BY_PROJECT_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.request.ProjectImportAnalyzeCraftColumnResource
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.request.ProjectImportAnalyzeResource
import com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.response.ProjectImportAnalysisResource.Companion.LINK_IMPORT
import com.bosch.pt.iot.smartsite.project.importer.repository.ImportBlobStorageRepository
import com.bosch.pt.iot.smartsite.project.importer.repository.MalwareScanResult.SAFE
import com.bosch.pt.iot.smartsite.project.importer.submitProjectImportFeatureToggle
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import io.mockk.every
import java.util.Locale
import java.util.UUID.randomUUID
import net.sf.mpxj.FieldType
import net.sf.mpxj.TaskField
import org.apache.avro.specific.SpecificRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.http.HttpHeaders.IF_MATCH
import org.springframework.mock.web.MockMultipartFile
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.BOOLEAN
import org.springframework.restdocs.payload.JsonFieldType.OBJECT
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class ProjectImportApiDocumentationTest : AbstractApiDocumentationTestV2() {

  @Autowired private lateinit var blobStorageRepository: ImportBlobStorageRepository

  @Autowired private lateinit var projectRepository: ProjectRepository

  @Autowired private lateinit var commandSendingService: CommandSendingServiceTestDouble

  @Autowired private lateinit var jobJsonSerializer: JobJsonSerializer

  @BeforeEach
  fun setup() {
    eventStreamGenerator
        .submitSystemUserAndActivate()
        .setupDatasetTestData()
        .submitProjectImportFeatureToggle()
        .setUserContext("userCsm1")

    setAuthentication(eventStreamGenerator.getIdentifier("userCsm1"))
    commandSendingService.clearRecords()

    every { blobStorageRepository.getMalwareScanResultBlocking(any()) } returns SAFE
  }

  @FileSource(container = "project-import-testdata", files = ["task-without-craft.mpp"])
  @ParameterizedTest
  fun `verify and document upload ms project file`(file: Resource) {
    every { blobStorageRepository.read(any()) } returns file.inputStream

    eventStreamGenerator.submitProject(asReference = "p2").submitParticipantG3(
        asReference = "csmP2") {
          it.project = getByReference("p2")
          it.user = getByReference("userCsm1")
          it.role = ParticipantRoleEnumAvro.CSM
        }

    val projectIdentifier = getIdentifier("p2")

    val msProjectFile =
        MockMultipartFile("file", "import.mpp", MEDIA_TYPE_MS_PROJECT, file.inputStream)

    projectEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                multipart(latestVersionOf(UPLOAD_BY_PROJECT_ID_ENDPOINT), projectIdentifier)
                    .file(msProjectFile)))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.id").exists(),
            jsonPath("$.version").value(0),
            jsonPath("$.columns.length()").value(1))
        .andDo(
            document(
                "project-import/document-project-import-upload",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID)
                        .description(PATH_VARIABLE_PROJECT_ID_DESCRIPTION)),
                responseFields(COLUMNS_RESOURCE_RESPONSE_FIELD_DESCRIPTORS)))

    projectEventStoreUtils.verifyEmpty()
  }

  @FileSource(
      container = "project-import-testdata",
      files = ["relation-3-non-finish-to-start-relations.mpp"])
  @ParameterizedTest
  fun `verify and document analyze ms project file`(file: Resource) {
    every { blobStorageRepository.read(any()) } returns file.inputStream

    eventStreamGenerator.submitProject(asReference = "p3").submitParticipantG3(
        asReference = "csmP3") {
          it.project = getByReference("p3")
          it.user = getByReference("userCsm1")
          it.role = ParticipantRoleEnumAvro.CSM
        }
    val projectIdentifier = getIdentifier("p3").asProjectId()
    uploadFile(projectIdentifier, file)

    every { blobStorageRepository.find(any()) } returns
        Blob(
            randomUUID().toString(),
            file.inputStream.readAllBytes(),
            BlobMetadata.fromMap(emptyMap()),
            MEDIA_TYPE_MS_PROJECT)

    val analyzeResource =
        ProjectImportAnalyzeResource(
            false,
            ProjectImportAnalyzeCraftColumnResource(
                ImportColumnType.TASK_FIELD, (TaskField.NAME as FieldType).name()),
            null)

    projectEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(ANALYZE_BY_PROJECT_ID_ENDPOINT), projectIdentifier),
                analyzeResource,
                0L))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.id").exists(),
            jsonPath("$.version").value(1),
            jsonPath("$.validationResults.length()").value(1),
            jsonPath("$.validationResults[0].type").value("INFO"),
            jsonPath("$.validationResults[0].summary")
                .value("Dependency type is not supported and will be skipped."),
            jsonPath("$.validationResults[0].elements.length()").value(3),
            jsonPath("$.validationResults[0].elements[0]")
                .value("Finish-to-Finish: “task02“ → “task03“"),
            jsonPath("$.statistics.crafts").value(6),
            jsonPath("$.statistics.milestones").value(0),
            jsonPath("$.statistics.tasks").value(5),
            jsonPath("$.statistics.relations").value(0),
            jsonPath("$.statistics.workAreas").value(0))
        .andDo(
            document(
                "project-import/document-project-import-analyze",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID)
                        .description(PATH_VARIABLE_PROJECT_ID_DESCRIPTION)),
                requestHeaders(headerWithName(IF_MATCH).description("If-Match header with ETag")),
                requestFields(ANALYZE_RESOURCE_REQUEST_FIELDS),
                responseFields(ANALYSIS_RESOURCE_RESPONSE_FIELD_DESCRIPTORS),
                links(IMPORT_LINK_DESCRIPTORS)))

    projectEventStoreUtils.verifyEmpty()
  }

  @FileSource(container = "project-import-testdata", files = ["task-without-craft.mpp"])
  @ParameterizedTest
  fun `verify and document import ms project file`(file: Resource) {
    every { blobStorageRepository.read(any()) } returns file.inputStream

    eventStreamGenerator.submitProject(asReference = "p4").submitParticipantG3(
        asReference = "csmP4") {
          it.project = getByReference("p4")
          it.user = getByReference("userCsm1")
          it.role = ParticipantRoleEnumAvro.CSM
        }
    val projectIdentifier = getIdentifier("p4").asProjectId()
    uploadFile(projectIdentifier, file)

    val project = projectRepository.findOneByIdentifier(projectIdentifier)

    every { blobStorageRepository.find(any()) } returns
        Blob(
            randomUUID().toString(),
            file.inputStream.readAllBytes(),
            BlobMetadata.fromMap(mapOf("filename" to file.file.name)),
            MEDIA_TYPE_MS_PROJECT)

    projectEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(IMPORT_BY_PROJECT_ID_ENDPOINT), projectIdentifier), null, 0L))
        .andExpectAll(
            status().isAccepted, content().contentType(HAL_JSON_VALUE), jsonPath("$.id").exists())
        .andDo(
            document(
                "project-import/document-project-import",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName(PATH_VARIABLE_PROJECT_ID)
                        .description(PATH_VARIABLE_PROJECT_ID_DESCRIPTION)),
                requestHeaders(headerWithName(IF_MATCH).description("If-Match header with ETag")),
                responseFields(IMPORT_RESOURCE_RESPONSE_FIELD_DESCRIPTORS)))

    projectEventStoreUtils.verifyEmpty()

    assertThat(commandSendingService.capturedRecords).hasSize(1)
    commandSendingService.capturedRecords.first().value.apply {
      assertJobTypeAndIdentifier(this)
      assertContext(this, project!!, file.file.name)
      assertImportCommand(this, projectIdentifier)
    }
  }

  private fun assertJobTypeAndIdentifier(message: SpecificRecord) {
    assertThat(message).isInstanceOf(EnqueueJobCommandAvro::class.java)
    (message as EnqueueJobCommandAvro).apply {
      assertThat(jobType).isEqualTo(PROJECT_IMPORT.name)
      assertThat(this.aggregateIdentifier.type).isEqualTo("JOB")
    }
  }

  private fun assertContext(message: SpecificRecord, project: Project, originalFilename: String) {
    (message as EnqueueJobCommandAvro).jsonSerializedContext.apply {
      ProjectImportJobContext(ResourceReference.from(project), originalFilename)
          .let { jobJsonSerializer.serialize(it) }
          .apply {
            assertThat(getType()).isEqualTo(this.type)
            assertThat(getJson()).isEqualTo(this.json)
          }
    }
  }

  private fun assertImportCommand(message: SpecificRecord, projectIdentifier: ProjectId) {
    (message as EnqueueJobCommandAvro).jsonSerializedCommand.apply {
      ProjectImportCommand(Locale.UK, projectIdentifier)
          .let { jobJsonSerializer.serialize(it) }
          .apply {
            assertThat(getType()).isEqualTo(this.type)
            assertThat(getJson()).isEqualTo(this.json)
          }
    }
  }

  private fun uploadFile(projectIdentifier: ProjectId, file: Resource) {
    val msProjectFile =
        MockMultipartFile("file", "import.mpp", MEDIA_TYPE_MS_PROJECT, file.inputStream)

    mockMvc.perform(
        requestBuilder(
            multipart(latestVersionOf(UPLOAD_BY_PROJECT_ID_ENDPOINT), projectIdentifier)
                .file(msProjectFile)))
  }

  companion object {

    private const val PATH_VARIABLE_PROJECT_ID_DESCRIPTION =
        "ID of the project where to import data"

    private const val LINK_IMPORT_DESCRIPTION =
        "Link to indicate that the data to import is valid and the import can be started"

    private const val MEDIA_TYPE_MS_PROJECT = "application/x-ms-project"

    private val ANALYZE_RESOURCE_REQUEST_CONSTRAINED_FIELD =
        ConstrainedFields(ProjectImportAnalyzeResource::class.java)

    private val ANALYZE_RESOURCE_REQUEST_FIELDS =
        listOf(
            ANALYZE_RESOURCE_REQUEST_CONSTRAINED_FIELD.withPath("readWorkAreasHierarchically")
                .description("Read working areas from file hierarchically")
                .type(BOOLEAN)
                .optional(),
            ANALYZE_RESOURCE_REQUEST_CONSTRAINED_FIELD.withPath("craftColumn")
                .description("Optional column of the file in which the craft names can be found")
                .type(OBJECT)
                .optional(),
            ANALYZE_RESOURCE_REQUEST_CONSTRAINED_FIELD.withPath("craftColumn.columnType")
                .description("Technical type of the craft column from upload step")
                .type(STRING),
            ANALYZE_RESOURCE_REQUEST_CONSTRAINED_FIELD.withPath("craftColumn.fieldType")
                .description("Technical type of the craft column from upload step")
                .type(STRING),
            ANALYZE_RESOURCE_REQUEST_CONSTRAINED_FIELD.withPath("workAreaColumn")
                .description(
                    "Optional column of the file in which the working areas names can be found")
                .type(OBJECT)
                .optional(),
            ANALYZE_RESOURCE_REQUEST_CONSTRAINED_FIELD.withPath("workAreaColumn.columnType")
                .description("Technical type of the working area column from upload step")
                .type(STRING),
            ANALYZE_RESOURCE_REQUEST_CONSTRAINED_FIELD.withPath("workAreaColumn.fieldType")
                .description("Technical type of the working area column from upload step")
                .type(STRING))

    private val ANALYSIS_RESOURCE_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            fieldWithPath("id").description("ID of the resource"),
            fieldWithPath("version").description("Version of the resource"),
            subsectionWithPath("validationResults")
                .description(
                    "List of elements that will be auto-corrected and errors detected " +
                        "in the data of the file to import")
                .type(ARRAY),
            subsectionWithPath("statistics")
                .description("Statistics about detected numbers of elements to import")
                .type(OBJECT),
            subsectionWithPath("_links").ignored())

    private val COLUMNS_RESOURCE_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            fieldWithPath("id").description("ID of the resource"),
            fieldWithPath("version").description("Version of the resource"),
            fieldWithPath("columns").description("Detected columns from file").type(ARRAY),
            fieldWithPath("columns[].name").description("The display name"),
            fieldWithPath("columns[].columnType")
                .description("Technical type to be sent back in the analyze step"),
            fieldWithPath("columns[].fieldType")
                .description("Technical type to be sent back in the analyze step"),
            subsectionWithPath("_links").ignored())

    private val IMPORT_RESOURCE_RESPONSE_FIELD_DESCRIPTORS =
        listOf(fieldWithPath("id").description("The ID of the async import job").type(STRING))

    private val IMPORT_LINK_DESCRIPTORS =
        listOf(linkWithRel(LINK_IMPORT).description(LINK_IMPORT_DESCRIPTION))
  }
}
