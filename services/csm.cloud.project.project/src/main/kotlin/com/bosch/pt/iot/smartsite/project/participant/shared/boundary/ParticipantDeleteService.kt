/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.participant.shared.boundary

import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import datadog.trace.api.Trace
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional

@Service
open class ParticipantDeleteService(private val participantRepository: ParticipantRepository) {

  @Trace
  @NoPreAuthorize
  @Transactional(propagation = MANDATORY)
  open fun deleteByProjectId(projectId: Long) {
    val participants = participantRepository.findAllByProjectId(projectId)
    if (participants.isNotEmpty()) {
      participantRepository.deleteAllInBatch(participants)
    }
  }
}
