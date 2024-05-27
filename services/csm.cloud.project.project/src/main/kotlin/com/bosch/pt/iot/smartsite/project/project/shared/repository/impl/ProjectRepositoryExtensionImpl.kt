/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.project.shared.repository.impl

import com.bosch.pt.iot.smartsite.company.model.Company_
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant_
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.ProjectId_
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project_
import com.bosch.pt.iot.smartsite.project.project.shared.model.dto.DateByProjectIdentifierDto
import com.bosch.pt.iot.smartsite.project.project.shared.model.dto.IdentifierAndNameByProjectIdentifierDto
import com.bosch.pt.iot.smartsite.project.project.shared.model.dto.NameByIdentifierDto
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepositoryExtension
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import java.util.UUID

class ProjectRepositoryExtensionImpl : ProjectRepositoryExtension {

  @PersistenceContext private lateinit var entityManager: EntityManager

  override fun markAsDeleted(projectId: Long) =
      entityManager
          .createNativeQuery("update project set deleted = true where id = :id")
          .apply { setParameter("id", projectId) }
          .executeUpdate()
          .let {}

  override fun findCompanyNamesByProjectIdentifiers(
      datesByProjectIdentifiers: Set<DateByProjectIdentifierDto>
  ): Map<ProjectId, NameByIdentifierDto> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(IdentifierAndNameByProjectIdentifierDto::class.java)

    val root = query.from(Participant::class.java)
    val projectJoin = root.join(Participant_.project)
    val companyJoin = root.join(Participant_.company)

    val constraint =
        datesByProjectIdentifiers
            .map {
              cb.and(
                  cb.equal(
                      projectJoin.join(Project_.identifier).get<UUID>(ProjectId_.identifier.name),
                      it.projectIdentifier.identifier),
                  cb.equal(root.get<Any>(Participant_.CREATED_DATE), it.date))
            }
            .reduce { lastPredicate, currentPredicate ->
              if (lastPredicate == null) {
                currentPredicate
              } else {
                cb.or(lastPredicate, currentPredicate)
              }
            }

    query.where(constraint)
    query.multiselect(
        projectJoin.get<Any>(Project_.IDENTIFIER),
        companyJoin.get<Any>(Company_.IDENTIFIER),
        companyJoin.get<Any>(Company_.NAME))

    return entityManager
        .createQuery(query)
        .resultList
        .associate { it.projectIdentifier to NameByIdentifierDto.of(it.identifier, it.name) }
        .toList()
        .associate { it.first to it.second }
  }
}
