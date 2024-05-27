/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.project.shared.repository

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.dto.DateByProjectIdentifierDto
import com.bosch.pt.iot.smartsite.project.project.shared.model.dto.NameByIdentifierDto

interface ProjectRepositoryExtension {

  fun markAsDeleted(projectId: Long)

  fun findCompanyNamesByProjectIdentifiers(
      datesByProjectIdentifiers: Set<DateByProjectIdentifierDto>
  ): Map<ProjectId, NameByIdentifierDto>
}
