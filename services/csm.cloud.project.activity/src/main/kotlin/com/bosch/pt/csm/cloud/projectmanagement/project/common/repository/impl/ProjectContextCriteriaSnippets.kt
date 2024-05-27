/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.common.repository.impl

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.AttributeNames.Project.PROJECT_IDENTIFIER
import java.util.UUID
import org.springframework.data.mongodb.core.query.Criteria

object ProjectContextCriteriaSnippets {
  fun belongsToProject(identifier: UUID): Criteria =
      Criteria.where(PROJECT_IDENTIFIER).`is`(identifier)
}
