/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.boundary.control

import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneRepository
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElement
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.MILESTONE
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.TASK
import com.bosch.pt.iot.smartsite.project.relation.model.dto.MilestoneDateDto
import com.bosch.pt.iot.smartsite.project.relation.model.dto.TaskScheduleDto
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository.TaskScheduleRepository
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Component

@Component
open class DateLookupComponent(
    private val milestoneRepository: MilestoneRepository,
    private val taskScheduleRepository: TaskScheduleRepository
) {

  open fun findDates(relation: Relation) = findDates(listOf(relation))

  open fun findDates(relations: Collection<Relation>) =
      findDatesOfRelationElements(relations.map { setOf(it.source, it.target) }.flatten())

  private fun findDatesOfRelationElements(relationElements: Collection<RelationElement>) =
      findDatesOfTasks(relationElements) + findDatesOfMilestones(relationElements)

  private fun findDatesOfTasks(relationElements: Collection<RelationElement>) =
      taskScheduleRepository
          .findAllByTaskIdentifierIn(
              relationElements.taskIdentifiers(), TaskScheduleDto::class.java)
          .map { RelationElementDates(it.taskIdentifier.toUuid(), TASK, it.start, it.end) }
          .distinct()

  private fun findDatesOfMilestones(relationElements: Collection<RelationElement>) =
      milestoneRepository
          .findAllByIdentifierIn(
              relationElements.milestoneIdentifiers(), MilestoneDateDto::class.java)
          .map { RelationElementDates(it.identifier.toUuid(), MILESTONE, it.date, it.date) }
          .distinct()

  private fun Collection<RelationElement>.milestoneIdentifiers() =
      filter { it.type == MILESTONE }.map { it.identifier.asMilestoneId() }.distinct()

  private fun Collection<RelationElement>.taskIdentifiers() =
      filter { it.type == TASK }.map { it.identifier.asTaskId() }.distinct()
}

data class RelationElementDates(
    val identifier: UUID,
    val type: RelationElementTypeEnum,
    val start: LocalDate?,
    val end: LocalDate?
)
