/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.service

import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.model.RfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.repository.RfvCustomizationRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class RfvCustomizationService(private val rfvCustomizationRepository: RfvCustomizationRepository) {

  @Trace
  fun save(rfvCustomization: RfvCustomization) = rfvCustomizationRepository.save(rfvCustomization)

  @Trace
  fun find(identifier: UUID, version: Long, projectIdentifier: UUID) =
      rfvCustomizationRepository.find(identifier, version, projectIdentifier)!!

  @Trace
  fun findLatest(identifier: UUID, projectIdentifier: UUID) =
      rfvCustomizationRepository.findLatest(identifier, projectIdentifier)!!

  @Trace
  fun delete(identifier: UUID, projectIdentifier: UUID) =
      rfvCustomizationRepository.delete(identifier, projectIdentifier)

  @Trace
  fun deleteByVersion(identifier: UUID, version: Long, projectIdentifier: UUID) =
      rfvCustomizationRepository.deleteByVersion(identifier, version, projectIdentifier)

  @Trace
  fun findLatestCachedByProjectIdentifierAndReason(
      projectIdentifier: UUID,
      reason: DayCardReasonEnum
  ): RfvCustomization? =
      rfvCustomizationRepository.findLatestCachedByProjectIdentifierAndReason(
          projectIdentifier, reason)
}
