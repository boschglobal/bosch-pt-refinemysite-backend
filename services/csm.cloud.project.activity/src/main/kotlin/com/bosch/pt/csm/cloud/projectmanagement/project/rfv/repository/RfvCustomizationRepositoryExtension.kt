/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.model.RfvCustomization
import java.util.UUID

interface RfvCustomizationRepositoryExtension {

  fun findLatestCachedByProjectIdentifierAndReason(
      projectIdentifier: UUID,
      reason: DayCardReasonEnum
  ): RfvCustomization?
}
