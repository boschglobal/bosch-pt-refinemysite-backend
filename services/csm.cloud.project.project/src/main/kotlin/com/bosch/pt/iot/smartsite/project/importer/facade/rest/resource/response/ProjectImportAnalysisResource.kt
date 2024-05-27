/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import com.bosch.pt.iot.smartsite.project.importer.boundary.dto.AnalysisStatistics
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.fasterxml.jackson.annotation.JsonProperty

class ProjectImportAnalysisResource(
    @JsonProperty("id") val identifier: ProjectId,
    val version: Long,
    val validationResults: List<ProjectImportValidationResult>,
    val statistics: AnalysisStatistics
) : AbstractResource() {

  companion object {
    const val LINK_IMPORT = "import"
  }
}
