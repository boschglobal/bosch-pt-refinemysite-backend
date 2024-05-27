/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.calendar.boundary

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.iot.smartsite.common.i18n.Key.PROJECT_VALIDATION_ERROR_PROJECT_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.calendar.api.CalendarExportParameters
import com.bosch.pt.iot.smartsite.project.calendar.boundary.CalendarExportHtmlService.CalendarTemplate.CALENDAR
import com.bosch.pt.iot.smartsite.project.calendar.boundary.CalendarExportHtmlService.CalendarTemplate.EMPTY_CALENDAR
import com.bosch.pt.iot.smartsite.project.calendar.boundary.assembler.CalendarAssembler
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.Calendar
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.Page
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.CRAFT
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.INVESTOR
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum.PROJECT
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneRepository
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.dto.MilestoneFilterDto
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElement
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.MILESTONE
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum.FINISH_TO_START
import com.bosch.pt.iot.smartsite.project.relation.repository.RelationRepository
import com.bosch.pt.iot.smartsite.project.relation.repository.dto.RelationFilterDto
import com.bosch.pt.iot.smartsite.project.task.authorization.TaskAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.TaskFilterDto
import com.bosch.pt.iot.smartsite.project.taskconstraint.boundary.TaskConstraintSelectionService
import com.bosch.pt.iot.smartsite.project.taskschedule.query.TaskScheduleQueryService
import com.bosch.pt.iot.smartsite.project.workday.shared.repository.WorkdayConfigurationRepository
import datadog.trace.api.Trace
import io.opentracing.util.GlobalTracer
import kotlin.Int.Companion.MAX_VALUE
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

