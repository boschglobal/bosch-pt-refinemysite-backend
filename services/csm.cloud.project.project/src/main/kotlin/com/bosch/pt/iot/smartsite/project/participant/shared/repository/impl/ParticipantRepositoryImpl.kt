/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.participant.shared.repository.impl

import com.bosch.pt.iot.smartsite.company.model.Company
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant_
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepositoryExtension
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.ProjectId_
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project_
import com.bosch.pt.iot.smartsite.user.model.PhoneNumber
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.User_
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import java.util.UUID

class ParticipantRepositoryImpl(@PersistenceContext val entityManager: EntityManager) :
    ParticipantRepositoryExtension {

  @Suppress("UNCHECKED_CAST")
  override fun findOneByUserIdentifierAndProjectIdentifierAndActiveTrue(
      userIdentifier: UUID,
      projectIdentifier: ProjectId
  ): Participant? {
    val cb = entityManager.criteriaBuilder
    val cq = cb.createQuery(Participant::class.java)

    val root = cq.from(Participant::class.java)
    val project =
        root.fetch<Participant, Project>(Participant_.PROJECT, JoinType.LEFT)
            as Join<Participant, Project>
    val user =
        root.fetch<Participant, User>(Participant_.USER, JoinType.LEFT) as Join<Participant, User>
    user.fetch<User, PhoneNumber>(User_.PHONENUMBERS, JoinType.LEFT)

    root.fetch<Participant, Company>(Participant_.COMPANY)

    cq.where(
        cb.and(
            cb.equal(user.get(User_.identifier), userIdentifier),
            cb.equal(
                project.join(Project_.identifier).get<UUID>(ProjectId_.identifier.name),
                projectIdentifier.identifier),
            cb.equal(root.get(Participant_.status), ParticipantStatusEnum.ACTIVE)))

    return entityManager.createQuery(cq).resultList.firstOrNull()
  }
}
