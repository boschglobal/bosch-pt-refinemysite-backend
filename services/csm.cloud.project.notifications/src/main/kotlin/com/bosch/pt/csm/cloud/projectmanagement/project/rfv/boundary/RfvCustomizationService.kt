/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.boundary

import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.model.RfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.repository.RfvCustomizationRepository
import datadog.trace.api.Trace
import java.util.UUID
import jakarta.validation.Valid
import org.springframework.stereotype.Service

@Service
class RfvCustomizationService(private val rfvCustomizationRepository: RfvCustomizationRepository) {

  @Trace
  fun save(@Valid rfvCustomization: RfvCustomization) =
      rfvCustomizationRepository.save(rfvCustomization)

  @Trace
  fun delete(identifier: UUID, projectIdentifier: UUID) =
      rfvCustomizationRepository.delete(identifier, projectIdentifier)

  @Trace
  fun findLatestCachedByProjectIdentifierAndReason(
      projectIdentifier: UUID,
      reason: DayCardReasonEnum
  ): RfvCustomization? =
      rfvCustomizationRepository.findLatestCachedByProjectIdentifierAndReason(
          projectIdentifier, reason)
}