@Service
open class CalendarExportHtmlService(
    private val templateEngine: TemplateEngine,
    private val projectRepository: ProjectRepository,
    private val workdayConfigurationRepository: WorkdayConfigurationRepository,
    private val taskRepository: TaskRepository,
    private val taskAuthorizationComponent: TaskAuthorizationComponent,
    private val taskConstraintSelectionService: TaskConstraintSelectionService,
    private val taskScheduleQueryService: TaskScheduleQueryService,
    private val milestoneRepository: MilestoneRepository,
    private val relationRepository: RelationRepository,
    private val calendarAssembler: CalendarAssembler,
    @Value("\${db.in.max-size}") private val partitionSize: Int
) {

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProject(#projectIdentifier)")
  open fun generateHtml(
      calendarExportParameters: CalendarExportParameters,
      projectIdentifier: ProjectId
  ): String {
    val project = loadProjectOrFail(projectIdentifier)
    val workdayConfiguration = loadWorkdayConfigurationOrFail(project)

    val taskFilterDto = calendarExportParameters.toTaskFilterDto(projectIdentifier)
    val milestoneFilterDto = calendarExportParameters.toMilestoneFilterDto(projectIdentifier)

    val tasks = loadTasks(taskFilterDto)
    val milestones = loadMilestones(milestoneFilterDto, calendarExportParameters.includeMilestones)

    // Check if there is any task or milestone to be represented in the pdf
    if (isAnyElementVisibleInCalendar(tasks, milestones)) {
      val taskIdentifiers = tasks.getTaskIdentifiers()
      val milestoneIdentifiers = milestones.getMilestoneIdentifiers()

      val constraintSelections = loadTaskConstraintSelections(taskIdentifiers.map { it }.toSet())
      val schedules = loadSchedules(taskIdentifiers, calendarExportParameters.includeDayCards)
      val criticalMilestoneRelations =
          loadCriticalMilestoneRelations(milestoneIdentifiers, projectIdentifier)

      val calendar =
          calendarAssembler.assemble(
              project,
              workdayConfiguration,
              calendarExportParameters.from,
              calendarExportParameters.to,
              tasks,
              constraintSelections,
              schedules,
              milestones,
              criticalMilestoneRelations,
              taskFilterDto.hasCalendarFiltersApplied(),
              calendarExportParameters.includeDayCards,
              calendarExportParameters.includeMilestones)

      val html = generateHtml(calendar)

      // add performance-relevant tags to trace
      GlobalTracer.get()
          .activeSpan()
          .setTag("custom.export.project", projectIdentifier.toString())
          .setTag("custom.export.from", calendarExportParameters.from.toString())
          .setTag("custom.export.to", calendarExportParameters.to.toString())
          .setTag("custom.export.includeDayCards", calendarExportParameters.includeDayCards)
          .setTag("custom.export.includeMilestones", calendarExportParameters.includeMilestones)
          .setTag("custom.export.tasks_count", tasks.size)
          .setTag("custom.export.schedules_count", schedules.size)
          .setTag("custom.export.milestones_count", milestones.size)
          .setTag(
              "custom.export.daycards_count", schedules.sumOf { it.scheduleSlotsWithDayCards.size })
          .setTag("custom.export.html_size_bytes", html.toByteArray().size)

      return html
    } else {
      val emptyCalendar =
          calendarAssembler.assembleEmpty(
              project,
              workdayConfiguration,
              calendarExportParameters.from,
              calendarExportParameters.to,
              taskFilterDto.hasCalendarFiltersApplied(),
              calendarExportParameters.includeDayCards,
              calendarExportParameters.includeMilestones)

      return generateHtml(emptyCalendar, template = EMPTY_CALENDAR)
    }
  }

  private fun loadProjectOrFail(projectIdentifier: ProjectId) =
      projectRepository.findOneByIdentifier(projectIdentifier)
          ?: throw AggregateNotFoundException(
              PROJECT_VALIDATION_ERROR_PROJECT_NOT_FOUND, projectIdentifier.toString())

  private fun loadWorkdayConfigurationOrFail(project: Project) =
      requireNotNull(
          workdayConfigurationRepository.findOneWithDetailsByProjectIdentifier(project.identifier))

  private fun loadTasks(filter: TaskFilterDto): Collection<Task> {
    val taskIdentifiers =
        taskRepository
            .findTaskIdentifiersForFilters(filter, PageRequest.of(0, MAX_VALUE))
            .toMutableList()

    val permittedTaskIdentifiers =
        taskAuthorizationComponent.filterTasksWithViewPermission(taskIdentifiers.toSet())
    taskIdentifiers.retainAll(permittedTaskIdentifiers)

    return taskIdentifiers
        .chunked(partitionSize)
        .flatMap(taskRepository::findAllForCalendarByIdentifierIn)
  }

  private fun loadTaskConstraintSelections(taskIdentifiers: Set<TaskId>) =
      if (taskIdentifiers.isNotEmpty())
          taskConstraintSelectionService.findSelections(taskIdentifiers)
      else emptyList()

  private fun loadSchedules(taskIdentifiers: Set<TaskId>, includeDayCards: Boolean = false) =
      if (taskIdentifiers.isNotEmpty() && includeDayCards)
          taskScheduleQueryService.findByTaskIdentifiers(taskIdentifiers)
      else emptyList()

  private fun loadMilestones(
      filter: MilestoneFilterDto,
      includeMilestones: Boolean = false
  ): List<Milestone> {
    if (includeMilestones.not()) return emptyList()

    val milestoneIds =
        milestoneRepository.findMilestoneIdentifiersForFilters(filter, PageRequest.of(0, MAX_VALUE))

    return milestoneIds
        .chunked(partitionSize)
        .flatMap(milestoneRepository::findAllWithDetailsByIdentifierIn)
  }

  private fun loadCriticalMilestoneRelations(
      milestonesIdentifiers: Set<MilestoneId>,
      projectIdentifier: ProjectId,
  ): List<Relation> {
    if (milestonesIdentifiers.isEmpty()) return emptyList()

    // The criticality is always null for relation types other than FINISH_TO_START
    val filters =
        RelationFilterDto(
            types = setOf(FINISH_TO_START),
            sources = milestonesIdentifiers.map { RelationElement(it.toUuid(), MILESTONE) }.toSet(),
            targets = milestonesIdentifiers.map { RelationElement(it.toUuid(), MILESTONE) }.toSet(),
            projectIdentifier = projectIdentifier)

    val relationIds = relationRepository.findForFilters(filters, PageRequest.of(0, MAX_VALUE))

    return relationIds
        .chunked(partitionSize)
        .flatMap(relationRepository::findAllWithDetailsByIdentifierIn)
        .filter { it.isCritical() }
  }

  private fun generateHtml(calendar: Calendar, template: CalendarTemplate = CALENDAR): String {
    val context = Context(LocaleContextHolder.getLocale())
    context.setVariable(PAGE, Page(calendar))

    return templateEngine.process(template.fileName, context)
  }

  private fun isAnyElementVisibleInCalendar(
      tasks: Collection<Task>,
      milestones: Collection<Milestone>
  ) = tasks.isNotEmpty() || milestones.isNotEmpty()

  private fun CalendarExportParameters.toTaskFilterDto(projectIdentifier: ProjectId) =
      TaskFilterDto.buildForCalendar(
          projectRef = projectIdentifier,
          taskStatus = status,
          projectCraftIds = projectCraftIds,
          workAreaIds = workAreaIds,
          assignedParticipants = assignees.participantIds,
          assignedCompanies = assignees.companyIds,
          rangeStartDate = from,
          rangeEndDate = to,
          topicCriticality = topicCriticality,
          hasTopics = hasTopics,
          allDaysInDateRange = allDaysInDateRange)

  private fun CalendarExportParameters.toMilestoneFilterDto(projectIdentifier: ProjectId) =
      MilestoneFilterDto(
          projectIdentifier = projectIdentifier,
          types = setOf(PROJECT, INVESTOR, CRAFT),
          craftIdentifiers = projectCraftIds.toSet(),
          workAreaIdentifiers = workAreaIds.toSet(),
          rangeStartDate = from,
          rangeEndDate = to)

  private fun Collection<Task>.getTaskIdentifiers() =
      this.map { requireNotNull(it.identifier) }.toSet()

  private fun Collection<Milestone>.getMilestoneIdentifiers() =
      this.map { requireNotNull(it.identifier) }.toSet()

  private fun Relation.isCritical() = this.critical != null && this.critical == true

  private enum class CalendarTemplate(val fileName: String) {
    /** an empty calendar without any tasks or milestones */
    EMPTY_CALENDAR("calendar-empty.html"),
    CALENDAR("calendar.html")
  }

  companion object {
    private const val PAGE = "page"
  }
}
