/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.participant.domain.ParticipantId
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.Participant
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.ParticipantStatusEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import org.springframework.data.mongodb.repository.MongoRepository

interface ParticipantRepository : MongoRepository<Participant, ParticipantId> {

  fun deleteAllByProject(projectId: ProjectId)

  fun findOneByIdentifier(id: ParticipantId): Participant?

  fun findAllByProjectIn(project: List<ProjectId>): List<Participant>

  fun findAllByProjectInAndStatus(
      project: List<ProjectId>,
      status: ParticipantStatusEnum
  ): List<Participant>

  fun findAllByUserAndStatus(user: UserId, status: ParticipantStatusEnum): List<Participant>
}
