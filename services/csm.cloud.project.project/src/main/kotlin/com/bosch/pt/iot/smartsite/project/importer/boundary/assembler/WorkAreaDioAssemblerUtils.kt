/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary.assembler

import com.bosch.pt.iot.smartsite.project.importer.boundary.ImportContext
import com.bosch.pt.iot.smartsite.project.importer.boundary.assembler.WorkAreaDioAssembler.Companion.workAreaIdByName
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumn
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.WorkAreaIdentifier
import net.sf.mpxj.Task

object WorkAreaDioAssemblerUtils {

  fun filterWorkAreaColumn(
      columns: List<ImportColumn>,
      workAreaColumnName: String,
      workAreaColumnFieldType: String?
  ): ImportColumn? =
      columns.firstOrNull {
        workAreaColumnName.lowercase() == it.name.trim().lowercase() &&
            workAreaColumnFieldType?.lowercase() == it.fieldType?.name?.lowercase()
      }
          ?: columns.firstOrNull { workAreaColumnName.lowercase() == it.name.trim().lowercase() }

  fun getWorkArea(
      task: Task,
      workAreaColumn: ImportColumn?,
      context: ImportContext,
      readWorkAreasRecursive: Boolean
  ): WorkAreaIdentifier? {
    var workAreaId = getWorkAreaIdentifierFromRow(context, task, workAreaColumn)
    if (workAreaId == null && readWorkAreasRecursive && task.parentTask != null) {
      workAreaId = getWorkAreaFromHierarchy(task, context)
    }
    return workAreaId
  }

  private fun getWorkAreaIdentifierFromRow(
      context: ImportContext,
      task: Task,
      workAreaColumn: ImportColumn?,
  ): WorkAreaIdentifier? =
      workAreaColumn
          ?.let { column -> ColumnUtils.getColumnValue(task, column)?.trim() }
          ?.let { workAreaIdByName(context, it) }

  private fun getWorkAreaFromHierarchy(task: Task, context: ImportContext): WorkAreaIdentifier? =
      if (context[WorkAreaIdentifier(task.parentTask.uniqueID)] != null) {
        WorkAreaIdentifier(task.parentTask.uniqueID)
      } else null
}
