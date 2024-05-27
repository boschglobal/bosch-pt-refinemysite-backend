/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.quickfilter.authorization

import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantAuthorizationRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.quickfilter.domain.QuickFilterId
import com.bosch.pt.iot.smartsite.project.quickfilter.repository.QuickFilterRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
open class QuickFilterAuthorizationComponent(
    private val quickFilterRepository: QuickFilterRepository,
    private val participantAuthorizationRepository: ParticipantAuthorizationRepository
) {
  open fun hasUpdateAndDeletePermissionOnQuickFilter(
      identifier: QuickFilterId,
      projectRef: ProjectId
  ): Boolean {
    val participant =
        participantAuthorizationRepository.getParticipantOfCurrentUser(projectRef) ?: return false

    return quickFilterRepository.existsOneByIdentifierAndParticipantIdentifierAndProjectIdentifier(
        identifier, participant.identifier, projectRef)
  }
}
