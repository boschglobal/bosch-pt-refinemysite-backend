/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary.assembler

import com.bosch.pt.iot.smartsite.project.importer.control.dto.ActivityCodeColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.CustomFieldColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ResourcesColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.TaskFieldColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.UserDefinedFieldColumn
import net.sf.mpxj.FieldType
import net.sf.mpxj.Task
import net.sf.mpxj.TaskField
import net.sf.mpxj.TaskField.DURATION_TEXT
import net.sf.mpxj.TaskField.FINISH_TEXT
import net.sf.mpxj.TaskField.INDICATORS
import net.sf.mpxj.TaskField.NAME
import net.sf.mpxj.TaskField.START_TEXT

object ColumnUtils {

  fun getColumnValue(task: Task, column: ImportColumn): String? =
      when (column) {
        is TaskFieldColumn -> {
          if (column.fieldType == TaskField.RESOURCE_NAMES) {
            task.resourceAssignments.firstOrNull()?.resource?.name
          } else {
            task.getCachedValue(column.fieldType)?.toString()
          }
        }
        is ActivityCodeColumn -> task.activityCodes.firstOrNull { it.type == column.code }?.name
        is CustomFieldColumn -> task.getCachedValue(column.fieldType)?.toString()
        is ResourcesColumn -> task.resourceAssignments.firstOrNull()?.resource?.name
        is UserDefinedFieldColumn -> task.getCachedValue(column.fieldType)?.toString()
        else -> throw IllegalArgumentException("Invalid column type detected")
      }

  fun shouldBeSkipped(fieldType: FieldType): Boolean = columnsToSkip.contains(fieldType)

  private val columnsToSkip = setOf(DURATION_TEXT, FINISH_TEXT, INDICATORS, NAME, START_TEXT)
}
