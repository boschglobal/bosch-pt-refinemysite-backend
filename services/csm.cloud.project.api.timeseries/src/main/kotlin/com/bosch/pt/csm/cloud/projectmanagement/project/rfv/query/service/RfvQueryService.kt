/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.service

import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.model.ProjectRfvs
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.model.Rfv
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.repository.RfvRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class RfvQueryService(private val rfvRepository: RfvRepository) {

  @PreAuthorize("isAuthenticated()")
  fun findAllByProjectsAndDeletedFalse(
      projectIds: List<ProjectId>
  ): Map<ProjectId, Map<DayCardReasonEnum, Rfv>> =
      rfvRepository
          .findAllByProjectInAndDeletedFalse(projectIds)
          .groupingBy { it.project }
          .fold(
              { _: ProjectId, _: Rfv -> mutableMapOf() },
              { _, accumulator, element -> accumulator.also { it[element.reason] = element } })

  @PreAuthorize("isAuthenticated()")
  fun findAllByProjectsWithMissing(projectIds: List<ProjectId>): List<ProjectRfvs> =
      rfvRepository
          .findAllByProjectIn(projectIds)
          .groupBy { it.project }
          .map { ProjectRfvs(it.key, it.value) }
          .let {
            val missingProjects = projectIds - it.map { it.projectId }.toSet()
            it + missingProjects.map { ProjectRfvs(it, emptyList()) }
          }

  @PreAuthorize("isAuthenticated()")
  fun findAllByProjectsAndDeletedFalseWithMissing(projectIds: List<ProjectId>): List<ProjectRfvs> =
      rfvRepository
          .findAllByProjectInAndDeletedFalse(projectIds)
          .groupBy { it.project }
          .map { ProjectRfvs(it.key, it.value) }
          .let {
            val missingProjects = projectIds - it.map { it.projectId }.toSet()
            it + missingProjects.map { ProjectRfvs(it, emptyList()) }
          }
}
