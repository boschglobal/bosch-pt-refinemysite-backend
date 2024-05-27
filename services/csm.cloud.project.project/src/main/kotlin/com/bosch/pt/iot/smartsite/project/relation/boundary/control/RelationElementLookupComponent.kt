/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.boundary.control

import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneIdDto
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.MILESTONE
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.TASK
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskIds
import com.bosch.pt.iot.smartsite.project.task.shared.dto.TaskIdDto
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import java.util.UUID
import org.springframework.stereotype.Component

@Component
open class RelationElementLookupComponent(
    private val taskRepository: TaskRepository,
    private val milestoneRepository: MilestoneRepository
) {

  open fun exists(
      identifier: UUID,
      type: RelationElementTypeEnum,
      projectIdentifier: ProjectId
  ): Boolean =
      when (type) {
        MILESTONE ->
            milestoneRepository.existsByIdentifierAndProjectIdentifier(
                MilestoneId(identifier), projectIdentifier)
        TASK ->
            taskRepository.existsByIdentifierAndProjectIdentifier(
                identifier.asTaskId(), projectIdentifier)
      }

  open fun find(relationElementFilter: RelationElementFilter): List<UUID> =
      when (relationElementFilter) {
        is MilestoneElementFilter ->
            milestoneRepository
                .findAllByIdentifierInAndProjectIdentifier(
                    relationElementFilter.identifiers.map { MilestoneId(it) }.toSet(),
                    relationElementFilter.projectIdentifier,
                    MilestoneIdDto::class.java)
                .map { it.identifier.toUuid() }
        is TaskElementFilter -> {
          taskRepository
              .findAllByIdentifierInAndProjectIdentifier(
                  relationElementFilter.identifiers.asTaskIds(),
                  relationElementFilter.projectIdentifier,
                  TaskIdDto::class.java)
              .map { it.identifier.toUuid() }
        }
      }
}

sealed class RelationElementFilter(val identifiers: Set<UUID>, val projectIdentifier: ProjectId)

class TaskElementFilter(identifiers: Set<UUID>, projectIdentifier: ProjectId) :
    RelationElementFilter(identifiers, projectIdentifier)

class MilestoneElementFilter(identifiers: Set<UUID>, projectIdentifier: ProjectId) :
    RelationElementFilter(identifiers, projectIdentifier)
