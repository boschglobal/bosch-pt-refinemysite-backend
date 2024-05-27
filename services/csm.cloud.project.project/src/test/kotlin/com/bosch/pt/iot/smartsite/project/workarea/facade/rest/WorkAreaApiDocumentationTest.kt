/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.WORKAREA
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.WORKAREALIST
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.workarea.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro.ITEMADDED
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro.REORDERED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ABSTRACT_RESOURCE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER_DESCRIPTOR
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.IF_MATCH_HEADER_DESCRIPTOR
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.LOCATION_HEADER_DESCRIPTOR
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.PROJECT_REFERENCE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.hasIdentifierAndVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.hasReference
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isCreatedBy
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isLastModifiedBy
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaListId
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.request.CreateWorkAreaResource
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.request.UpdateWorkAreaListResource
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.request.UpdateWorkAreaResource
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.WorkAreaResource.Companion.LINK_CREATE
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.WorkAreaResource.Companion.LINK_DELETE
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.WorkAreaResource.Companion.LINK_PROJECT
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.WorkAreaResource.Companion.LINK_REORDER
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.WorkAreaResource.Companion.LINK_UPDATE
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkAreaList
import java.util.UUID
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.matchesRegex
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.JsonFieldType.NUMBER
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
class WorkAreaApiDocumentationTest : AbstractApiDocumentationTestV2() {

