/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.shared.repository.impl

import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneList
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneList_
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneListRepositoryExtension
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.ProjectId_
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project_
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId_
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea_
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import java.time.LocalDate
import java.util.UUID

class MilestoneListRepositoryImpl(@PersistenceContext val entityManager: EntityManager) :
    MilestoneListRepositoryExtension {

  override fun findOneByKey(
      projectIdentifier: ProjectId,
      date: LocalDate,
      header: Boolean,
      workAreaIdentifier: WorkAreaId?
  ): MilestoneList? {
    val cb = entityManager.criteriaBuilder
    val cq = cb.createQuery(MilestoneList::class.java)
    val root = cq.from(MilestoneList::class.java)
    root.fetch<MilestoneList, Milestone>(MilestoneList_.MILESTONES)

    val project =
        root.fetch<MilestoneList, Project>(MilestoneList_.PROJECT) as Join<MilestoneList, Project>
    val workArea = root.join(MilestoneList_.workArea, JoinType.LEFT)
    val predicates =
        mutableListOf<Predicate>().apply {
          add(
              cb.equal(
                  project.join(Project_.identifier).get<UUID>(ProjectId_.identifier.name),
                  projectIdentifier.identifier))
          add(cb.equal(root.get(MilestoneList_.date), date))
          add(cb.equal(root.get(MilestoneList_.header), header))
          add(
              if (workAreaIdentifier == null) {
                cb.isNull(workArea)
              } else {
                cb.equal(
                    workArea.join(WorkArea_.identifier).get<UUID>(WorkAreaId_.identifier.name),
                    workAreaIdentifier.identifier)
              })
        }

    cq.where(*predicates.toTypedArray())

    return entityManager.createQuery(cq).resultList.firstOrNull()
  }
}
