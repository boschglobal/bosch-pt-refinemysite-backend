/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.participant.model.Participant
import java.util.UUID

interface ParticipantRepositoryExtension {
    fun findAllByProjectIdentifier(projectIdentifier: UUID): List<Participant>
}
