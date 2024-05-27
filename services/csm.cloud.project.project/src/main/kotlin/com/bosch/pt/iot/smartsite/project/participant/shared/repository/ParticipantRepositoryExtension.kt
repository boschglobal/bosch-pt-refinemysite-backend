/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.shared.repository

import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import java.util.UUID

interface ParticipantRepositoryExtension {

  fun findOneByUserIdentifierAndProjectIdentifierAndActiveTrue(
      userIdentifier: UUID,
      projectIdentifier: ProjectId
  ): Participant?
}