  private val creatorUser by lazy { repositories.findUser(getIdentifier("userCsm1"))!! }
  private val testUser by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }
  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }
  private val workArea by lazy {
    repositories.findWorkArea(getIdentifier("workArea").asWorkAreaId())!!
  }
  private val workAreaTwo by lazy {
    repositories.findWorkArea(getIdentifier("workAreaTwo").asWorkAreaId())!!
  }
  private val workAreaList by lazy {
    repositories.findWorkAreaList(getIdentifier("workAreaList").asWorkAreaListId())!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator
        .setupDatasetTestData()
        .submitWorkArea(asReference = "workAreaTwo")
        .submitWorkAreaList(eventType = ITEMADDED) {
          it.workAreas = listOf(getByReference("workArea"), getByReference("workAreaTwo"))
        }

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document create work area`() {
    val resource = CreateWorkAreaResource(project.identifier, "New work area name", 1)

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf("/projects/workareas")), resource, workAreaList.version))
        .andExpectAll(
            status().isCreated,
            header().string(ETAG_HEADER, "\"2\""),
            header()
                .string(
                    LOCATION,
                    matchesRegex(
                        ".*${latestVersionOf("/projects/${project.identifier}/workareas$")}")),
            jsonPath("$.workAreas").exists(),
            jsonPath("$.workAreas.length()").value(3),
            jsonPath("$.version").value("2"),
            jsonPath("$._links.create.href").exists(),
            jsonPath("$._links.reorder.href").exists(),
            jsonPath("$.workAreas[0].id").exists(),
            jsonPath("$.workAreas[0].version").value("0"),
            jsonPath("$.workAreas[0].createdBy.id").value(testUser.identifier.toString()),
            jsonPath("$.workAreas[0].createdBy.displayName").value(testUser.getDisplayName()),
            jsonPath("$.workAreas[0].createdDate").exists(),
            jsonPath("$.workAreas[0].lastModifiedBy.id").value(testUser.identifier.toString()),
            jsonPath("$.workAreas[0].lastModifiedBy.displayName").value(testUser.getDisplayName()),
            jsonPath("$.workAreas[0].lastModifiedBy").exists(),
            jsonPath("$.workAreas[0].project.id").value(project.identifier.toString()),
            jsonPath("$.workAreas[0].project.displayName").value(project.getDisplayName()),
            jsonPath("$.workAreas[0].name").value("New work area name"),
            jsonPath("$.workAreas[0]._links.project.href").exists(),
            jsonPath("$.workAreas[0]._links.update.href").exists(),
            jsonPath("$.workAreas[0]._links.delete.href").exists(),
            jsonPath("$.workAreas[1].id").value(getIdentifier("workArea").toString()),
            jsonPath("$.workAreas[2].id").value(getIdentifier("workAreaTwo").toString()))
        .andDo(
            document(
                "project-workareas/document-create-project-workarea",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(IF_MATCH_HEADER_DESCRIPTOR),
                requestFields(WORKAREA_CREATE_REQUEST_FIELD_DESCRIPTORS),
                responseHeaders(ETAG_HEADER_DESCRIPTOR, LOCATION_HEADER_DESCRIPTOR),
                WORKAREALIST_RESPONSE_FIELDS,
                links(WORKAREALIST_LINK_DESCRIPTORS)))

    projectEventStoreUtils.verifyContainsInSequence(
        listOf(WorkAreaEventAvro::class.java, WorkAreaListEventAvro::class.java))

    projectEventStoreUtils
        .verifyContainsAndGet(WorkAreaEventAvro::class.java, CREATED, 1, false)
        .also { verifyCreatedAggregate(it[0].aggregate, resource) }

    val updateWorkAreaList =
        repositories.findWorkAreaList(getIdentifier("workAreaList").asWorkAreaListId())!!
    projectEventStoreUtils
        .verifyContainsAndGet(WorkAreaListEventAvro::class.java, ITEMADDED, 1, false)
        .also {
          verifyUpdateAggregate(it[0].aggregate, updateWorkAreaList, project.identifier.toUuid())
        }
  }

  @Test
  fun `verify and document create work area with identifier`() {
    val identifier = randomUUID()
    val resource = CreateWorkAreaResource(project.identifier, "New work area name", 1)

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf("/projects/workareas/{workAreaId}"), identifier),
                resource,
                workAreaList.version))
        .andExpectAll(
            status().isCreated,
            header().string(ETAG_HEADER, "\"2\""),
            header()
                .string(
                    LOCATION,
                    matchesRegex(
                        ".*${latestVersionOf("/projects/${project.identifier}/workareas$")}")),
            jsonPath("$.workAreas").exists(),
            jsonPath("$.workAreas.length()").value(3),
            jsonPath("$.version").value("2"),
            jsonPath("$._links.create.href").exists(),
            jsonPath("$._links.reorder.href").exists(),
            jsonPath("$.workAreas[0].id").value(identifier.toString()),
            jsonPath("$.workAreas[0].version").value("0"),
            jsonPath("$.workAreas[0].createdBy.id").value(testUser.identifier.toString()),
            jsonPath("$.workAreas[0].createdBy.displayName").value(testUser.getDisplayName()),
            jsonPath("$.workAreas[0].createdDate").exists(),
            jsonPath("$.workAreas[0].lastModifiedBy.id").value(testUser.identifier.toString()),
            jsonPath("$.workAreas[0].lastModifiedBy.displayName").value(testUser.getDisplayName()),
            jsonPath("$.workAreas[0].lastModifiedBy").exists(),
            jsonPath("$.workAreas[0].project.id").value(project.identifier.toString()),
            jsonPath("$.workAreas[0].project.displayName").value(project.getDisplayName()),
            jsonPath("$.workAreas[0].name").value("New work area name"),
            jsonPath("$.workAreas[0]._links.project.href").exists(),
            jsonPath("$.workAreas[0]._links.update.href").exists(),
            jsonPath("$.workAreas[0]._links.delete.href").exists(),
            jsonPath("$.workAreas[1].id").value(getIdentifier("workArea").toString()),
            jsonPath("$.workAreas[2].id").value(getIdentifier("workAreaTwo").toString()))
        .andDo(
            document(
                "project-workareas/document-create-project-workarea-with-identifier",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(IF_MATCH_HEADER_DESCRIPTOR),
                pathParameters(WORKAREA_PATH_PARAMETER_DESCRIPTOR),
                requestFields(WORKAREA_CREATE_REQUEST_FIELD_DESCRIPTORS),
                responseHeaders(ETAG_HEADER_DESCRIPTOR, LOCATION_HEADER_DESCRIPTOR),
                WORKAREALIST_RESPONSE_FIELDS,
                links(WORKAREALIST_LINK_DESCRIPTORS)))

    projectEventStoreUtils.verifyContainsInSequence(
        listOf(WorkAreaEventAvro::class.java, WorkAreaListEventAvro::class.java))

    projectEventStoreUtils
        .verifyContainsAndGet(WorkAreaEventAvro::class.java, CREATED, 1, false)
        .also { verifyCreatedAggregate(it[0].aggregate, resource) }

    val updatedWorkAreaList =
        repositories.findWorkAreaList(getIdentifier("workAreaList").asWorkAreaListId())!!
    projectEventStoreUtils
        .verifyContainsAndGet(WorkAreaListEventAvro::class.java, ITEMADDED, 1, false)
        .also {
          verifyUpdateAggregate(it[0].aggregate, updatedWorkAreaList, project.identifier.toUuid())
        }
  }

  @Test
  fun `verify and document find all work areas by project`() {
    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf("/projects/{projectId}/workareas"), project.identifier)))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"1\""),
            jsonPath("$.workAreas").exists(),
            jsonPath("$.workAreas.length()").value(2),
            jsonPath("$.version").value("1"),
            jsonPath("$._links.create.href").exists(),
            jsonPath("$._links.reorder.href").exists(),
            jsonPath("$.workAreas[0].id").value(getIdentifier("workArea").toString()),
            jsonPath("$.workAreas[1].id").value(getIdentifier("workAreaTwo").toString()))
        .andDo(
            document(
                "project-workareas/document-get-project-workareas",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(PROJECT_PATH_PARAMETER_DESCRIPTOR),
                responseHeaders(ETAG_HEADER_DESCRIPTOR),
                WORKAREALIST_RESPONSE_FIELDS,
                links(WORKAREALIST_LINK_DESCRIPTORS)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document find one work area`() {
    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf("/projects/workareas/{workAreaId}"), workArea.identifier)))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"0\""),
            *hasIdentifierAndVersion(workArea.identifier.toUuid()),
            *isCreatedBy(creatorUser),
            *isLastModifiedBy(creatorUser),
            *hasReference(project),
            jsonPath("$.name").value(workArea.name),
            jsonPath("$._links.project.href").exists(),
            jsonPath("$._links.update.href").exists(),
            jsonPath("$._links.delete.href").exists())
        .andDo(
            document(
                "project-workareas/document-get-project-workarea",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(WORKAREA_PATH_PARAMETER_DESCRIPTOR),
                responseHeaders(ETAG_HEADER_DESCRIPTOR),
                responseFields(WORKAREA_RESPONSE_FIELDS),
                links(WORKAREA_LINK_DESCRIPTORS)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document update work area`() {
    val resource = UpdateWorkAreaResource("Update work area name")

    mockMvc
        .perform(
            requestBuilder(
                put(latestVersionOf("/projects/workareas/{workAreaId}"), workArea.identifier),
                resource,
                workArea.version))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"1\""),
            *hasIdentifierAndVersion(workArea.identifier.toUuid(), 1),
            *isCreatedBy(creatorUser),
            *isLastModifiedBy(testUser),
            *hasReference(project),
            jsonPath("$.name").value("Update work area name"),
            jsonPath("$._links.project.href").exists(),
            jsonPath("$._links.update.href").exists(),
            jsonPath("$._links.delete.href").exists())
        .andDo(
            document(
                "project-workareas/document-update-project-workarea",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(IF_MATCH_HEADER_DESCRIPTOR),
                pathParameters(WORKAREA_PATH_PARAMETER_DESCRIPTOR),
                requestFields(WORKAREA_UPDATE_REQUEST_FIELD_DESCRIPTORS),
                responseHeaders(ETAG_HEADER_DESCRIPTOR),
                responseFields(WORKAREA_RESPONSE_FIELDS),
                links(WORKAREA_LINK_DESCRIPTORS)))

    val updatedWorkArea = repositories.findWorkArea(getIdentifier("workArea").asWorkAreaId())!!
    projectEventStoreUtils
        .verifyContainsAndGet(WorkAreaEventAvro::class.java, UPDATED, 1, true)
        .also { verifyUpdateAggregate(it[0].aggregate, updatedWorkArea) }
  }

  @Test
  fun `verify and document update of a position in the work area list`() {
    val resource = UpdateWorkAreaListResource(workArea.identifier, 2)

    mockMvc
        .perform(
            requestBuilder(
                put(latestVersionOf("/projects/workareas")), resource, workAreaList.version))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"2\""),
            jsonPath("$.workAreas").exists(),
            jsonPath("$.workAreas.length()").value(2),
            jsonPath("$.version").value("2"),
            jsonPath("$._links.create.href").exists(),
            jsonPath("$._links.reorder.href").exists(),
            jsonPath("$.workAreas[0].id").value(getIdentifier("workAreaTwo").toString()),
            jsonPath("$.workAreas[1].id").value(getIdentifier("workArea").toString()))
        .andDo(
            document(
                "project-workareas/document-update-project-workarealist",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(IF_MATCH_HEADER_DESCRIPTOR),
                requestFields(WORKAREALIST_UPDATE_REQUEST_FIELD_DESCRIPTORS),
                responseHeaders(ETAG_HEADER_DESCRIPTOR),
                WORKAREALIST_RESPONSE_FIELDS,
                links(WORKAREALIST_LINK_DESCRIPTORS)))

    val updatedWorkAreaList =
        repositories.findWorkAreaList(getIdentifier("workAreaList").asWorkAreaListId())!!
    projectEventStoreUtils
        .verifyContainsAndGet(WorkAreaListEventAvro::class.java, REORDERED, 1, true)
        .also {
          verifyUpdateAggregate(it[0].aggregate, updatedWorkAreaList, project.identifier.toUuid())
        }
  }

  @Test
  fun `verify and document delete work area`() {
    mockMvc
        .perform(
            requestBuilder(
                request =
                    delete(
                        latestVersionOf("/projects/workareas/{workAreaId}"),
                        getIdentifier("workAreaTwo")),
                version = workAreaTwo.version))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"2\""),
            jsonPath("$.workAreas").exists(),
            jsonPath("$.workAreas.length()").value(1),
            jsonPath("$.version").value("2"),
            jsonPath("$._links.create.href").exists(),
            jsonPath("$._links.reorder.href").exists(),
            jsonPath("$.workAreas[0].id").value(getIdentifier("workArea").toString()))
        .andDo(
            document(
                "project-workareas/document-delete-project-workarea",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(IF_MATCH_HEADER_DESCRIPTOR),
                pathParameters(WORKAREA_PATH_PARAMETER_DESCRIPTOR),
                responseHeaders(ETAG_HEADER_DESCRIPTOR),
                WORKAREALIST_RESPONSE_FIELDS,
                links(WORKAREALIST_LINK_DESCRIPTORS)))

    projectEventStoreUtils.verifyContainsInSequence(
        listOf(WorkAreaListEventAvro::class.java, WorkAreaEventAvro::class.java))

    val updatedWorkAreaList =
        repositories.findWorkAreaList(getIdentifier("workAreaList").asWorkAreaListId())!!
    projectEventStoreUtils
        .verifyContainsAndGet(WorkAreaListEventAvro::class.java, ITEMADDED, 1, false)
        .also {
          verifyUpdateAggregate(it[0].aggregate, updatedWorkAreaList, project.identifier.toUuid())
        }

    projectEventStoreUtils
        .verifyContainsAndGet(
            WorkAreaEventAvro::class.java, WorkAreaEventEnumAvro.DELETED, 1, false)
        .also { verifyDeletedAggregate(it[0].aggregate, workAreaTwo) }
  }

  private fun verifyCreatedAggregate(
      aggregate: WorkAreaAggregateAvro,
      resource: CreateWorkAreaResource
  ) =
      with(aggregate) {
        validateCreatedAggregateAuditInfoAndAggregateIdentifier(this, WORKAREA, testUser)
        assertThat(name).isEqualTo(resource.name)
      }

  private fun verifyUpdateAggregate(aggregate: WorkAreaAggregateAvro, workArea: WorkArea) =
      with(aggregate) {
        validateUpdatedAggregateAuditInfoAndAggregateIdentifier(this, workArea, WORKAREA)
        assertThat(name).isEqualTo(workArea.name)
      }

  private fun verifyDeletedAggregate(aggregate: WorkAreaAggregateAvro, workArea: WorkArea) =
      with(aggregate) {
        validateDeletedAggregateAuditInfoAndAggregateIdentifier(this, workArea, WORKAREA, testUser)
        assertThat(name).isEqualTo(workArea.name)
      }

  private fun verifyUpdateAggregate(
      aggregate: WorkAreaListAggregateAvro,
      workAreaList: WorkAreaList,
      projectIdentifier: UUID
  ) =
      with(aggregate) {
        validateUpdatedAggregateAuditInfoAndAggregateIdentifier(this, workAreaList, WORKAREALIST)
        assertThat(getProjectIdentifier()).isEqualTo(projectIdentifier)
        assertThat(workAreas)
            .extracting<UUID> { it.identifier.toUUID() }
            .containsExactlyElementsOf(workAreaList.workAreas.map { it.identifier.identifier })
      }

  companion object {

    private val PROJECT_PATH_PARAMETER_DESCRIPTOR =
        listOf(parameterWithName("projectId").description("ID of the project"))

    private val WORKAREA_PATH_PARAMETER_DESCRIPTOR =
        listOf(parameterWithName("workAreaId").description("ID of the work area to be created"))

    private val WORKAREA_CREATE_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            ConstrainedFields(CreateWorkAreaResource::class.java)
                .withPath("projectId")
                .description("The identifier of the project")
                .type(STRING),
            ConstrainedFields(CreateWorkAreaResource::class.java)
                .withPath("name")
                .description("The name of the work area")
                .type(STRING),
            ConstrainedFields(CreateWorkAreaResource::class.java)
                .withPath("position")
                .description("The position of the work area")
                .type(NUMBER)
                .optional())

    private val WORKAREA_UPDATE_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            ConstrainedFields(UpdateWorkAreaResource::class.java)
                .withPath("name")
                .description("The name of the work area")
                .type(STRING))

    private val WORKAREALIST_UPDATE_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            ConstrainedFields(UpdateWorkAreaListResource::class.java)
                .withPath("workAreaId")
                .description("The identifier of the work area")
                .type(STRING),
            ConstrainedFields(UpdateWorkAreaListResource::class.java)
                .withPath("position")
                .description("The position of the work area in the list")
                .type(NUMBER))

    private val WORKAREA_RESPONSE_FIELDS =
        listOf(
            *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
            *PROJECT_REFERENCE_FIELD_DESCRIPTORS,
            fieldWithPath("name").description("The name of the work area").type(STRING),
            fieldWithPath("parent")
                .description("Optional identifier of the parent working area")
                .type(STRING)
                .optional(),
            subsectionWithPath("_links").ignored())

    private val WORKAREALIST_RESPONSE_FIELDS =
        responseFields(
                fieldWithPath("workAreas[]")
                    .description("List of work areas")
                    .type(JsonFieldType.ARRAY),
                fieldWithPath("version")
                    .description("The version of the work area list")
                    .type(NUMBER),
                subsectionWithPath("_links").ignored())
            .andWithPrefix("workAreas[].", WORKAREA_RESPONSE_FIELDS)

    private val WORKAREA_LINK_DESCRIPTORS =
        listOf(
            linkWithRel(LINK_PROJECT).description("Link to project of the work area resource"),
            linkWithRel(LINK_UPDATE).description("Link to update the work area resource"),
            linkWithRel(LINK_DELETE).description("Link to delete the work area resource"))

    private val WORKAREALIST_LINK_DESCRIPTORS =
        listOf(
            linkWithRel(LINK_CREATE).description("Link to create work area resources"),
            linkWithRel(LINK_REORDER).description("Link to reorder the work area list resource"))
  }
}
