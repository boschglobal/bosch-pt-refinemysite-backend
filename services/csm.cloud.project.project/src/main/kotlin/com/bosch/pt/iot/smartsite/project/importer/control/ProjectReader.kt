/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.control

import com.bosch.pt.iot.smartsite.project.importer.boundary.ImportContext
import com.bosch.pt.iot.smartsite.project.importer.boundary.assembler.CraftDioAssembler
import com.bosch.pt.iot.smartsite.project.importer.boundary.assembler.CraftDioAssemblerUtils.filterCraftColumn
import com.bosch.pt.iot.smartsite.project.importer.boundary.assembler.DioAssembler.Companion.addToContext
import com.bosch.pt.iot.smartsite.project.importer.boundary.assembler.MilestoneDioAssembler
import com.bosch.pt.iot.smartsite.project.importer.boundary.assembler.RelationDioAssembler
import com.bosch.pt.iot.smartsite.project.importer.boundary.assembler.TaskDioAssembler
import com.bosch.pt.iot.smartsite.project.importer.boundary.assembler.TaskScheduleDioAssembler
import com.bosch.pt.iot.smartsite.project.importer.boundary.assembler.WorkAreaDioAssembler
import com.bosch.pt.iot.smartsite.project.importer.boundary.assembler.WorkAreaDioAssemblerUtils.filterWorkAreaColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ActivityCodeColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.CustomFieldColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ResourcesColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.TaskFieldColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.UserDefinedFieldColumn
import com.bosch.pt.iot.smartsite.project.importer.model.ImportModel
import com.bosch.pt.iot.smartsite.project.importer.model.dio.MilestoneDio
import com.bosch.pt.iot.smartsite.project.importer.model.dio.ProjectDio
import com.bosch.pt.iot.smartsite.project.importer.model.dio.TaskDio
import com.bosch.pt.iot.smartsite.project.importer.model.dio.TaskScheduleDio
import com.bosch.pt.iot.smartsite.project.importer.model.dio.WorkAreaDio
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.ProjectIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.WorkAreaIdentifier
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.workday.shared.model.Holiday
import datadog.trace.api.Trace
import java.time.DayOfWeek
import java.util.concurrent.atomic.AtomicInteger
import net.sf.mpxj.DataType
import net.sf.mpxj.ProjectCalendarException
import net.sf.mpxj.ProjectFile
import net.sf.mpxj.TaskField
import net.sf.mpxj.TimeUnit
import net.sf.mpxj.UserDefinedField
import org.springframework.stereotype.Component

