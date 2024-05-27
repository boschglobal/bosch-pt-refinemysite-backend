/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.boundary

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.CRITICAL
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.UNCRITICAL
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.relation.boundary.control.DateLookupComponent
import com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto.RelationElementDto
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElement
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.MILESTONE
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum.FINISH_TO_START
import com.bosch.pt.iot.smartsite.project.relation.repository.RelationRepository
import com.bosch.pt.iot.smartsite.project.relation.repository.dto.RelationFilterDto
import datadog.trace.api.Trace
import java.time.LocalDate
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable.unpaged
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert

@Service
open class RelationCriticalityService(
    private val dateLookupComponent: DateLookupComponent,
    private val relationRepository: RelationRepository
) {

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional
  open fun calculateCriticalityByRelationElements(
      changedElements: Set<RelationElementDto>,
      projectIdentifier: ProjectId
  ) {
    val sourceAndTargetElements = changedElements.map { it.toRelationElement() }.toSet()
    val affectedRelations = findRelations(sourceAndTargetElements, projectIdentifier)
    calculateFinishToStartCriticality(affectedRelations)
  }

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional
  open fun calculateCriticalityByRelations(relationIdentifiers: Collection<UUID>) =
      relationRepository.findAllByIdentifierIn(relationIdentifiers.distinct()).apply {
        calculateFinishToStartCriticality(this)
      }

  private fun calculateFinishToStartCriticality(relations: Collection<Relation>) {
    val finishToStartRelations = relations.filter { it.type == FINISH_TO_START }

    val relationElementDates =
        dateLookupComponent.findDates(finishToStartRelations).associateBy { it.identifier }

    for (relation in finishToStartRelations) {
      val predecessorDates = relationElementDates[relation.source.identifier]
      val successorDates = relationElementDates[relation.target.identifier]
      if (predecessorDates == null) {
        LOGGER.warn(
            "Cannot update criticality of $relation. No date found for relation source ${relation.source}.")
      }
      if (successorDates == null) {
        LOGGER.warn(
            "Cannot update criticality of $relation. No date found for relation target ${relation.target}.")
      }
      if (predecessorDates != null && successorDates != null) {
        if (predecessorDates.end != null && successorDates.start != null) {
          calculateFinishToStartCriticality(relation, predecessorDates.end, successorDates.start)
        } else {
          LOGGER.warn(
              "Either the predecessor end date or the successor start date is undefined for finish-to-start " +
                  "relation $relation. The relation is invalid. Skipping criticality calculation.")
        }
      } else {
        LOGGER.warn(
            "Could not find schedule of predecessor or successor in finish-to-start relation $relation")
      }
    }
  }

  private fun calculateFinishToStartCriticality(
      relation: Relation,
      predecessorEnd: LocalDate,
      successorStart: LocalDate
  ) {
    assertIsFinishToStartRelation(relation)

    val critical =
        if (relation.source.type == MILESTONE || relation.target.type == MILESTONE) {
          // a successor milestone placed at the end date of its predecessor task must not be
          // critical; vice versa, a predecessor milestone placed at the start date of its successor
          // task must not be critical. Also, we decided to apply the same logic if both ends of the
          // relation are milestones (but there is no strong requirement to do so).
          predecessorEnd.isAfter(successorStart)
        } else {
          // task-task relations must be critical if their schedules overlap
          predecessorEnd.isAfter(successorStart) || predecessorEnd.isEqual(successorStart)
        }
    if (relation.critical != critical) {
      relation.critical = critical
      relationRepository.save(relation, if (critical) CRITICAL else UNCRITICAL)

      val criticality = if (critical) "critical" else "uncritical"
      LOGGER.debug("Relation became $criticality: $relation")
    }
  }

  private fun findRelations(
      relationElements: Set<RelationElement>,
      projectIdentifier: ProjectId
  ): List<Relation> {
    val filters =
        RelationFilterDto(
            types = setOf(FINISH_TO_START),
            sources = relationElements,
            targets = relationElements,
            projectIdentifier = projectIdentifier)

    val relationIdentifiers =
        relationRepository.findForFilters(filters, unpaged()).also {
          // double-check that we did not miss any relation matching the filters
          assertIdentifiersCountIsExpected(it, filters)
        }

    return relationRepository.findAllByIdentifierIn(relationIdentifiers)
  }

  private fun assertIdentifiersCountIsExpected(
      relationIdentifiers: List<UUID>,
      filters: RelationFilterDto
  ) {
    val actualCount = relationIdentifiers.size.toLong()
    val expectedCount = relationRepository.countForFilters(filters)
    Assert.isTrue(
        expectedCount == actualCount,
        "Found $actualCount relations but expected $expectedCount relations to be found. This is probably a bug.")
  }

  private fun assertIsFinishToStartRelation(relation: Relation) =
      Assert.isTrue(relation.type == FINISH_TO_START, "Relation type must be $FINISH_TO_START")

  companion object {
    private val LOGGER = LoggerFactory.getLogger(RelationCriticalityService::class.java)
  }
}
