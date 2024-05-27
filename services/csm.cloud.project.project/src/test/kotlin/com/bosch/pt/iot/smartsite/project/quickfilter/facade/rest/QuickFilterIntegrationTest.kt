/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest

import com.bosch.pt.csm.cloud.common.command.exceptions.EntityOutdatedException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.i18n.Key.QUICK_FILTER_VALIDATION_ERROR_MAX_NUMBER_REACHED
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.FilterMilestoneListResource
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.CRAFT
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.INVESTOR
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.PROJECT
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.asParticipantId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.quickfilter.domain.asQuickFilterId
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.request.SaveQuickFilterResource
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.request.SaveQuickFilterResource.CriteriaResource
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.response.QuickFilterListResource
import com.bosch.pt.iot.smartsite.project.quickfilter.model.MilestoneCriteria
import com.bosch.pt.iot.smartsite.project.quickfilter.model.QuickFilter
import com.bosch.pt.iot.smartsite.project.quickfilter.model.TaskCriteria
import com.bosch.pt.iot.smartsite.project.quickfilter.repository.QuickFilterRepository
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.FilterTaskListResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request.FilterTaskListResource.FilterAssigneeResource
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import com.bosch.pt.iot.smartsite.util.withMessageKey
import java.time.LocalDate.now
import java.util.UUID
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class QuickFilterIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: QuickFilterController

  @Autowired private lateinit var quickFilterRepository: QuickFilterRepository

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }
  private val workAreaIdentifier by lazy { getIdentifier("workArea").asWorkAreaId() }
  private val projectCraftIdentifier by lazy { getIdentifier("projectCraft").asProjectCraftId() }
  private val participantIdentifier by lazy { getIdentifier("participantCsm1").asParticipantId() }
  private val quickFilterIdentifier by lazy {
    quickFilterRepository.save(quickFilter("Test Filter")).identifier
  }
  private lateinit var quickFilterResource: SaveQuickFilterResource

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication("userCsm1")

    quickFilterResource =
        SaveQuickFilterResource(
            name = "My filter updated",
            highlight = false,
            useMilestoneCriteria = false,
            useTaskCriteria = false,
            criteria =
                CriteriaResource(
                    milestones =
                        FilterMilestoneListResource(
                            from = now(),
                            to = now().plusDays(7),
                        ),
                    tasks =
                        FilterTaskListResource(
                            from = now(), to = now().plusDays(7), hasTopics = false)))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify create quick filter works without any value defined`() {

    val criteria = cut.createQuickFilter(projectIdentifier, quickFilterResource).body!!.criteria

    assertThat(criteria.tasks.assignees.participantIds).isEmpty()
    assertThat(criteria.tasks.assignees.companyIds).isEmpty()
    assertThat(criteria.milestones.types.projectCraftIds).isEmpty()
    assertThat(criteria.milestones.workAreas.workAreaIds).isEmpty()
    assertThat(criteria.tasks.status).isEmpty()
    assertThat(criteria.tasks.from).isEqualTo(now())
    assertThat(criteria.tasks.to).isEqualTo(now().plusDays(7))
    assertThat(criteria.tasks.topicCriticality).isEmpty()
    assertThat(criteria.tasks.hasTopics).isFalse
  }

  @Test
  fun `verify create quick filter with a list that contain 100 elements fail`() {

    createQuickFilters(100)

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.createQuickFilter(projectIdentifier, quickFilterResource) }
        .withMessageKey(QUICK_FILTER_VALIDATION_ERROR_MAX_NUMBER_REACHED)
  }

  @Test
  fun `verify find quick filter retrieves the list of filters by name ascending`() {

    quickFilterRepository.save(quickFilter("2 My filter"))
    quickFilterRepository.save(quickFilter("3 My filter"))
    quickFilterRepository.save(quickFilter("1 My filter"))

    val filters = cut.findQuickFilters(projectIdentifier).body!!

    assertThat(filters.items.size).isEqualTo(3)
    assertThat(filters.items.elementAt(0).name).isEqualTo("1 My filter")
    assertThat(filters.items.elementAt(1).name).isEqualTo("2 My filter")
    assertThat(filters.items.elementAt(2).name).isEqualTo("3 My filter")
  }

  @Test
  fun `verify find quick filter with 100 elements doesn't contain the create link`() {

    createQuickFilters(99)

    var filters = cut.findQuickFilters(projectIdentifier).body!!
    assertThat(filters.items.size).isEqualTo(99)
    assertThat(filters.links.getLink(QuickFilterListResource.LINK_FILTER_CREATE)).isNotEmpty

    quickFilterRepository.save(quickFilter("100"))

    filters = cut.findQuickFilters(projectIdentifier).body!!
    assertThat(filters.items.size).isEqualTo(100)
    assertThat(filters.links.getLink(QuickFilterListResource.LINK_FILTER_CREATE)).isEmpty
  }

  @Test
  fun `verify update quick filter fails with wrong ETag`() {
    assertThatExceptionOfType(EntityOutdatedException::class.java).isThrownBy {
      cut.updateQuickFilter(
          quickFilterIdentifier, projectIdentifier, quickFilterResource, ETag.from("1"))
    }
  }

  @Test
  fun `verify the cleanup of the filter removes all non-existing criteria elements on create`() {
    val projectCraftIdentifiers = setOf(projectCraftIdentifier, ProjectCraftId(), ProjectCraftId())
    val workAreaIdentifiers =
        setOf(
            WorkAreaIdOrEmpty(workAreaIdentifier),
            WorkAreaIdOrEmpty(WorkAreaId()),
            WorkAreaIdOrEmpty(WorkAreaId()))

    val criteriaResource =
        CriteriaResource(
            milestones =
                FilterMilestoneListResource(
                    from = now(),
                    to = now().plusDays(7),
                    workAreas =
                        FilterMilestoneListResource.WorkAreaFilter(
                            header = false, workAreaIds = workAreaIdentifiers),
                    types =
                        FilterMilestoneListResource.TypesFilter(
                            types = setOf(PROJECT, INVESTOR, CRAFT),
                            projectCraftIds = projectCraftIdentifiers),
                ),
            tasks =
                FilterTaskListResource(
                    from = now(),
                    to = now().plusDays(7),
                    workAreaIds = workAreaIdentifiers.toList(),
                    allDaysInDateRange = true,
                    hasTopics = false))

    val saveQuickFilterResource =
        SaveQuickFilterResource(
            name = "My filter",
            highlight = false,
            useMilestoneCriteria = false,
            useTaskCriteria = false,
            criteria = criteriaResource)

    val criteria = cut.createQuickFilter(projectIdentifier, saveQuickFilterResource).body!!.criteria

    assertThat(criteria.milestones.types.types).containsExactlyInAnyOrder(PROJECT, INVESTOR, CRAFT)
    assertThat(criteria.milestones.types.projectCraftIds).containsExactly(projectCraftIdentifier)
    assertThat(criteria.milestones.workAreas.header).isFalse
    assertThat(criteria.tasks.allDaysInDateRange).isTrue
    assertThat(criteria.tasks.hasTopics).isFalse
  }

  @Test
  fun `verify the cleanup of the filter removes all non-existing criteria elements on update`() {
    val projectCraftIdentifiers = setOf(projectCraftIdentifier, ProjectCraftId(), ProjectCraftId())
    val workAreaIdentifiers =
        setOf(
            WorkAreaIdOrEmpty(workAreaIdentifier),
            WorkAreaIdOrEmpty(WorkAreaId()),
            WorkAreaIdOrEmpty(WorkAreaId()))

    val criteriaResource =
        CriteriaResource(
            milestones =
                FilterMilestoneListResource(
                    from = now(),
                    to = now().plusDays(7),
                    workAreas =
                        FilterMilestoneListResource.WorkAreaFilter(
                            header = false, workAreaIds = workAreaIdentifiers),
                    types =
                        FilterMilestoneListResource.TypesFilter(
                            types = setOf(PROJECT, INVESTOR, CRAFT),
                            projectCraftIds = projectCraftIdentifiers),
                ),
            tasks =
                FilterTaskListResource(
                    from = now(),
                    to = now().plusDays(7),
                    workAreaIds = workAreaIdentifiers.toList(),
                    allDaysInDateRange = true,
                    hasTopics = false))

    val saveQuickFilterResource =
        SaveQuickFilterResource(
            name = "My filter",
            highlight = false,
            useMilestoneCriteria = false,
            useTaskCriteria = false,
            criteria = criteriaResource)

    val criteria =
        cut.updateQuickFilter(
                quickFilterIdentifier, projectIdentifier, saveQuickFilterResource, ETag.from("0"))
            .body!!
            .criteria

    assertThat(criteria.milestones.types.types).containsExactlyInAnyOrder(PROJECT, INVESTOR, CRAFT)
    assertThat(criteria.milestones.types.projectCraftIds).containsExactly(projectCraftIdentifier)
    assertThat(criteria.milestones.workAreas.header).isFalse
    assertThat(criteria.tasks.allDaysInDateRange).isTrue
    assertThat(criteria.tasks.hasTopics).isFalse
  }

  @Test
  fun `verify the cleanup of the filter doesn't remove empty work area criteria`() {

    val workAreaIdentifiers =
        setOf(
            WorkAreaIdOrEmpty(workAreaIdentifier),
            WorkAreaIdOrEmpty(),
            WorkAreaIdOrEmpty(WorkAreaId()),
            WorkAreaIdOrEmpty(WorkAreaId()))

    val criteriaResource =
        CriteriaResource(
            milestones =
                FilterMilestoneListResource(
                    workAreas =
                        FilterMilestoneListResource.WorkAreaFilter(
                            header = false, workAreaIds = workAreaIdentifiers)),
            tasks = FilterTaskListResource())

    val saveQuickFilterResource =
        SaveQuickFilterResource(
            name = "My filter",
            highlight = false,
            useMilestoneCriteria = false,
            useTaskCriteria = false,
            criteria = criteriaResource)

    val criteria = cut.createQuickFilter(projectIdentifier, saveQuickFilterResource).body!!.criteria

    assertThat(criteria.milestones.workAreas.workAreaIds)
        .containsExactly(WorkAreaIdOrEmpty(workAreaIdentifier), WorkAreaIdOrEmpty())
  }

  @Test
  fun `verify the cleanup of the filter doesn't remove inactive assigneeId criteria`() {
    eventStreamGenerator
        .submitUser("userFmInactive")
        .submitEmployee("employeeFmInactive") { it.roles = listOf(EmployeeRoleEnumAvro.FM) }
        .submitParticipantG3("participantFmInactive") {
          it.status = ParticipantStatusEnumAvro.INACTIVE
          it.role = ParticipantRoleEnumAvro.FM
        }

    val participantFmInactiveIdentifier = getIdentifier("participantFmInactive").asParticipantId()

    val assigneeIdentifiers =
        setOf(participantFmInactiveIdentifier, ParticipantId(), ParticipantId())

    val criteriaResource =
        CriteriaResource(
            milestones = FilterMilestoneListResource(),
            tasks =
                FilterTaskListResource(
                    assignees =
                        FilterAssigneeResource(participantIds = assigneeIdentifiers.toList())))

    val saveQuickFilterResource =
        SaveQuickFilterResource(
            name = "My filter",
            highlight = false,
            useMilestoneCriteria = false,
            useTaskCriteria = false,
            criteria = criteriaResource)

    val criteria = cut.createQuickFilter(projectIdentifier, saveQuickFilterResource).body!!.criteria

    assertThat(criteria.tasks.assignees.participantIds)
        .containsExactly(participantFmInactiveIdentifier)
  }

  private fun createQuickFilters(numberOfQuickFilters: Int) {
    for (i in 0 until numberOfQuickFilters) {
      quickFilterRepository.save(quickFilter(i.toString()))
    }
  }

  private fun quickFilter(
      name: String,
      identifier: UUID = randomUUID(),
  ) =
      QuickFilter(
          identifier = identifier.asQuickFilterId(),
          name = name,
          projectIdentifier = projectIdentifier,
          participantIdentifier = participantIdentifier,
          taskCriteria = TaskCriteria(from = now(), to = now().plusDays(5)),
          milestoneCriteria = MilestoneCriteria(from = now(), to = now().plusDays(5)))
}
