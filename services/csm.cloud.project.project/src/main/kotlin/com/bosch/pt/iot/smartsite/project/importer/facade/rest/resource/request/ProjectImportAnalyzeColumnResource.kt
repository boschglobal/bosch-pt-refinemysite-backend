/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.request

import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_CRAFT_COLUMN_NAME_UNKNOWN
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_WORK_AREA_COLUMN_NAME_UNKNOWN
import com.bosch.pt.iot.smartsite.project.importer.boundary.dto.AnalysisColumn
import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumnType

abstract class ProjectImportAnalyzeColumnResource(
    val columnType: ImportColumnType,
    val fieldType: String?
) {

  abstract fun errorMessageKey(): String
}

class ProjectImportAnalyzeCraftColumnResource(columnType: ImportColumnType, fieldType: String?) :
    ProjectImportAnalyzeColumnResource(columnType, fieldType) {

  override fun errorMessageKey(): String = IMPORT_VALIDATION_CRAFT_COLUMN_NAME_UNKNOWN
}

class ProjectImportAnalyzeWorkAreaColumnResource(columnType: ImportColumnType, fieldType: String?) :
    ProjectImportAnalyzeColumnResource(columnType, fieldType) {

  override fun errorMessageKey(): String = IMPORT_VALIDATION_WORK_AREA_COLUMN_NAME_UNKNOWN
}

fun ProjectImportAnalyzeColumnResource.toAnalysisColumn() =
    AnalysisColumn(this.fieldType, this.columnType, this.errorMessageKey())