@Component
class ProjectReader(
    private val craftDioAssembler: CraftDioAssembler,
    private val milestoneDioAssembler: MilestoneDioAssembler,
    private val relationDioAssembler: RelationDioAssembler,
    private val taskDioAssembler: TaskDioAssembler,
    private val taskScheduleDioAssembler: TaskScheduleDioAssembler,
    private val workAreaDioAssembler: WorkAreaDioAssembler
) {

  @Trace
  fun read(
      project: Project,
      projectFile: ProjectFile,
      importContext: ImportContext,
      readWorkAreasRecursive: Boolean,
      craftColumnName: String?,
      craftColumnFieldType: String?,
      workAreaColumnName: String?,
      workAreaColumnFieldType: String?
  ): ImportModel {

    val columns = readColumns(projectFile)
    val workAreaColumn =
        workAreaColumnName?.let { filterWorkAreaColumn(columns, it, workAreaColumnFieldType) }
    val craftColumn = craftColumnName?.let { filterCraftColumn(columns, it, craftColumnFieldType) }

    val projectId = ProjectIdentifier(1)
    val projectDio = ProjectDio(projectId, project.identifier.identifier)
    addToContext(importContext, projectDio)

    val placeholderWorkAreaCount = AtomicInteger(1)
    val workAreas =
        workAreaDioAssembler
            .assemble(
                projectFile,
                importContext,
                projectId,
                workAreaColumn,
                readWorkAreasRecursive,
                placeholderWorkAreaCount)
            .also { addToContext(importContext, it) }

    val placeholderCraftCount = AtomicInteger(1)
    val placeholderCraft =
        craftDioAssembler.assemblePlaceholderCraft(
            craftColumn, projectFile.tasks, projectId, placeholderCraftCount)
    val crafts =
        craftDioAssembler
            .assemble(projectFile, projectId, craftColumn, placeholderCraftCount)
            .also { addToContext(importContext, it) }
            .toMutableList()
            .also {
              addToContext(importContext, placeholderCraft)
              it.add(placeholderCraft)
            }

    val tasks =
        taskDioAssembler
            .assemble(
                projectFile,
                importContext,
                craftColumn,
                crafts,
                readWorkAreasRecursive,
                workAreaColumn)
            .also { addToContext(importContext, it) }

    val taskSchedules =
        taskScheduleDioAssembler.assemble(projectFile, importContext).also {
          addToContext(importContext, it)
        }

    val milestones =
        milestoneDioAssembler
            .assemble(
                projectFile,
                projectId,
                importContext,
                craftColumn,
                crafts,
                readWorkAreasRecursive,
                workAreaColumn)
            .also { addToContext(importContext, it) }

    val relations = relationDioAssembler.assemble(projectFile, importContext)

    // Optimization to not create the placeholder craft if it isn't used anywhere
    if (tasks.none { it.craftId == placeholderCraft.id } &&
        milestones.none { it.craftId == placeholderCraft.id }) {
      crafts.remove(placeholderCraft)
      importContext.map.remove(placeholderCraft.id)
    }

    // Optimization to not create working areas that are not referenced by tasks
    val workAreaIds = workAreas.map { it.id }.toMutableSet()
    filterUnusedWorkAreasFromTasks(workAreaIds, tasks)
    filterUnusedWorkAreasFromMilestones(workAreaIds, milestones)
    filterUnusedWorkAreasFromWorkAreas(importContext, workAreaIds, workAreas)
    val filteredWorkingAreas = workAreas.filter { !workAreaIds.contains(it.id) }

    val workDays = readWorkDaysFromFile(projectFile)
    val holidays: List<Holiday> = readHolidaysFromFile(projectFile).distinct()
    val hasWorkOnNonWorkDays = hasWorkOnNonWorkDays(taskSchedules, workDays)

    return ImportModel(
        projectId,
        filteredWorkingAreas,
        crafts,
        tasks,
        taskSchedules,
        milestones,
        relations,
        workDays,
        holidays,
        hasWorkOnNonWorkDays)
  }

  @Trace
  fun readColumns(projectFile: ProjectFile): List<ImportColumn> {
    val columns = mutableSetOf<ImportColumn>()

    // Read columns from table if exists (only in case it's a MS Project file)
    columns.addAll(
        projectFile.tables
            .map { table ->
              table.columns
                  .filter { it.fieldType is TaskField && it.fieldType.dataType == DataType.STRING }
                  .filter { it.title != "WBS" } // Hide WBS column
                  .map { TaskFieldColumn(it.title, it.fieldType) }
            }
            .flatten())

    // Read columns from custom fields
    val columnNamesFromTable: List<String> = columns.map { it.name }
    val columnsFromCustomFields =
        projectFile.customFields
            .filter { it.alias != null }
            // Filter out user defined fields as they have to be mapped separately
            .filter { it.fieldType !is UserDefinedField }
            .map { CustomFieldColumn(it.alias, it.fieldType) }
            .filter { !columnNamesFromTable.contains(it.name) }
    columns.addAll(columnsFromCustomFields)

    // Read columns from user defined fields

    // Occasionally there are duplicate columns where we have to choose the one
    // which contains data for the tasks.

    // Find duplicate column names of duplicate columns
    val userDefinedFieldsDuplicateNames =
        projectFile.userDefinedFields
            .filter { it.name != null }
            .map { UserDefinedFieldColumn(it.name, it) }
            .groupingBy { it.name }
            .eachCount()
            .filter { it.value > 1 }
            .keys

    // Get corresponding duplicate columns
    val userDefinedFieldsDuplicates =
        projectFile.userDefinedFields.filter { userDefinedFieldsDuplicateNames.contains(it.name) }

    // For each duplicate find the correct one that contains data
    val userDefinedFieldsDeduplicatedColumnsWithValues =
        userDefinedFieldsDuplicates
            .associateWith { field -> projectFile.tasks.any { it.get(field) != null } }
            .filter { it.value }
            .map { UserDefinedFieldColumn(it.key.name, it.key) }

    // Collect all non-duplicate user-defined-fields
    val userDefinedFields =
        projectFile.userDefinedFields
            .filter { it.name != null }
            .filter { !userDefinedFieldsDuplicates.contains(it) }
            .map { UserDefinedFieldColumn(it.name, it) }

    // Take de-duplicated and non-duplicate columns as valid columns
    val columnsFromUserDefinedFields =
        userDefinedFieldsDeduplicatedColumnsWithValues + userDefinedFields

    columns.addAll(columnsFromUserDefinedFields)

    // If it's an MS project or PP file then just take the columns from the tables
    // and custom fields.
    if (columns.isNotEmpty()) {
      return columns.sortedBy { it.name }
    }

    // Read columns from activity codes
    columns.addAll(
        projectFile.activityCodes
            .filter { it.name != null }
            .map { code -> ActivityCodeColumn(code.name, code) })

    // If the project has resources defined add an artificial column to select as it could
    // be a column containing crafts or working areas.
    if (columns.none { it.fieldType == TaskField.RESOURCE_NAMES } &&
        projectFile.resources.isNotEmpty()) {
      columns.add(ResourcesColumn())
    }
    return columns.sortedBy { it.name }
  }

  private fun filterUnusedWorkAreasFromTasks(
      workAreaIds: MutableSet<WorkAreaIdentifier>,
      tasks: List<TaskDio>
  ) {
    for (task in tasks) {
      if (workAreaIds.isEmpty()) {
        break
      }
      if (task.workAreaId != null) {
        workAreaIds.remove(task.workAreaId)
      }
    }
  }

  private fun filterUnusedWorkAreasFromMilestones(
      workAreaIds: MutableSet<WorkAreaIdentifier>,
      milestones: List<MilestoneDio>
  ) {
    for (milestone in milestones) {
      if (workAreaIds.isEmpty()) {
        break
      }
      if (milestone.workAreaId != null) {
        workAreaIds.remove(milestone.workAreaId)
      }
    }
  }

  private fun filterUnusedWorkAreasFromWorkAreas(
      context: ImportContext,
      workAreaIds: MutableSet<WorkAreaIdentifier>,
      workAreas: List<WorkAreaDio>
  ) {
    for (workArea in workAreas) {
      if (workAreaIds.isEmpty()) {
        break
      }
      if (workArea.parent != null) {

        val usedWorkingAreas =
            workAreaIds.map { Pair(it, context[it]) }.filter { it.second == workArea.parent }
        workAreaIds.removeAll(usedWorkingAreas.map { it.first }.toSet())
      }
    }
  }

  private fun hasWorkOnNonWorkDays(
      taskSchedules: List<TaskScheduleDio>,
      workDays: List<DayOfWeek>
  ): Boolean {
    val nonWorkDays = DayOfWeek.values().toList() - workDays.toSet()
    if (nonWorkDays.isEmpty()) {
      return false
    }

    taskSchedules.forEach { schedule ->
      if (schedule.start?.dayOfWeek?.let { nonWorkDays.contains(it) } == true ||
          schedule.end?.dayOfWeek?.let { nonWorkDays.contains(it) } == true) {
        return true
      }
    }
    return false
  }

  private fun readWorkDaysFromFile(projectFile: ProjectFile) =
      DayOfWeek.values().mapNotNull {
        if (projectFile.defaultCalendar?.isWorkingDay(it) == true &&
            (projectFile.defaultCalendar?.getWork(it, TimeUnit.HOURS)?.duration?.toLong() ?: 0) > 0)
            it
        else null
      }

  private fun readHolidaysFromFile(projectFile: ProjectFile): List<Holiday> {
    fun name(holiday: ProjectCalendarException): String = holiday.name ?: "---"

    return projectFile.defaultCalendar
        ?.calendarExceptions
        // Ignore work day exceptions, as RmS doesn't support them at the moment
        ?.filter { !it.working }
        ?.map { exception ->
          if (exception.recurring == null) {
            val fromDate = exception.fromDate
            val toDate = exception.toDate
            if (fromDate != null && toDate != null && fromDate.isBefore(toDate)) {
              var date = fromDate
              val list = mutableListOf<Holiday>()
              while (date <= toDate) {
                list.add(Holiday(name(exception), date))
                date = date.plusDays(1)
              }
              list
            } else if (fromDate != null) {
              listOf(Holiday(name(exception), fromDate))
            } else if (toDate != null) {
              listOf(Holiday(name(exception), toDate))
            } else {
              emptyList()
            }
          } else {
            exception.recurring.dates.map { Holiday(exception.name, it) }
          }
        }
        ?.flatten()
        ?: emptyList()
  }
}
