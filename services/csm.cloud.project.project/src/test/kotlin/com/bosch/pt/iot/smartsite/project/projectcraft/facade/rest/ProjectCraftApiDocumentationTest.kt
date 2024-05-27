/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro.ITEMADDED
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro.ITEMREMOVED
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro.REORDERED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftList
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
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftListId
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.request.ReorderProjectCraftResource
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.request.SaveProjectCraftResource
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.ProjectCraftListResource.Companion.LINK_CREATE
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.ProjectCraftResource.Companion.LINK_DELETE
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.ProjectCraftResource.Companion.LINK_UPDATE
import com.bosch.pt.iot.smartsite.project.projectcraft.util.ProjectCraftTestUtil.verifyCreatedAggregate
import com.bosch.pt.iot.smartsite.project.projectcraft.util.ProjectCraftTestUtil.verifyDeletedAggregate
import com.bosch.pt.iot.smartsite.project.projectcraft.util.ProjectCraftTestUtil.verifyUpdatedAggregate
import com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response.WorkAreaResource.Companion.LINK_REORDER
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
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
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
class ProjectCraftApiDocumentationTest : AbstractApiDocumentationTestV2() {

  private val creatorUser by lazy { repositories.findUser(getIdentifier("userCsm1"))!! }
  private val testUser by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }
  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }
  private val projectCraft by lazy {
    repositories.findProjectCraft(getIdentifier("projectCraft").asProjectCraftId())!!
  }
  private val projectCraftList by lazy {
    repositories.findProjectCraftList(getIdentifier("projectCraftList").asProjectCraftListId())!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document create project craft`() {
    val resource = SaveProjectCraftResource("Craft name", "#FFFFFF", 1)

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf("/projects/{projectId}/crafts"), project.identifier),
                resource,
                projectCraftList.version))
        .andExpectAll(
            status().isCreated,
            header().string(ETAG_HEADER, "\"1\""),
            header()
                .string(
                    LOCATION,
                    matchesRegex(
                        ".*${latestVersionOf("/projects/${project.identifier}/crafts$")}")),
            jsonPath("$.projectCrafts").exists(),
            jsonPath("$.projectCrafts.length()").value(2),
            jsonPath("$.version").value("1"),
            jsonPath("$._links.create.href").exists(),
            jsonPath("$._links.reorder.href").exists(),
            jsonPath("$.projectCrafts[0].id").exists(),
            jsonPath("$.projectCrafts[0].version").value("0"),
            jsonPath("$.projectCrafts[0].createdBy.id").value(testUser.identifier.toString()),
            jsonPath("$.projectCrafts[0].createdBy.displayName").value(testUser.getDisplayName()),
            jsonPath("$.projectCrafts[0].createdDate").exists(),
            jsonPath("$.projectCrafts[0].lastModifiedBy.id").value(testUser.identifier.toString()),
            jsonPath("$.projectCrafts[0].lastModifiedBy.displayName")
                .value(testUser.getDisplayName()),
            jsonPath("$.projectCrafts[0].lastModifiedBy").exists(),
            jsonPath("$.projectCrafts[0].project.id").value(project.identifier.toString()),
            jsonPath("$.projectCrafts[0].project.displayName").value(project.getDisplayName()),
            jsonPath("$.projectCrafts[0].name").value("Craft name"),
            jsonPath("$.projectCrafts[0].color").value("#FFFFFF"),
            jsonPath("$.projectCrafts[0]._links.update.href").exists(),
            jsonPath("$.projectCrafts[0]._links.delete.href").exists())
        .andDo(
            document(
                "project-crafts/document-create-project-craft",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(IF_MATCH_HEADER_DESCRIPTOR),
                pathParameters(PROJECT_PATH_PARAMETER_DESCRIPTOR),
                requestFields(SAVE_PROJECTCRAFT_REQUEST_FIELD_DESCRIPTORS),
                responseHeaders(ETAG_HEADER_DESCRIPTOR, LOCATION_HEADER_DESCRIPTOR),
                PROJECTCRAFT_RESPONSE_FIELDS,
                links(PROJECTCRAFTLIST_LINK_DESCRIPTORS)))

    projectEventStoreUtils
        .verifyContainsAndGet(ProjectCraftEventG2Avro::class.java, CREATED, 1, false)
        .also { verifyCreatedAggregate(it[0].aggregate, resource, project.identifier, testUser) }

    val updateProjectCraftList = repositories.findProjectCraftList(projectCraftList.identifier)!!
    projectEventStoreUtils
        .verifyContainsAndGet(ProjectCraftListEventAvro::class.java, ITEMADDED, 1, false)
        .also {
          verifyUpdatedAggregate(it[0].aggregate, project.identifier, updateProjectCraftList)
        }
  }

  @Test
  fun `verify and document find one project craft`() {
    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf("/projects/{projectId}/crafts/{projectCraftId}"),
                    project.identifier,
                    projectCraft.identifier)))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"0\""),
            *hasIdentifierAndVersion(projectCraft.identifier.toUuid()),
            *isCreatedBy(creatorUser),
            *isLastModifiedBy(creatorUser),
            *hasReference(project),
            jsonPath("$.name").value(projectCraft.name),
            jsonPath("$.color").value(projectCraft.color),
            jsonPath("$._links.update.href").exists(),
            jsonPath("$._links.delete.href").exists())
        .andDo(
            document(
                "project-crafts/document-get-project-craft",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(PROJECTCRAFT_PATH_PARAMETER_DESCRIPTOR),
                responseHeaders(ETAG_HEADER_DESCRIPTOR),
                responseFields(PROJECTCRAFT_RESPONSE_FIELD_DESCRIPTORS),
                links(PROJECTCRAFT_LINK_DESCRIPTORS)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document find all project craft`() {
    mockMvc
        .perform(
            requestBuilder(
                get(latestVersionOf("/projects/{projectId}/crafts"), project.identifier)))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"0\""),
            jsonPath("$.projectCrafts").exists(),
            jsonPath("$.projectCrafts.length()").value(1),
            jsonPath("$.version").value("0"),
            jsonPath("$._links.create.href").exists(),
            jsonPath("$._links.reorder.href").exists(),
            jsonPath("$.projectCrafts[0].id").value(projectCraft.identifier.toString()))
        .andDo(
            document(
                "project-crafts/document-get-project-crafts",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(PROJECT_PATH_PARAMETER_DESCRIPTOR),
                responseHeaders(ETAG_HEADER_DESCRIPTOR),
                PROJECTCRAFT_RESPONSE_FIELDS,
                links(PROJECTCRAFTLIST_LINK_DESCRIPTORS)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document update project craft`() {
    val resource = SaveProjectCraftResource("Update Craft name", "#333333", 1)

    mockMvc
        .perform(
            requestBuilder(
                put(
                    latestVersionOf("/projects/{projectId}/crafts/{projectCraftId}"),
                    project.identifier,
                    projectCraft.identifier),
                resource,
                projectCraft.version))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"1\""),
            *hasIdentifierAndVersion(projectCraft.identifier.toUuid(), 1),
            *isCreatedBy(creatorUser),
            *isLastModifiedBy(testUser),
            *hasReference(project),
            jsonPath("$.name").value(resource.name),
            jsonPath("$.color").value(resource.color),
            jsonPath("$._links.update.href").exists(),
            jsonPath("$._links.delete.href").exists())
        .andDo(
            document(
                "project-crafts/document-update-project-craft",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(IF_MATCH_HEADER_DESCRIPTOR),
                pathParameters(PROJECTCRAFT_PATH_PARAMETER_DESCRIPTOR),
                requestFields(SAVE_PROJECTCRAFT_REQUEST_FIELD_DESCRIPTORS),
                responseHeaders(ETAG_HEADER_DESCRIPTOR),
                responseFields(PROJECTCRAFT_RESPONSE_FIELD_DESCRIPTORS),
                links(PROJECTCRAFT_LINK_DESCRIPTORS)))

    val updatedProjectCraft = repositories.findProjectCraft(projectCraft.identifier)!!
    projectEventStoreUtils
        .verifyContainsAndGet(ProjectCraftEventG2Avro::class.java, UPDATED, 1, true)
        .also { verifyUpdatedAggregate(it[0].aggregate, updatedProjectCraft, project.identifier) }
  }

  @Test
  fun `verify and document reorder project craft`() {
    // Add another project craft to the first position to properly test the reorder
    eventStreamGenerator
        .submitProjectCraftG2(asReference = "anotherProjectCraft")
        .submitProjectCraftList(asReference = "projectCraftList", eventType = ITEMADDED) {
          it.projectCrafts =
              listOf(getByReference("anotherProjectCraft"), getByReference("projectCraft"))
        }

    projectEventStoreUtils.reset()

    val anotherProjectCraftIdentifier = getIdentifier("anotherProjectCraft").asProjectCraftId()
    val resource = ReorderProjectCraftResource(projectCraft.identifier, 1)

    mockMvc
        .perform(
            requestBuilder(
                put(latestVersionOf("/projects/{projectId}/crafts"), project.identifier),
                resource,
                projectCraftList.version))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"2\""),
            jsonPath("$.projectCrafts").exists(),
            jsonPath("$.projectCrafts.length()").value(2),
            jsonPath("$.version").value("2"),
            jsonPath("$._links.create.href").exists(),
            jsonPath("$._links.reorder.href").exists(),
            jsonPath("$.projectCrafts[0].id").value(projectCraft.identifier.toString()),
            jsonPath("$.projectCrafts[1].id").value(anotherProjectCraftIdentifier.toString()))
        .andDo(
            document(
                "project-crafts/document-reorder-project-craft",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(IF_MATCH_HEADER_DESCRIPTOR),
                pathParameters(PROJECT_PATH_PARAMETER_DESCRIPTOR),
                requestFields(REORDER_PROJECTCRAFT_REQUEST_FIELD_DESCRIPTORS),
                responseHeaders(ETAG_HEADER_DESCRIPTOR),
                PROJECTCRAFT_RESPONSE_FIELDS,
                links(PROJECTCRAFTLIST_LINK_DESCRIPTORS)))

    val updateProjectCraftList = repositories.findProjectCraftList(projectCraftList.identifier)!!
    projectEventStoreUtils
        .verifyContainsAndGet(ProjectCraftListEventAvro::class.java, REORDERED, 1, true)
        .also {
          verifyUpdatedAggregate(it[0].aggregate, project.identifier, updateProjectCraftList)
        }
  }

  @Test
  fun `verify and document delete project craft`() {
    eventStreamGenerator
        .submitProjectCraftG2(asReference = "anotherProjectCraft")
        .submitProjectCraftList(asReference = "projectCraftList", eventType = ITEMADDED) {
          it.projectCrafts =
              listOf(getByReference("projectCraft"), getByReference("anotherProjectCraft"))
        }

    projectEventStoreUtils.reset()

    val anotherProjectCraft =
        repositories.findProjectCraft(getIdentifier("anotherProjectCraft").asProjectCraftId())!!

    mockMvc
        .perform(
            requestBuilder(
                request =
                    delete(
                        latestVersionOf("/projects/{projectId}/crafts/{projectCraftId}"),
                        project.identifier,
                        anotherProjectCraft.identifier),
                version = projectCraft.version))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"2\""),
            jsonPath("$.projectCrafts").exists(),
            jsonPath("$.projectCrafts.length()").value(1),
            jsonPath("$.version").value("2"),
            jsonPath("$._links.create.href").exists(),
            jsonPath("$._links.reorder.href").exists(),
            jsonPath("$.projectCrafts[0].id").value(projectCraft.identifier.toString()))
        .andDo(
            document(
                "project-crafts/document-delete-project-craft",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(IF_MATCH_HEADER_DESCRIPTOR),
                pathParameters(PROJECTCRAFT_PATH_PARAMETER_DESCRIPTOR),
                responseHeaders(ETAG_HEADER_DESCRIPTOR),
                PROJECTCRAFT_RESPONSE_FIELDS,
                links(PROJECTCRAFTLIST_LINK_DESCRIPTORS)))

    projectEventStoreUtils
        .verifyContainsAndGet(ProjectCraftEventG2Avro::class.java, DELETED, 1, false)
        .also {
          verifyDeletedAggregate(it[0].aggregate, anotherProjectCraft, project.identifier, testUser)
        }

    val updateProjectCraftList = repositories.findProjectCraftList(projectCraftList.identifier)!!
    projectEventStoreUtils
        .verifyContainsAndGet(ProjectCraftListEventAvro::class.java, ITEMREMOVED, 1, false)
        .also {
          verifyUpdatedAggregate(it[0].aggregate, project.identifier, updateProjectCraftList)
        }
  }

  companion object {

    private val PROJECT_PATH_PARAMETER_DESCRIPTOR =
        listOf(parameterWithName("projectId").description("ID of the project"))

    private val PROJECTCRAFT_PATH_PARAMETER_DESCRIPTOR =
        listOf(
            parameterWithName("projectId").description("ID of the project"),
            parameterWithName("projectCraftId").description("ID of the project craft"))

    private val SAVE_PROJECTCRAFT_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            ConstrainedFields(SaveProjectCraftResource::class.java)
                .withPath("name")
                .description("The name of the project craft")
                .type(STRING),
            ConstrainedFields(SaveProjectCraftResource::class.java)
                .withPath("color")
                .description("The color of the project craft")
                .type(STRING),
            ConstrainedFields(SaveProjectCraftResource::class.java)
                .withPath("position")
                .description("The position of the craft")
                .type(NUMBER)
                .optional())

    private val REORDER_PROJECTCRAFT_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            ConstrainedFields(ReorderProjectCraftResource::class.java)
                .withPath("projectCraftId")
                .description("The project craft identifier")
                .type(STRING),
            ConstrainedFields(ReorderProjectCraftResource::class.java)
                .withPath("position")
                .description("The position of the craft")
                .type(NUMBER))

    private val PROJECTCRAFT_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
            *PROJECT_REFERENCE_FIELD_DESCRIPTORS,
            fieldWithPath("name").description("The name of the project craft"),
            fieldWithPath("color").description("The color of the project craft"),
            subsectionWithPath("_links").ignored())

    private val PROJECTCRAFT_RESPONSE_FIELDS =
        responseFields(
                fieldWithPath("projectCrafts[]").description("List of crafts").type(ARRAY),
                fieldWithPath("version")
                    .description("The version of the project craft list")
                    .type(NUMBER),
                subsectionWithPath("_links").ignored())
            .andWithPrefix("projectCrafts[].", PROJECTCRAFT_RESPONSE_FIELD_DESCRIPTORS)

    private val PROJECTCRAFT_LINK_DESCRIPTORS =
        listOf(
            linkWithRel(LINK_UPDATE).description("Link to update the project craft resource"),
            linkWithRel(LINK_DELETE).description("Link to delete the project craft resource"))

    private val PROJECTCRAFTLIST_LINK_DESCRIPTORS =
        listOf(
            linkWithRel(LINK_CREATE).description("Link to create work area resources"),
            linkWithRel(LINK_REORDER)
                .description("Link to reorder the project craft list resource"))
  }
}
