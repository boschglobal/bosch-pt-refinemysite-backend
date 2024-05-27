/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary.dto

import com.bosch.pt.iot.smartsite.project.importer.control.dto.ImportColumn
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResult
import kotlin.properties.Delegates.notNull

class AnalysisResult(
    val validationResults: List<ValidationResult>,
    val statistics: AnalysisStatistics,
    val craftColumn: ImportColumn?,
    val workAreaColumn: ImportColumn?
) {
  var version by notNull<Long>()
}

class AnalysisStatistics(
    val workAreas: Int,
    val crafts: Int,
    val tasks: Int,
    val milestones: Int,
    val relations: Int
)
