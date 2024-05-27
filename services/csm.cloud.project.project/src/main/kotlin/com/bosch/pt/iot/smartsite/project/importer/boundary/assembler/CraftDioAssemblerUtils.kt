/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary.assembler

import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumn
import com.bosch.pt.iot.smartsite.project.importer.model.dio.CraftDio.Companion.PLACEHOLDER_CRAFT_NAME
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.CraftIdentifier
import net.sf.mpxj.Task

object CraftDioAssemblerUtils {

  fun filterCraftColumn(
      columns: List<ImportColumn>,
      craftColumnName: String,
      craftColumnFieldType: String?
  ): ImportColumn? =
      columns.firstOrNull {
        craftColumnName.lowercase() == it.name.trim().lowercase() &&
            craftColumnFieldType?.lowercase() == it.fieldType?.name?.lowercase()
      }
          ?: columns.firstOrNull { craftColumnName.lowercase() == it.name.trim().lowercase() }

  fun getCraft(
      task: Task,
      craftColumn: ImportColumn?,
      craftIdentifiersByName: Map<String, CraftIdentifier>,
  ): CraftIdentifier? {
    var craftId = getCraftIdentifierFromRow(task, craftColumn, craftIdentifiersByName)
    if (craftId == null) {
      craftId = craftIdentifiersByName[PLACEHOLDER_CRAFT_NAME.uppercase()]
    }
    return craftId
  }

  fun getCraftIdentifierFromRow(
      task: Task,
      craftColumn: ImportColumn?,
      craftIdentifiersByName: Map<String, CraftIdentifier>
  ): CraftIdentifier? =
      craftColumn
          ?.let { column ->
            val name = ColumnUtils.getColumnValue(task, column)
            name?.trim()
          }
          ?.let { craftIdentifiersByName[it.uppercase()] }
}
