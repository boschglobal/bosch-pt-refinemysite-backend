/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.calendar.boundary

import com.bosch.pt.iot.smartsite.company.model.dto.EmployeeDto
import com.bosch.pt.iot.smartsite.company.repository.EmployeeRepository
import com.bosch.pt.iot.smartsite.project.calendar.api.CalendarExportParameters
import com.bosch.pt.iot.smartsite.project.calendar.boundary.model.dto.CalendarExportRowDto
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.rfv.boundary.RfvService
import com.bosch.pt.iot.smartsite.project.task.authorization.TaskAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.TaskFilterDto
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.COMMON_UNDERSTANDING
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.CUSTOM1
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.CUSTOM2
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.CUSTOM3
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.CUSTOM4
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.EQUIPMENT
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.EXTERNAL_FACTORS
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.INFORMATION
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.MATERIAL
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.PRELIMINARY_WORK
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.RESOURCES
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintEnum.SAFE_WORKING_ENVIRONMENT
import com.bosch.pt.iot.smartsite.project.taskconstraint.model.TaskConstraintSelection
import com.bosch.pt.iot.smartsite.project.taskconstraint.repository.TaskConstraintSelectionRepository
import com.bosch.pt.iot.smartsite.project.taskschedule.query.TaskScheduleQueryService
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleSlotWithDayCardDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleWithDayCardsDto
import com.google.common.collect.Lists
import datadog.trace.api.Trace
import java.util.UUID
import java.util.stream.Collectors
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class CalendarExportDataService(
    private val messageSource: MessageSource,
    private val taskRepository: TaskRepository,
    private val employeeRepository: EmployeeRepository,
    private val taskAuthorizationComponent: TaskAuthorizationComponent,
    private val taskConstraintSelectionRepository: TaskConstraintSelectionRepository,
    private val taskScheduleQueryService: TaskScheduleQueryService,
    private val rfvService: RfvService,
    @param:Value("\${db.in.max-size}") private val partitionSize: Int
) {

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProject(#projectIdentifier)")
  open fun generateExportRows(
      projectIdentifier: ProjectId,
      exportParameters: CalendarExportParameters
  ): List<CalendarExportRowDto> {
    val taskFilterDto =
        TaskFilterDto.buildForCalendar(
            projectIdentifier,
            exportParameters.status,
            exportParameters.projectCraftIds,
            exportParameters.workAreaIds,
            exportParameters.assignees.participantIds,
            exportParameters.assignees.companyIds,
            exportParameters.from,
            exportParameters.to,
            exportParameters.topicCriticality,
            exportParameters.hasTopics,
            exportParameters.allDaysInDateRange)
    val tasks = loadTasks(taskFilterDto)
    val taskConstraintSelections = loadTaskConstraintSelections(tasks)
    val taskToTaskScheduleMap = loadTaskSchedules(tasks)
    val userIdentifiers = collectUserIdentifiers(taskToTaskScheduleMap)
    val userIdentifierToCompanyNameMap =
        employeeRepository
            .findAllByUserIdentifierIn(userIdentifiers)
            .stream()
            .collect(Collectors.toMap(EmployeeDto::userIdentifier, EmployeeDto::companyName))
    val rfvs = rfvService.resolveProjectRfvs(projectIdentifier)

    val response: MutableList<CalendarExportRowDto> = ArrayList()
    for (task in tasks) {
      val slotsWithDayCardForTask =
          if (taskToTaskScheduleMap.containsKey(task.identifier))
              taskToTaskScheduleMap[task.identifier]!!.scheduleSlotsWithDayCards
          else ArrayList()
      if (slotsWithDayCardForTask.isEmpty()) {
        response.add(
            buildCalenderExportRow(task, filterConstraintsForTask(taskConstraintSelections, task)))
      } else {
        for (slotWithDayCard in slotsWithDayCardForTask) {
          response.add(
              buildCalenderExportRow(
                  taskConstraintSelections,
                  userIdentifierToCompanyNameMap,
                  task,
                  slotWithDayCard,
                  rfvs))
        }
      }
    }
    return response
  }

  private fun collectUserIdentifiers(
      taskToTaskScheduleMap: Map<TaskId, TaskScheduleWithDayCardsDto>
  ): Set<UUID> =
      taskToTaskScheduleMap.values
          .asSequence()
          .map { it.scheduleSlotsWithDayCards }
          .flatten()
          .map { slot: TaskScheduleSlotWithDayCardDto ->
            listOf(
                slot.slotsDayCardCreatedBy?.identifier, slot.slotsDayCardLastModifiedBy?.identifier)
          }
          .flatten()
          .filterNotNull()
          .toSet()

  private fun buildCalenderExportRow(
      task: Task,
      constraints: Set<TaskConstraintEnum>
  ): CalendarExportRowDto =
      CalendarExportRowDto(
          taskAssigneeCompanyName =
              if (task.assignee != null) task.assignee!!.company!!.getDisplayName() else null,
          taskAssigneeUserName =
              if (task.assignee != null) task.assignee!!.getDisplayName() else null,
          taskCraftName = task.projectCraft.getDisplayName(),
          taskDescription = task.description,
          taskIdentifier = task.identifier,
          taskLocation = task.location,
          taskName = task.name,
          taskStatus = translateEnumValue(task.status),
          taskWorkAreaName = if (task.workArea != null) task.workArea!!.getDisplayName() else null,
          taskStartDate = if (task.taskSchedule != null) task.taskSchedule!!.start else null,
          taskEndDate = if (task.taskSchedule != null) task.taskSchedule!!.end else null,
          taskConstraintCommonUnderstanding = constraints.contains(COMMON_UNDERSTANDING),
          taskConstraintEquipment = constraints.contains(EQUIPMENT),
          taskConstraintExternalFactors = constraints.contains(EXTERNAL_FACTORS),
          taskConstraintInformation = constraints.contains(INFORMATION),
          taskConstraintMaterial = constraints.contains(MATERIAL),
          taskConstraintPreliminaryWork = constraints.contains(PRELIMINARY_WORK),
          taskConstraintResources = constraints.contains(RESOURCES),
          taskConstraintSafeWorkingEnvironment = constraints.contains(SAFE_WORKING_ENVIRONMENT),
          taskConstraintCustom1 = constraints.contains(CUSTOM1),
          taskConstraintCustom2 = constraints.contains(CUSTOM2),
          taskConstraintCustom3 = constraints.contains(CUSTOM3),
          taskConstraintCustom4 = constraints.contains(CUSTOM4))

  private fun buildCalenderExportRow(
      constraintSelections: List<TaskConstraintSelection>,
      userIdentifierToCompanyNameMapping: Map<UUID, String>,
      task: Task,
      slotWithDayCard: TaskScheduleSlotWithDayCardDto,
      rfvs: Map<DayCardReasonEnum, String>
  ): CalendarExportRowDto =
      buildCalenderExportRow(task, filterConstraintsForTask(constraintSelections, task)).apply {
        dayCardCreatedAtTimestamp = slotWithDayCard.slotsDayCardCreatedDate
        dayCardCreatedByCompanyName =
            userIdentifierToCompanyNameMapping[slotWithDayCard.slotsDayCardCreatedBy?.toUuid()]

        dayCardCreatedByIdentifier = slotWithDayCard.slotsDayCardCreatedBy?.toUuid()

        dayCardDate = slotWithDayCard.slotsDate
        dayCardIdentifier = slotWithDayCard.slotsDayCardIdentifier
        dayCardLastModifiedAtTimestamp = slotWithDayCard.slotsDayCardLastModifiedDate
        dayCardLastModifiedByCompanyName =
            userIdentifierToCompanyNameMapping[slotWithDayCard.slotsDayCardLastModifiedBy?.toUuid()]

        dayCardLastModifiedByIdentifier = slotWithDayCard.slotsDayCardLastModifiedBy?.toUuid()

        dayCardManpower = slotWithDayCard.slotsDayCardManpower
        dayCardNotes = slotWithDayCard.slotsDayCardNotes
        dayCardReason = slotWithDayCard.slotsDayCardReason?.let { rfvs[it] }
        dayCardStatus = translateEnumValue(slotWithDayCard.slotsDayCardStatus)
        dayCardTitle = slotWithDayCard.slotsDayCardTitle
      }

  private fun loadTasks(filter: TaskFilterDto): List<Task> {
    val taskIds =
        taskRepository
            .findTaskIdentifiersForFilters(filter, PageRequest.of(0, Int.MAX_VALUE))
            .toMutableList()
    removeTasksWithoutViewPermission(taskIds)
    return Lists.partition(taskIds, partitionSize)
        .map { taskRepository.findAllForCalendarByIdentifierIn(it) }
        .flatten()
        .sortedWith(Comparator.comparingInt { taskIds.indexOf(it.identifier) })
  }

  private fun loadTaskSchedules(tasks: List<Task>): Map<TaskId, TaskScheduleWithDayCardsDto> =
      taskScheduleQueryService
          .findByTaskIdentifiers(tasks.map { requireNotNull(it.identifier) })
          .toSet()
          .associateBy { it.taskIdentifier }

  private fun loadTaskConstraintSelections(tasks: List<Task>): List<TaskConstraintSelection> =
      taskConstraintSelectionRepository.findAllWithDetailsByTaskIdentifierIn(
          tasks.map { requireNotNull(it.identifier) }.toSet())

  private fun removeTasksWithoutViewPermission(taskIdentifiers: MutableList<TaskId>) {
    val permittedIdentifiers =
        taskAuthorizationComponent.filterTasksWithViewPermission(HashSet(taskIdentifiers))
    taskIdentifiers.retainAll(permittedIdentifiers)
  }

  private fun filterConstraintsForTask(
      taskConstraintSelections: List<TaskConstraintSelection>,
      task: Task
  ): Set<TaskConstraintEnum> =
      taskConstraintSelections
          .filter { it.task.identifier == task.identifier }
          .map { it.constraints }
          .flatten()
          .toSet()

  private fun translateEnumValue(dayCardStatus: DayCardStatusEnum): String =
      messageSource.getMessage(
          "DayCardStatusEnum_" + dayCardStatus.name, arrayOf(), LocaleContextHolder.getLocale())

  private fun translateEnumValue(taskStatus: TaskStatusEnum): String =
      messageSource.getMessage(
          "TaskStatusEnum_" + taskStatus.name, arrayOf(), LocaleContextHolder.getLocale())
}
