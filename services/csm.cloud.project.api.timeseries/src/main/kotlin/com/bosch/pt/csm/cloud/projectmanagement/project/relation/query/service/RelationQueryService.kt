/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.service

import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.MILESTONE
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TASK
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.domain.MilestoneId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.model.Relation
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.model.RelationTypeEnum.PART_OF
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.repository.RelationRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service

@Service
class RelationQueryService(private val relationRepository: RelationRepository) {

  @Cacheable(cacheNames = ["relations-predecessors-by-milestones-and-reference-type"])
  @PreAuthorize("isAuthenticated()")
  fun findAllPredecessorsOfMilestonesOfTypeAndDeletedFalse(
      milestoneIds: List<MilestoneId>,
      referencedType: String
  ): List<Relation> =
      relationRepository
          .findAllByTargetIdentifierInAndTargetTypeAndSourceTypeAndTypeNotAndDeletedFalse(
              milestoneIds.map { it.value }, MILESTONE.name, referencedType, PART_OF)

  @Cacheable(cacheNames = ["relations-successors-by-milestones-and-reference-type"])
  @PreAuthorize("isAuthenticated()")
  fun findAllSuccessorsOfMilestoneOfTypeAndDeletedFalse(
      milestoneIds: List<MilestoneId>,
      referencedType: String
  ): List<Relation> =
      relationRepository
          .findAllBySourceIdentifierInAndSourceTypeAndTargetTypeAndTypeNotAndDeletedFalse(
              milestoneIds.map { it.value }, MILESTONE.name, referencedType, PART_OF)

  @Cacheable(cacheNames = ["relations-predecessors-by-tasks-and-reference-type"])
  @PreAuthorize("isAuthenticated()")
  fun findAllPredecessorsOfTasksOfTypeAndDeletedFalse(
      taskIds: List<TaskId>,
      referencedType: String
  ): List<Relation> =
      relationRepository
          .findAllByTargetIdentifierInAndTargetTypeAndSourceTypeAndTypeNotAndDeletedFalse(
              taskIds.map { it.value }, TASK.name, referencedType, PART_OF)

  @Cacheable(cacheNames = ["relations-successors-by-task-and-reference-type"])
  @PreAuthorize("isAuthenticated()")
  fun findAllSuccessorsOfTasksOfTypeAndDeletedFalse(
      taskIds: List<TaskId>,
      referencedType: String
  ): List<Relation> =
      relationRepository
          .findAllBySourceIdentifierInAndSourceTypeAndTargetTypeAndTypeNotAndDeletedFalse(
              taskIds.map { it.value }, TASK.name, referencedType, PART_OF)

  @Cacheable(cacheNames = ["relations-nested-tasks-of-milestones"])
  @PreAuthorize("isAuthenticated()")
  fun findAllNestedTasksAndDeletedFalse(milestoneIds: List<MilestoneId>): List<Relation> =
      relationRepository.findAllByTargetIdentifierInAndTypeAndDeletedFalse(
          milestoneIds.map { it.value }, PART_OF)

  @Cacheable(cacheNames = ["relations-parent-milestones-of-task"])
  @PreAuthorize("isAuthenticated()")
  fun findAllParentMilestonesAndDeletedFalse(taskIds: List<TaskId>): List<Relation> =
      relationRepository
          .findAllBySourceIdentifierInAndSourceTypeAndTargetTypeAndTypeAndDeletedFalse(
              taskIds.map { it.value }, TASK.name, MILESTONE.name, PART_OF)

  @Cacheable(cacheNames = ["relations-by-project"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByProjects(projectIds: List<ProjectId>): List<Relation> =
      relationRepository.findAllByProjectIn(projectIds)

  @Cacheable(cacheNames = ["relations-by-project-deleted-false"])
  @PreAuthorize("isAuthenticated()")
  fun findAllByProjectsAndDeletedFalse(projectIds: List<ProjectId>): List<Relation> =
    relationRepository.findAllByProjectInAndDeletedFalse(projectIds)
}
