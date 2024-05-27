/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.MilestoneApiDocumentationTest.Companion.MILESTONE_RESPONSE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.FilterMilestoneListResource
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.FilterMilestoneListResource.WorkAreaFilter
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneResource.Companion.LINK_CREATE_CRAFT_MILESTONE
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneResource.Companion.LINK_CREATE_INVESTOR_MILESTONE
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response.MilestoneResource.Companion.LINK_CREATE_PROJECT_MILESTONE
import com.bosch.pt.iot.smartsite.project.milestone.query.MilestoneQueryService.Companion.MILESTONE_SEARCH_ALLOWED_SORTING_PROPERTIES
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
import org.springframework.restdocs.payload.JsonFieldType.BOOLEAN
import org.springframework.restdocs.payload.JsonFieldType.OBJECT
import org.springframework.restdocs.payload.JsonFieldType.STRING
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@EnableAllKafkaListeners
class MilestoneSearchApiDocumentationTest : AbstractApiDocumentationTestV2() {

  @BeforeEach
  fun setup() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(getIdentifier("userCsm2"))
  }

  @Test
  fun `verify and document search milestones`() {
    mockMvc
        .perform(
            requestBuilder(
                post(
                        latestVersionOf("/projects/{projectId}/milestones/search"),
                        getIdentifier("project").asProjectId())
                    .param("sort", "date")
                    .param("size", "1")
                    .param("page", "0"),
                FilterMilestoneListResource(workAreas = WorkAreaFilter(header = true))))
        .andExpectAll(
            status().isOk,
            content().contentType(HAL_JSON_VALUE),
            jsonPath("$.items.length()").value(1),
            jsonPath("$._links.createCraftMilestone").exists(),
            jsonPath("$._links.createInvestorMilestone").exists(),
            jsonPath("$._links.createProjectMilestone").exists(),
        )
        .andDo(
            document(
                "milestones/document-search-milestones",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    buildSortingAndPagingParameterDescriptors(
                        MILESTONE_SEARCH_ALLOWED_SORTING_PROPERTIES.keys) +
                        parameterWithName("projectId").description("The id of the project")),
                requestFields(buildSearchMilestonesRequestFields()),
                MILESTONE_LIST_RESPONSE_FIELDS,
                links(MILESTONE_LIST_LINK_DESCRIPTORS)))
  }

  companion object {

    const val LINK_CREATE_CRAFT_MILESTONE_DESCRIPTION = "Link to create a craft milestone."
    const val LINK_CREATE_INVESTORS_MILESTONE_DESCRIPTION = "Link to create an investors milestone."
    const val LINK_CREATE_PROJECT_MILESTONE_DESCRIPTION = "Link to create a project milestone."

    private val MILESTONE_TYPE_VALUES =
        "Valid values are: " + MilestoneTypeEnum.values().joinToString(", ")

    fun buildSearchMilestonesRequestFields(pathPrefix: String = "") =
        listOf(
            ConstrainedFields(FilterMilestoneListResource::class.java)
                .withPath(pathPrefix, "types")
                .description(
                    "A JSON object to filter milestones by their type or the subset of craft identifiers. " +
                        "If only the project craft identifiers are set, the craft milestones will be returned " +
                        "that satisfy the conditions.")
                .optional()
                .type(OBJECT),
            ConstrainedFields(FilterMilestoneListResource::class.java)
                .withPath(pathPrefix, "types.types")
                .description("List of milestone types. $MILESTONE_TYPE_VALUES")
                .optional()
                .type(ARRAY),
            ConstrainedFields(FilterMilestoneListResource::class.java)
                .withPath(pathPrefix, "types.projectCraftIds")
                .description("List of craft identifiers")
                .optional()
                .type(ARRAY),
            ConstrainedFields(FilterMilestoneListResource::class.java)
                .withPath(pathPrefix, "workAreas")
                .description(
                    "A JSON object to filter milestones by their work area or by belonging to the calendar header. " +
                        "If workAreaIds and headers are both set, those milestones will be returned " +
                        "that satisfy any of the conditions (logical or).")
                .optional()
                .type(OBJECT),
            ConstrainedFields(FilterMilestoneListResource::class.java)
                .withPath(pathPrefix, "workAreas.workAreaIds")
                .description("List of work area identifiers")
                .optional()
                .type(ARRAY),
            ConstrainedFields(FilterMilestoneListResource::class.java)
                .withPath(pathPrefix, "workAreas.header")
                .description(
                    "Set to true to include milestones that belong to the calendar header.")
                .optional()
                .type(BOOLEAN),
            ConstrainedFields(FilterMilestoneListResource::class.java)
                .withPath(pathPrefix, "from")
                .description("Lower bound of milestones dates")
                .optional()
                .type(STRING),
            ConstrainedFields(FilterMilestoneListResource::class.java)
                .withPath(pathPrefix, "to")
                .description("Upper bound of milestones dates")
                .optional()
                .type(STRING),
            ConstrainedFields(FilterMilestoneListResource::class.java)
                .withPath(pathPrefix, "milestoneListIds")
                .description(
                    "List of milestone list identifiers representing slots in the calendar. " +
                        "This was only introduced to resolve live update event received by a client.")
                .optional()
                .type(ARRAY))

    private val MILESTONE_LIST_RESPONSE_FIELDS =
        buildPagedItemsListResponseFields(MILESTONE_RESPONSE_FIELD_DESCRIPTORS)

    private val MILESTONE_LIST_LINK_DESCRIPTORS =
        listOf(
            linkWithRel(LINK_CREATE_CRAFT_MILESTONE)
                .description(LINK_CREATE_CRAFT_MILESTONE_DESCRIPTION),
            linkWithRel(LINK_CREATE_INVESTOR_MILESTONE)
                .description(LINK_CREATE_INVESTORS_MILESTONE_DESCRIPTION),
            linkWithRel(LINK_CREATE_PROJECT_MILESTONE)
                .description(LINK_CREATE_PROJECT_MILESTONE_DESCRIPTION))
  }
}
