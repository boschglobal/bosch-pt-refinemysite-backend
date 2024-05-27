/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.api

import com.bosch.pt.iot.smartsite.project.exporter.api.MilestoneExportSchedulingType.MANUALLY_SCHEDULED
import com.bosch.pt.iot.smartsite.project.exporter.api.TaskExportSchedulingType.AUTO_SCHEDULED
import java.util.Locale
import java.util.UUID

data class ProjectExportCommand(
    val locale: Locale,
    val projectIdentifier: UUID,
    val exportParameters: ProjectExportParameters
)

data class ProjectExportParameters(
    val format: ProjectExportFormatEnum,
    val includeMilestones: Boolean = true,
    val includeComments: Boolean,
    val taskExportSchedulingType: TaskExportSchedulingType = AUTO_SCHEDULED,
    val milestoneExportSchedulingType: MilestoneExportSchedulingType = MANUALLY_SCHEDULED,
)

data class ProjectExportZipCommand(val locale: Locale, val projectIdentifier: UUID)

enum class ProjectExportFormatEnum {
  MS_PROJECT_XML,
  PRIMAVERA_P6_XML
}

enum class TaskExportSchedulingType {
  MANUALLY_SCHEDULED,
  AUTO_SCHEDULED
}

enum class MilestoneExportSchedulingType {
  MANUALLY_SCHEDULED,
  AUTO_SCHEDULED
}
