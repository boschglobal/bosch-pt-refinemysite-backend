/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ABSTRACT_RESOURCE_FIELD_DESCRIPTORS
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.ETAG_HEADER
import com.bosch.pt.iot.smartsite.common.facade.rest.ApiDocumentationSnippets.LOCATION_HEADER_DESCRIPTOR
import com.bosch.pt.iot.smartsite.common.facade.rest.ConstrainedFields
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.hasIdentifierAndVersion
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isCreatedBy
import com.bosch.pt.iot.smartsite.common.facade.rest.CustomMockMvcResultMatchers.isLastModifiedBy
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.MilestoneSearchApiDocumentationTest.Companion.buildSearchMilestonesRequestFields
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.FilterMilestoneListResource
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.quickfilter.domain.QuickFilterId
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.QuickFilterController.Companion.QUICK_FILTERS_ENDPOINT
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.QuickFilterController.Companion.QUICK_FILTER_ENDPOINT
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.request.SaveQuickFilterResource
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.request.SaveQuickFilterResource.CriteriaResource
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.response.QuickFilterListResource
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.response.QuickFilterResource
import com.bosch.pt.iot.smartsite.project.quickfilter.model.MilestoneCriteria
import com.bosch.pt.iot.smartsite.project.quickfilter.model.QuickFilter
import com.bosch.pt.iot.smartsite.project.quickfilter.model.TaskCriteria
import com.bosch.pt.iot.smartsite.project.quickfilter.repository.QuickFilterRepository
import com.bosch.pt.iot.smartsite.project.task.facade.rest.common.AbstractTaskApiDocumentationTest.Companion.buildSearchTasksRequestFieldDescriptorsV2
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.FilterTaskListResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.FilterTaskListResource.FilterAssigneeResource
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.CRITICAL
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import java.time.LocalDate
import java.util.Collections.emptySet
import java.util.UUID
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.JsonFieldType.ARRAY
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
class QuickFilterApiDocumentationTest : AbstractApiDocumentationTestV2() {

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }
  private val testUser by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }
  private val participantCsmIdentifier by lazy {
    getIdentifier("participantCsm1").asParticipantId()
  }
  private val participantCsm2Identifier by lazy {
    getIdentifier("participantCsm2").asParticipantId()
  }
  private val projectCraftIdentifier by lazy { getIdentifier("projectCraft").asProjectCraftId() }
  private val workAreaIdentifier by lazy { getIdentifier("workArea").asWorkAreaId() }

  @Autowired private lateinit var quickFilterRepository: QuickFilterRepository

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication("userCsm2")
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify and document create quick filter`() {

    val createQuickFilterResource =
        buildCreateQuickFilterResource(
            name = "Quick Filter",
            assigneeIds = setOf(participantCsm2Identifier),
            projectCraftIds = setOf(projectCraftIdentifier),
            workAreaIds = setOf(WorkAreaIdOrEmpty(workAreaIdentifier)),
            status = setOf(OPEN),
            from = LocalDate.now(),
            to = LocalDate.now().plusDays(14),
            topicCriticality = setOf(CRITICAL))

    this.mockMvc
        .perform(
            requestBuilder(
                post(latestVersionOf(QUICK_FILTERS_ENDPOINT), projectIdentifier),
                createQuickFilterResource))
        .andExpectAll(
            status().isCreated,
            header().string(ETAG_HEADER, "\"0\""),
            jsonPath("$.name").value(createQuickFilterResource.name),
            header().string(LOCATION, containsString("/projects/$projectIdentifier/quickfilters")),
            *hasIdentifierAndVersion(),
            *isCreatedBy(testUser),
            *isLastModifiedBy(testUser),
            jsonPath("$.useMilestoneCriteria").value(true),
            jsonPath("$.useTaskCriteria").value(true),
            jsonPath("$.highlight").value(false),
            jsonPath("$.criteria.milestones.from")
                .value(createQuickFilterResource.criteria.milestones.from.toString()),
            jsonPath("$.criteria.milestones.to")
                .value(createQuickFilterResource.criteria.milestones.to.toString()),
            jsonPath(
                "$.criteria.milestones.workAreas.workAreaIds",
                contains(workAreaIdentifier.toString())),
            jsonPath("$.criteria.milestones.workAreas.header").value(false),
            jsonPath("$.criteria.milestones.types.types").isEmpty,
            jsonPath(
                "$.criteria.milestones.types.projectCraftIds",
                contains(projectCraftIdentifier.toString())),
            jsonPath("$.criteria.tasks.from")
                .value(createQuickFilterResource.criteria.tasks.from.toString()),
            jsonPath("$.criteria.tasks.to")
                .value(createQuickFilterResource.criteria.tasks.to.toString()),
            jsonPath("$.criteria.tasks.workAreaIds", contains(workAreaIdentifier.toString())),
            jsonPath(
                "$.criteria.tasks.projectCraftIds", contains(projectCraftIdentifier.toString())),
            jsonPath("$.criteria.tasks.status", contains(OPEN.toString())),
            jsonPath(
                "$.criteria.tasks.assignees.participantIds",
                contains(participantCsm2Identifier.toString())),
            jsonPath("$.criteria.tasks.topicCriticality", contains(CRITICAL.toString())),
            jsonPath("$.createdBy.id").value(testUser.identifier.toString()),
            jsonPath("$.createdBy.displayName").value(testUser.getDisplayName()),
            jsonPath("$.lastModifiedBy.id").value(testUser.identifier.toString()),
            jsonPath("$.lastModifiedBy.displayName").value(testUser.getDisplayName()))
        .andDo(
            document(
                "quick-filters/document-create-quick-filter",
                preprocessRequest(Preprocessors.prettyPrint()),
                preprocessResponse(Preprocessors.prettyPrint()),
                pathParameters(PROJECT_ID_PATH_PARAMETER_DESCRIPTOR),
                requestFields(
                    CREATE_QUICK_FILTER_REQUEST_FIELD_DESCRIPTORS +
                        buildSearchMilestonesRequestFields("criteria.milestones") +
                        buildSearchTasksRequestFieldDescriptorsV2("criteria.tasks")),
                responseFields(QUICK_FILTER_RESPONSE_FIELD_DESCRIPTORS),
                responseHeaders(LOCATION_HEADER_DESCRIPTOR),
                QUICK_FILTER_LINK_DESCRIPTORS))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document update quick filter`() {

    val quickFilterIdentifier =
        quickFilterRepository
            .save(
                quickFilter(
                    participantCsm2Identifier,
                    "Quick filter",
                    LocalDate.now(),
                    LocalDate.now().plusDays(14)))
            .identifier

    val updateQuickFilterResource =
        buildCreateQuickFilterResource(
            name = "Quick filter updated",
            assigneeIds = setOf(participantCsmIdentifier),
            projectCraftIds = setOf(projectCraftIdentifier),
            workAreaIds = setOf(WorkAreaIdOrEmpty(workAreaIdentifier)),
            status = setOf(OPEN),
            from = LocalDate.now(),
            to = LocalDate.now().plusDays(21),
            topicCriticality = setOf(CRITICAL))

    val expectedVersion = 1L

    this.mockMvc
        .perform(
            requestBuilder(
                put(
                    latestVersionOf(QUICK_FILTER_ENDPOINT),
                    projectIdentifier,
                    quickFilterIdentifier),
                updateQuickFilterResource,
                0L))
        .andExpectAll(
            status().isOk,
            header().string(ETAG_HEADER, "\"$expectedVersion\""),
            jsonPath("$.name").value(updateQuickFilterResource.name),
            *hasIdentifierAndVersion(quickFilterIdentifier.toUuid(), expectedVersion),
            *isCreatedBy(testUser),
            *isLastModifiedBy(testUser),
            jsonPath("$.name").value(updateQuickFilterResource.name),
            jsonPath("$.useMilestoneCriteria").value(true),
            jsonPath("$.useTaskCriteria").value(true),
            jsonPath("$.highlight").value(false),
            jsonPath("$.criteria.milestones.from")
                .value(updateQuickFilterResource.criteria.milestones.from.toString()),
            jsonPath("$.criteria.milestones.to")
                .value(updateQuickFilterResource.criteria.milestones.to.toString()),
            jsonPath(
                "$.criteria.milestones.workAreas.workAreaIds",
                contains(workAreaIdentifier.toString())),
            jsonPath("$.criteria.milestones.workAreas.header").value(false),
            jsonPath("$.criteria.milestones.types.types").isEmpty,
            jsonPath(
                "$.criteria.milestones.types.projectCraftIds",
                contains(projectCraftIdentifier.toString())),
            jsonPath("$.criteria.tasks.from")
                .value(updateQuickFilterResource.criteria.tasks.from.toString()),
            jsonPath("$.criteria.tasks.to")
                .value(updateQuickFilterResource.criteria.tasks.to.toString()),
            jsonPath("$.criteria.tasks.workAreaIds", contains(workAreaIdentifier.toString())),
            jsonPath(
                "$.criteria.tasks.projectCraftIds", contains(projectCraftIdentifier.toString())),
            jsonPath("$.criteria.tasks.status", contains(OPEN.toString())),
            jsonPath(
                "$.criteria.tasks.assignees.participantIds",
                contains(participantCsmIdentifier.toString())),
            jsonPath("$.criteria.tasks.topicCriticality", contains(CRITICAL.toString())),
            jsonPath("$.createdBy.id").value(testUser.identifier.toString()),
            jsonPath("$.createdBy.displayName").value(testUser.getDisplayName()),
            jsonPath("$.lastModifiedBy.id").value(testUser.identifier.toString()),
            jsonPath("$.lastModifiedBy.displayName").value(testUser.getDisplayName()))
        .andDo(
            document(
                "quick-filters/document-update-quick-filter",
                preprocessRequest(Preprocessors.prettyPrint()),
                preprocessResponse(Preprocessors.prettyPrint()),
                pathParameters(
                    PROJECT_ID_PATH_PARAMETER_DESCRIPTOR, FILTER_ID_PATH_PARAMETER_DESCRIPTOR),
                requestFields(
                    CREATE_QUICK_FILTER_REQUEST_FIELD_DESCRIPTORS +
                        buildSearchMilestonesRequestFields("criteria.milestones") +
                        buildSearchTasksRequestFieldDescriptorsV2("criteria.tasks")),
                responseFields(QUICK_FILTER_RESPONSE_FIELD_DESCRIPTORS),
                QUICK_FILTER_LINK_DESCRIPTORS))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document find quick filters`() {

    val createQuickFilterResource1 =
        buildCreateQuickFilterResource(
            name = "1 Quick Filter",
            assigneeIds = setOf(participantCsm2Identifier),
            projectCraftIds = setOf(projectCraftIdentifier),
            workAreaIds = setOf(WorkAreaIdOrEmpty(workAreaIdentifier)),
            status = setOf(OPEN),
            from = LocalDate.now(),
            to = LocalDate.now().plusDays(14),
            topicCriticality = setOf(CRITICAL))
    val createQuickFilterResource2 =
        buildCreateQuickFilterResource(
            name = "2 Quick Filter", from = LocalDate.now(), to = LocalDate.now().plusDays(14))

    this.mockMvc.perform(
        requestBuilder(
            post(latestVersionOf(QUICK_FILTERS_ENDPOINT), projectIdentifier),
            createQuickFilterResource2))
    this.mockMvc.perform(
        requestBuilder(
            post(latestVersionOf(QUICK_FILTERS_ENDPOINT), projectIdentifier),
            createQuickFilterResource1))

    this.mockMvc
        .perform(
            requestBuilder(
                RestDocumentationRequestBuilders.get(
                    latestVersionOf(QUICK_FILTERS_ENDPOINT), projectIdentifier)))
        .andExpectAll(
            status().isOk,
            jsonPath("$.items.length()").value(2),
            jsonPath("$.items[0].name").value(createQuickFilterResource1.name),
            jsonPath("$.items[0].useTaskCriteria").value(true),
            jsonPath("$.items[0].highlight").value(false),
            jsonPath("$.items[0].criteria.milestones.from")
                .value(createQuickFilterResource1.criteria.milestones.from.toString()),
            jsonPath("$.items[0].criteria.milestones.to")
                .value(createQuickFilterResource1.criteria.milestones.to.toString()),
            jsonPath(
                "$.items[0].criteria.milestones.workAreas.workAreaIds",
                contains(workAreaIdentifier.toString())),
            jsonPath("$.items[0].criteria.milestones.workAreas.header").value(false),
            jsonPath("$.items[0].criteria.milestones.types.types").isEmpty,
            jsonPath(
                "$.items[0].criteria.milestones.types.projectCraftIds",
                contains(projectCraftIdentifier.toString())),
            jsonPath("$.items[0].criteria.tasks.from")
                .value(createQuickFilterResource1.criteria.tasks.from.toString()),
            jsonPath("$.items[0].criteria.tasks.to")
                .value(createQuickFilterResource1.criteria.tasks.to.toString()),
            jsonPath(
                "$.items[0].criteria.tasks.workAreaIds", contains(workAreaIdentifier.toString())),
            jsonPath(
                "$.items[0].criteria.tasks.projectCraftIds",
                contains(projectCraftIdentifier.toString())),
            jsonPath("$.items[0].criteria.tasks.status", contains(OPEN.toString())),
            jsonPath(
                "$.items[0].criteria.tasks.assignees.participantIds",
                contains(participantCsm2Identifier.toString())),
            jsonPath("$.items[0].criteria.tasks.topicCriticality", contains(CRITICAL.toString())),
            jsonPath("$.items[0].createdBy.id").value(testUser.identifier.toString()),
            jsonPath("$.items[0].createdBy.displayName").value(testUser.getDisplayName()),
            jsonPath("$.items[0].lastModifiedBy.id").value(testUser.identifier.toString()),
            jsonPath("$.items[0].lastModifiedBy.displayName").value(testUser.getDisplayName()),
            jsonPath("$.items[1].name").value(createQuickFilterResource2.name))
        .andDo(
            document(
                "quick-filters/document-get-quick-filters",
                preprocessRequest(Preprocessors.prettyPrint()),
                preprocessResponse(Preprocessors.prettyPrint()),
                pathParameters(PROJECT_ID_PATH_PARAMETER_DESCRIPTOR),
                QUICK_FILTER_LIST_RESPONSE_FIELD,
                QUICK_FILTER_LIST_LINK_DESCRIPTORS))

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify and document delete quick filter`() {
    val quickFilterIdentifier =
        quickFilterRepository
            .save(
                quickFilter(
                    participantCsm2Identifier,
                    "Quick filter 1",
                    LocalDate.now(),
                    LocalDate.now().plusDays(14)))
            .identifier

    this.mockMvc
        .perform(
            requestBuilder(
                delete(
                    latestVersionOf(QUICK_FILTER_ENDPOINT),
                    projectIdentifier,
                    quickFilterIdentifier)))
        .andExpect(status().isNoContent)
        .andDo(
            document(
                "quick-filters/document-delete-quick-filter",
                preprocessRequest(Preprocessors.prettyPrint()),
                preprocessResponse(Preprocessors.prettyPrint()),
                pathParameters(
                    PROJECT_ID_PATH_PARAMETER_DESCRIPTOR, FILTER_ID_PATH_PARAMETER_DESCRIPTOR)))

    projectEventStoreUtils.verifyEmpty()
  }

  private fun buildCreateQuickFilterResource(
      name: String,
      highlight: Boolean = false,
      assigneeIds: Set<ParticipantId> = emptySet(),
      companyIds: Set<UUID> = emptySet(),
      projectCraftIds: Set<ProjectCraftId> = emptySet(),
      workAreaIds: Set<WorkAreaIdOrEmpty> = emptySet(),
      status: Set<TaskStatusEnum> = emptySet(),
      from: LocalDate,
      to: LocalDate,
      topicCriticality: Set<TopicCriticalityEnum> = emptySet(),
      hasTopics: Boolean? = null,
  ): SaveQuickFilterResource =
      SaveQuickFilterResource(
          name = name,
          useMilestoneCriteria = true,
          useTaskCriteria = true,
          highlight = highlight,
          criteria =
              CriteriaResource(
                  milestones =
                      FilterMilestoneListResource(
                          types =
                              FilterMilestoneListResource.TypesFilter(
                                  kotlin.collections.emptySet(), projectCraftIds = projectCraftIds),
                          workAreas =
                              FilterMilestoneListResource.WorkAreaFilter(
                                  false, workAreaIds = workAreaIds),
                          from = from,
                          to = to),
                  tasks =
                      FilterTaskListResource(
                          FilterAssigneeResource(
                              participantIds = assigneeIds.toList(),
                              companyIds = companyIds.toList()),
                          projectCraftIds = projectCraftIds.toList(),
                          workAreaIds = workAreaIds.toList(),
                          from = LocalDate.now(),
                          to = LocalDate.now().plusDays(7),
                          topicCriticality = topicCriticality.toList(),
                          status = status.toList(),
                          hasTopics = hasTopics)))

  private fun quickFilter(
      participantIdentifier: ParticipantId,
      name: String,
      from: LocalDate,
      to: LocalDate
  ): QuickFilter =
      QuickFilter(
          identifier = QuickFilterId(),
          name = name,
          projectIdentifier = projectIdentifier,
          participantIdentifier = participantIdentifier,
          milestoneCriteria = MilestoneCriteria(from = from, to = to),
          taskCriteria = TaskCriteria(from = from, to = to))

  companion object {

    private val PROJECT_ID_PATH_PARAMETER_DESCRIPTOR =
        parameterWithName("projectId").description("ID of the project")

    private val FILTER_ID_PATH_PARAMETER_DESCRIPTOR =
        parameterWithName("quickFilterId").description("ID of the quick filter")

    private val QUICK_FILTER_RESPONSE_FIELD_DESCRIPTORS =
        listOf(
            *ABSTRACT_RESOURCE_FIELD_DESCRIPTORS,
            fieldWithPath("name").description("Name of the quick filter").type(STRING),
            fieldWithPath("highlight")
                .description(
                    "Whether the client should highlight filter results when this quick filter is applied.")
                .type(BOOLEAN),
            fieldWithPath("useMilestoneCriteria")
                .description(
                    "Whether the client should highlight filter results when useMilestoneCriteria is applied.")
                .type(BOOLEAN),
            fieldWithPath("useTaskCriteria")
                .description(
                    "Whether the client should highlight filter results when useTaskCriteria is applied.")
                .type(BOOLEAN),
            fieldWithPath("criteria")
                .description(
                    "Object with the quick filter criteria " +
                        "(please visit the request field of this operation for more documentation)"),
            subsectionWithPath("criteria.*").ignored(),
            subsectionWithPath("_links").ignored())

    private var CREATE_QUICK_FILTER_REQUEST_FIELD_DESCRIPTORS =
        listOf(
            ConstrainedFields(SaveQuickFilterResource::class.java)
                .withPath("name")
                .description("Name of the quick filter")
                .type(STRING),
            ConstrainedFields(SaveQuickFilterResource::class.java)
                .withPath("highlight")
                .description(
                    "Whether the client should highlight filter results when this quick filter is applied.")
                .type(BOOLEAN),
            ConstrainedFields(SaveQuickFilterResource::class.java)
                .withPath("useTaskCriteria")
                .description("Boolean indicating if the task filter criteria should be applied")
                .type(BOOLEAN),
            ConstrainedFields(SaveQuickFilterResource::class.java)
                .withPath("useMilestoneCriteria")
                .description(
                    "Boolean indicating if the milestone filter criteria should be applied")
                .type(BOOLEAN),
            ConstrainedFields(SaveQuickFilterResource::class.java)
                .withPath("criteria")
                .description("The criteria used for filtering the elements")
                .type(JsonFieldType.OBJECT),
            ConstrainedFields(CriteriaResource::class.java)
                .withPath("criteria.milestones")
                .description(
                    "The criteria used for filtering the milestones, " +
                        "more information can be found in the documentation of the search of milestones")
                .type(JsonFieldType.OBJECT),
            ConstrainedFields(CriteriaResource::class.java)
                .withPath("criteria.tasks")
                .description(
                    "The criteria used for filtering the tasks, " +
                        "more information can be found in the documentation of the search of tasks")
                .type(JsonFieldType.OBJECT),
        )

    private val QUICK_FILTER_LIST_RESPONSE_FIELD =
        responseFields(
                fieldWithPath("items[]").description("A list of quick filters").type(ARRAY),
                subsectionWithPath("_links").ignored())
            .andWithPrefix("items[].", QUICK_FILTER_RESPONSE_FIELD_DESCRIPTORS)

    private val QUICK_FILTER_LIST_LINK_DESCRIPTORS =
        links(
            linkWithRel(QuickFilterListResource.LINK_FILTER_CREATE)
                .description(
                    "Link to create a new quick filter is the limit of 100 filters was not reached"))

    private val QUICK_FILTER_LINK_DESCRIPTORS =
        links(
            linkWithRel(QuickFilterResource.LINK_FILTER_UPDATE)
                .description("Link to update the quick filter"),
            linkWithRel(QuickFilterResource.LINK_FILTER_DELETE)
                .description("Link to delete the quick filter"))
  }
}
