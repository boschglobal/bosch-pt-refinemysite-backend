/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.repository

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.mongodb.client.result.UpdateResult
import java.util.UUID

interface MilestoneRepositoryExtension {

  fun updatePosition(
      projectIdentifier: UUID,
      aggregateIdentifier: AggregateIdentifier,
      position: Int
  ): UpdateResult
}
