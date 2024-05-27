/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.autorization

import com.bosch.pt.csm.cloud.projectmanagement.application.security.SecurityContextHelper
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.Participant
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.ParticipantStatusEnum.ACTIVE
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.repository.ParticipantRepository
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.asUserId
import org.springframework.stereotype.Component

@Component
class ParticipantAuthorizationComponent(private val participantRepository: ParticipantRepository) {

  fun hasReadPermissionOnProjects(participants: List<Participant>): Boolean {
    if (participants.isEmpty()) return false

    val currentUserDetails = SecurityContextHelper.getCurrentUserDetails()
    val currentUsersProjects =
        participantRepository
            .findAllByUserAndStatus(currentUserDetails.userIdentifier().asUserId(), ACTIVE)
            .map { it.project }
    return currentUsersProjects.containsAll(participants.map { it.project })
  }
}
