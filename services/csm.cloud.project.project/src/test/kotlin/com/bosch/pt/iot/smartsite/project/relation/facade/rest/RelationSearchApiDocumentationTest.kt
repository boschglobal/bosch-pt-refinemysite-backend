/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRelation
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.hasIdentifierAndVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isCreatedBy
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isLastModifiedBy
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isPage
import com.bosch.pt.iot.smartsite.project.relation.boundary.RelationSearchService.Companion.RELATION_SEARCH_ALLOWED_SORTING_PROPERTIES
import com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto.RelationElementDto
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.RelationApiDocumentationTest.Companion.RELATION_WITH_CRITICAL_RESPONSE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.RelationController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.request.FilterRelationResource
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.MILESTONE
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.TASK
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum.FINISH_TO_START
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class RelationSearchApiDocumentationTest : AbstractApiDocumentationTestV2() {

  private val creatorUser by lazy { repositories.findUser(getIdentifier("userCsm1"))!! }

  @BeforeEach
  fun setup() {
    // Because the relations are return sorted by identifier
    // there is the need to manually set up the aggregate identifier
    eventStreamGenerator
        .setupDatasetTestData()
        .submitTask(asReference = "task1")
        .submitRelation(asReference = "relation1") {
          it.aggregateIdentifierBuilder.identifier = "11111111-1111-1111-1111-111111111111"
        }
        .submitTask(asReference = "task2")
        .submitRelation(asReference = "relation2") {
          it.aggregateIdentifierBuilder.identifier = "22222222-2222-2222-2222-222222222222"
        }
        .submitTask(asReference = "task3")
        .submitRelation(asReference = "relation3") {
          it.aggregateIdentifierBuilder.identifier = "33333333-3333-3333-3333-333333333333"
        }

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document search relations`() {
    val filterResource =
        FilterRelationResource(
            types = emptySet(),
            sources =
                setOf(
                    RelationElementDto(getIdentifier("task1"), TASK),
                    RelationElementDto(getIdentifier("task2"), TASK),
                    RelationElementDto(getIdentifier("task3"), TASK)),
            targets = setOf())

    this.mockMvc
        .perform(
            requestBuilder(
                    post(
                        latestVersionOf("/projects/{projectId}/relations/search"),
                        getIdentifier("project")),
                    filterResource)
                .param("size", "1")
                .param("page", "1"))
        .andExpectAll(
            status().isOk,
            *hasIdentifierAndVersion(getIdentifier("relation2"), index = 0),
            *isCreatedBy(creatorUser, index = 0),
            *isLastModifiedBy(creatorUser, index = 0),
            jsonPath("$.items[0].type").value(FINISH_TO_START.name),
            jsonPath("$.items[0].source.id").value(getIdentifier("task2").toString()),
            jsonPath("$.items[0].source.type").value(TASK.name),
            jsonPath("$.items[0].target.id").value(getIdentifier("milestone").toString()),
            jsonPath("$.items[0].target.type").value(MILESTONE.name),
            jsonPath("$.items[0]._links.delete.href").exists(),
            *isPage(pageNumber = 1, pageSize = 1, totalPages = 3, totalElements = 3))
        .andDo(
            MockMvcRestDocumentation.document(
                "relations/document-search-relations",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(PROJECT_REQUEST_PARAMETER_DESCRIPTOR),
                requestFields(RELATION_SEARCH_REQUEST_FIELD_DESCRIPTORS),
                buildPagedItemsListResponseFields(
                    RELATION_WITH_CRITICAL_RESPONSE_FIELD_DESCRIPTORS),
                links(PREVIOUS_LINK_DESCRIPTOR, NEXT_LINK_DESCRIPTOR)))

    projectEventStoreUtils.verifyEmpty()
  }

  companion object {

    private val PROJECT_REQUEST_PARAMETER_DESCRIPTOR =
        listOf(
            parameterWithName(PATH_VARIABLE_PROJECT_ID).description("ID of the project"),
            *buildSortingAndPagingParameterDescriptors(
                    RELATION_SEARCH_ALLOWED_SORTING_PROPERTIES.keys)
                .toTypedArray())

    private val RELATION_SEARCH_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            ConstrainedFields(FilterRelationResource::class.java)
                .withPath("types[]")
                .description(
                    "The relation types filter criterion. Leave empty to search all relation types.")
                .type(ARRAY)
                .optional(),
            ConstrainedFields(FilterRelationResource::class.java)
                .withPath("sources[]")
                .description("The sources filter criterion.")
                .type(ARRAY)
                .optional(),
            ConstrainedFields(FilterRelationResource::class.java)
                .withPath("sources[].id")
                .description("The source ID")
                .type(STRING),
            ConstrainedFields(FilterRelationResource::class.java)
                .withPath("sources[].type")
                .description("The source type. Valid values are: `MILESTONE`, `TASK`.")
                .type(STRING),
            ConstrainedFields(FilterRelationResource::class.java)
                .withPath("targets[]")
                .description("The targets filter criterion.")
                .type(ARRAY)
                .optional(),
            ConstrainedFields(FilterRelationResource::class.java)
                .withPath("targets[].id")
                .description("The target ID")
                .type(STRING),
            ConstrainedFields(FilterRelationResource::class.java)
                .withPath("targets[].type")
                .description("The target type. Valid values are: `MILESTONE`, `TASK`.")
                .type(STRING),
        )
  }
}
