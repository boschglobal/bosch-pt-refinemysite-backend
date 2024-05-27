/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.CreateBatchRequestResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.RELATION
import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitMilestone
import com.bosch.pt.csm.cloud.projectmanagement.relation.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.relation.message.getSourceIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.relation.message.getTargetIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ABSTRACT_RESOURCE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER_DESCRIPTOR
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.IF_MATCH_HEADER_DESCRIPTOR
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.LOCATION_HEADER_DESCRIPTOR
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.hasIdentifierAndVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isCreatedBy
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isLastModifiedBy
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto.RelationElementDto
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.RelationController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.RelationController.Companion.PATH_VARIABLE_RELATION_ID
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.request.CreateRelationResource
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.response.RelationResource.Companion.LINK_DELETE
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.MILESTONE
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.TASK
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum.FINISH_TO_START
import java.util.UUID
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
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.BOOLEAN
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
class RelationApiDocumentationTest : AbstractApiDocumentationTestV2() {

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }
  private val taskIdentifier by lazy { getIdentifier("task") }
  private val milestoneIdentifier by lazy { getIdentifier("milestone") }

  private val creatorUser by lazy { repositories.findUser(getIdentifier("userCsm1"))!! }
  private val testUser by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }
  private val relation by lazy {
    repositories.findRelation(getIdentifier("relation"), projectIdentifier)!!
  }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document create relation`() {
    val createResource =
        CreateRelationResource(
            type = FINISH_TO_START,
            source = RelationElementDto(milestoneIdentifier, MILESTONE),
            target = RelationElementDto(taskIdentifier, TASK))

    mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf("/projects/{projectId}/relations"), projectIdentifier),
                createResource))
        .andExpectAll(
            status().isCreated,
            header().string(ETAG_HEADER, "\"0\""),
            header()
                .string(
                    LOCATION,
                    matchesRegex(
                        ".*${latestVersionOf(
                          "/projects/$projectIdentifier/relations/[-a-z0-9]{36}$")}")),
            *hasIdentifierAndVersion(),
            *isCreatedBy(testUser),
            *isLastModifiedBy(testUser),
            jsonPath("$.type").value(FINISH_TO_START.name),
            jsonPath("$.source.id").value(milestoneIdentifier.toString()),
            jsonPath("$.source.type").value(MILESTONE.name),
            jsonPath("$.target.id").value(taskIdentifier.toString()),
            jsonPath("$.target.type").value(TASK.name),
            jsonPath("$.critical").doesNotExist(),
            jsonPath("$._links.delete.href").exists())
        .andDo(
            document(
                "relations/document-create-relation",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(PROJECT_PATH_PARAMETER_DESCRIPTOR),
                requestFields(RELATION_CREATE_REQUEST_FIELD_DESCRIPTORS),
                responseFields(RELATION_WITHOUT_CRITICAL_RESPONSE_FIELD_DESCRIPTORS),
                responseHeaders(ETAG_HEADER_DESCRIPTOR, LOCATION_HEADER_DESCRIPTOR),
                links(RELATION_LINK_DESCRIPTORS)))

    projectEventStoreUtils
        .verifyContainsAndGet(RelationEventAvro::class.java, CREATED, 1, true)
        .also {
          verifyCreatedAggregate(it[0].getAggregate(), createResource, projectIdentifier.toUuid())
        }
  }

  @Test
  fun `verify and document batch create relations`() {
    val createResource =
        CreateBatchRequestResource(
            items =
                setOf(
                    CreateRelationResource(
                        type = FINISH_TO_START,
                        source = RelationElementDto(milestoneIdentifier, MILESTONE),
                        target = RelationElementDto(taskIdentifier, TASK))))

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf("/projects/{projectId}/relations/batch/create"),
                    projectIdentifier),
                createResource))
        .andExpectAll(
            status().isOk,
            *hasIdentifierAndVersion(index = 0),
            *isCreatedBy(testUser, index = 0),
            *isLastModifiedBy(testUser, index = 0),
            jsonPath("$.items[0].type").value(FINISH_TO_START.name),
            jsonPath("$.items[0].source.id").value(milestoneIdentifier.toString()),
            jsonPath("$.items[0].source.type").value(MILESTONE.name),
            jsonPath("$.items[0].target.id").value(taskIdentifier.toString()),
            jsonPath("$.items[0].target.type").value(TASK.name),
            jsonPath("$.items[0].critical").doesNotExist(),
            jsonPath("$.items[0]._links.delete.href").exists())
        .andDo(
            document(
                "relations/document-batch-create-relations",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(PROJECT_PATH_PARAMETER_DESCRIPTOR),
                buildCreateBatchRequestFields(RELATION_CREATE_REQUEST_FIELD_DESCRIPTORS),
                buildBatchItemsListResponseFields(
                    RELATION_WITHOUT_CRITICAL_RESPONSE_FIELD_DESCRIPTORS)))

    projectEventStoreUtils
        .verifyContainsAndGet(RelationEventAvro::class.java, CREATED, 1, false)
        .also {
          verifyCreatedAggregate(
              it[0].getAggregate(), createResource.items.elementAt(0), projectIdentifier.toUuid())
        }
  }

  @Test
  fun `verify and document find relation`() {
    // trigger calculation of relation criticality
    useOnlineListener()
    eventStreamGenerator.submitMilestone(eventType = MilestoneEventEnumAvro.UPDATED)
    projectEventStoreUtils.reset()

    mockMvc
        .perform(
            requestBuilder(
                get(
                    latestVersionOf("/projects/{projectId}/relations/{relationId}"),
                    projectIdentifier,
                    relation.identifier)))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"1\""),
            *hasIdentifierAndVersion(relation.identifier!!, 1),
            *isCreatedBy(creatorUser),
            *isLastModifiedBy(creatorUser),
            jsonPath("$.type").value(FINISH_TO_START.name),
            jsonPath("$.source.id").value(taskIdentifier.toString()),
            jsonPath("$.source.type").value(TASK.name),
            jsonPath("$.target.id").value(milestoneIdentifier.toString()),
            jsonPath("$.target.type").value(MILESTONE.name),
            jsonPath("$.critical").value(false),
            jsonPath("$._links.delete.href").exists())
        .andDo(
            document(
                "relations/document-get-relation",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    PROJECT_PATH_PARAMETER_DESCRIPTOR, RELATION_PATH_PARAMETER_DESCRIPTOR),
                responseFields(RELATION_WITH_CRITICAL_RESPONSE_FIELD_DESCRIPTORS),
                responseHeaders(ETAG_HEADER_DESCRIPTOR),
                links(RELATION_LINK_DESCRIPTORS)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document batch find relations`() {
    // trigger calculation of relation criticality
    useOnlineListener()
    eventStreamGenerator.submitMilestone(eventType = MilestoneEventEnumAvro.UPDATED)
    projectEventStoreUtils.reset()

    val findResource = BatchRequestResource(ids = setOf(relation.identifier!!))

    mockMvc
        .perform(
            requestBuilder(
                post(
                    latestVersionOf("/projects/{projectId}/relations/batch/find"),
                    projectIdentifier),
                findResource))
        .andExpectAll(
            status().isOk,
            *hasIdentifierAndVersion(relation.identifier!!, 1, index = 0),
            *isCreatedBy(creatorUser, index = 0),
            *isLastModifiedBy(creatorUser, index = 0),
            jsonPath("$.items[0].type").value(FINISH_TO_START.name),
            jsonPath("$.items[0].source.id").value(taskIdentifier.toString()),
            jsonPath("$.items[0].source.type").value(TASK.name),
            jsonPath("$.items[0].target.id").value(milestoneIdentifier.toString()),
            jsonPath("$.items[0].target.type").value(MILESTONE.name),
            jsonPath("$.items[0].critical").value(false),
            jsonPath("$.items[0]._links.delete.href").exists())
        .andDo(
            document(
                "relations/document-batch-get-relations",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(PROJECT_PATH_PARAMETER_DESCRIPTOR),
                requestFields(BATCH_REQUEST_RESOURCE_FIELD_DESCRIPTORS),
                buildBatchItemsListResponseFields(
                    RELATION_WITH_CRITICAL_RESPONSE_FIELD_DESCRIPTORS)))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document delete relation`() {
    mockMvc
        .perform(
            requestBuilder(
                request =
                    delete(
                        latestVersionOf("/projects/{projectId}/relations/{relationId}"),
                        projectIdentifier,
                        relation.identifier),
                version = 0L))
        .andExpect(status().isNoContent)
        .andDo(
            document(
                "relations/document-delete-relation",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(IF_MATCH_HEADER_DESCRIPTOR),
                pathParameters(
                    PROJECT_PATH_PARAMETER_DESCRIPTOR, RELATION_PATH_PARAMETER_DESCRIPTOR)))

    projectEventStoreUtils
        .verifyContainsAndGet(RelationEventAvro::class.java, DELETED, 1, true)
        .also { verifyDeletedAggregate(it[0].getAggregate(), relation, projectIdentifier.toUuid()) }
  }

  private fun verifyCreatedAggregate(
      aggregate: RelationAggregateAvro,
      resource: CreateRelationResource,
      projectId: UUID
  ) =
      with(aggregate) {
        validateCreatedAggregateAuditInfoAndAggregateIdentifier(this, RELATION, testUser)
        assertThat(getProjectIdentifier()).isEqualTo(projectId)
        assertThat(getType().name).isEqualTo(resource.type.name)
        assertThat(getSource().getType()).isEqualTo(resource.source.type.name)
        assertThat(getSourceIdentifier()).isEqualTo(resource.source.id)
        assertThat(getTarget().getType()).isEqualTo(resource.target.type.name)
        assertThat(getTargetIdentifier()).isEqualTo(resource.target.id)
      }

  private fun verifyDeletedAggregate(
      aggregate: RelationAggregateAvro,
      relation: Relation,
      projectId: UUID
  ) =
      with(aggregate) {
        validateDeletedAggregateAuditInfoAndAggregateIdentifier(this, relation, testUser)
        assertThat(getProjectIdentifier()).isEqualTo(projectId)
        assertThat(getType().name).isEqualTo(relation.type.name)
        assertThat(getSource().getType()).isEqualTo(relation.source.type.name)
        assertThat(getSourceIdentifier()).isEqualTo(relation.source.identifier)
        assertThat(getTarget().getType()).isEqualTo(relation.target.type.name)
        assertThat(getTargetIdentifier()).isEqualTo(relation.target.identifier)
      }

  companion object {

    private val PROJECT_PATH_PARAMETER_DESCRIPTOR =
        parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project")

    private val RELATION_PATH_PARAMETER_DESCRIPTOR =
        parameterWithName(PATH_VARIABLE_RELATION_ID).description("ID of the relation")

    private val RELATION_CREATE_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            ConstrainedFields(CreateRelationResource::class.java)
                .withPath("type")
                .description("Type of the relation. Valid values are: `FINISH_TO_START`, `PART_OF`")
                .type(STRING),
            ConstrainedFields(CreateRelationResource::class.java)
                .withPath("source.id")
                .description("ID of the relation source")
                .type(STRING),
            ConstrainedFields(CreateRelationResource::class.java)
                .withPath("source.type")
                .description(
                    "Type of the relation source. Valid values are: `MILESTONE`, `TASK`. " +
                        "Must be `TASK` if relation type is `PART_OF`.")
                .type(STRING),
            ConstrainedFields(CreateRelationResource::class.java)
                .withPath("target.id")
                .description("ID of the relation target")
                .type(STRING),
            ConstrainedFields(CreateRelationResource::class.java)
                .withPath("target.type")
                .description(
                    "Type of the relation target. Valid values are: `MILESTONE`, `TASK`. " +
                        "Must be `MILESTONE` if relation type is `PART_OF`.")
                .type(STRING),
        )

    private fun buildCommonResponseFieldDescriptors() =
        listOf(
            *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
            fieldWithPath("type")
                .description(
                    "Type of the relation. Possible values include: `FINISH_TO_START`, `PART_OF`")
                .type(STRING),
            fieldWithPath("source.id").description("ID of the relation source").type(STRING),
            fieldWithPath("source.type")
                .description(
                    "Type of the relation source. Possible values include: `MILESTONE`, `TASK`.")
                .type(STRING),
            fieldWithPath("target.id").description("ID of the relation target").type(STRING),
            fieldWithPath("target.type")
                .description(
                    "Type of the relation target. Possible values include: `MILESTONE`, `TASK`.")
                .type(STRING),
            subsectionWithPath("_links").ignored(),
        )

    private val RELATION_WITHOUT_CRITICAL_RESPONSE_FIELD_DESCRIPTORS =
        buildCommonResponseFieldDescriptors()

    private val RELATION_LINK_DESCRIPTORS =
        listOf(
            linkWithRel(LINK_DELETE)
                .description("Link to delete the relation, available if delete is possible")
                .optional())

    // This variable is used in the RelationSearchApiDocumentationTest
    val RELATION_WITH_CRITICAL_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            *buildCommonResponseFieldDescriptors().toTypedArray(),
            fieldWithPath("critical")
                .description(
                    "Criticality of the relation. This field is present only for `FINISH_TO_START` " +
                        "relations. A relation is critical if the successor's (start) date is after the (end) " +
                        "date of the predecessor. The criticality (re-)calculation is triggered automatically " +
                        "after creating a relation, or after updating the source or target of an existing relation. " +
                        "Only after the initial calculation, this field will be present.")
                .type(BOOLEAN)
                .optional())
  }
}
