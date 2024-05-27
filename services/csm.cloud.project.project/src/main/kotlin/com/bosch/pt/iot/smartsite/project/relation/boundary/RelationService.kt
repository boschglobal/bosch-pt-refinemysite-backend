/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.boundary

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventEnumAvro.DELETED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.exceptions.DuplicateEntityException
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.verify
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_ENTITY_DUPLICATED
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_PROJECT_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.RELATION_VALIDATION_ERROR_ELEMENT_NOT_FOUND
import com.bosch.pt.iot.smartsite.common.i18n.Key.RELATION_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.project.businesstransaction.boundary.ProjectProducerBusinessTransactionManager
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.relation.boundary.control.DateLookupComponent
import com.bosch.pt.iot.smartsite.project.relation.boundary.control.MilestoneElementFilter
import com.bosch.pt.iot.smartsite.project.relation.boundary.control.RelationElementFilter
import com.bosch.pt.iot.smartsite.project.relation.boundary.control.RelationElementLookupComponent
import com.bosch.pt.iot.smartsite.project.relation.boundary.control.TaskElementFilter
import com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto
import com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto.RelationElementDto
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.MILESTONE
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.TASK
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum.FINISH_TO_START
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum.PART_OF
import com.bosch.pt.iot.smartsite.project.relation.repository.RelationRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert
import org.springframework.util.IdGenerator

@Service
open class RelationService(
    private val idGenerator: IdGenerator,
    private val projectRepository: ProjectRepository,
    private val dateLookupComponent: DateLookupComponent,
    private val relationElementLookupComponent: RelationElementLookupComponent,
    private val relationRepository: RelationRepository,
    private val businessTransactionManager: ProjectProducerBusinessTransactionManager
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@relationAuthorizationComponent.hasCreateRelationPermissionOnProject(#projectIdentifier)")
  open fun create(relationDto: RelationDto, projectIdentifier: ProjectId): UUID {
    val project = findProjectOrFail(projectIdentifier)

    existsOrFail(relationDto.source, projectIdentifier)
    existsOrFail(relationDto.target, projectIdentifier)

    assertSourceAndTargetDifferent(relationDto)
    if (relationDto.type == PART_OF) {
      assertValidPartOfRelation(relationDto)
    }

    val relation = relationDto.toRelation(project).apply { identifier = idGenerator.generateId() }
    if (relationDto.type == FINISH_TO_START) {
      assertDatesExistForSourceAndTarget(relation)
    }
    assertDoesNotExist(relation)

    return relationRepository.save(relation, CREATED).identifier!!
  }

  @Trace
  @Transactional
  @PreAuthorize(
      "@relationAuthorizationComponent.hasCreateRelationPermissionOnProject(#projectIdentifier)")
  open fun createBatch(
      relations: Collection<RelationDto>,
      projectIdentifier: ProjectId
  ): Set<UUID> =
      businessTransactionManager.doBatchInBusinessTransaction(projectIdentifier) {
        val project = findProjectOrFail(projectIdentifier)

        // Collect identifiers of sources and targets of tasks and milestones
        val sourceTaskIds = relations.filter { it.source.type == TASK }.map { it.source.id }.toSet()
        val targetTaskIds = relations.filter { it.target.type == TASK }.map { it.target.id }.toSet()

        val sourceMilestoneIds =
            relations.filter { it.source.type == MILESTONE }.map { it.source.id }.toSet()
        val targetMilestoneIds =
            relations.filter { it.target.type == MILESTONE }.map { it.target.id }.toSet()

        // Ensure tasks and milestones exist
        allExistOrFail(TaskElementFilter(sourceTaskIds + targetTaskIds, projectIdentifier))
        allExistOrFail(
            MilestoneElementFilter(sourceMilestoneIds + targetMilestoneIds, projectIdentifier))

        // Map DTOs to Entities
        val relationEntities =
            relations.map { relationDto ->
              assertSourceAndTargetDifferent(relationDto)
              if (relationDto.type == PART_OF) {
                assertValidPartOfRelation(relationDto)
              }

              relationDto.toRelation(project).apply { identifier = idGenerator.generateId() }
            }

        relationEntities
            .filter { it.type == FINISH_TO_START }
            .also { assertDatesExistForSourceAndTarget(it) }

        // Ideally we would check here for duplicates. There is though no ideal solution to query
        // a list of relations for a list of triple-identifiers (type, identifier, version).
        // A "faster" solution could be to load all existing relations of a project and check for
        // duplicates.

        return@doBatchInBusinessTransaction relationEntities
            .map { relationRepository.save(it, CREATED).identifier!! }
            .toSet()
      }

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize("@relationAuthorizationComponent.hasViewPermissionOnProject(#projectIdentifier)")
  open fun find(identifier: UUID, projectIdentifier: ProjectId) =
      findRelationWithDetailsOrFail(identifier, projectIdentifier)

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize("@relationAuthorizationComponent.hasViewPermissionOnProject(#projectIdentifier)")
  open fun findBatch(identifiers: Collection<UUID>, projectIdentifier: ProjectId) =
      findAllRelationsWithDetails(identifiers, projectIdentifier)

  @Trace
  @Transactional
  @PreAuthorize(
      "@relationAuthorizationComponent.hasDeletePermissionOnRelationOfProject(#identifier, #projectIdentifier)")
  open fun delete(identifier: UUID, projectIdentifier: ProjectId, eTag: ETag? = null) {
    val relation = findRelationOrFail(identifier, projectIdentifier)
    eTag?.verify(relation)
    relationRepository.delete(relation, DELETED)
  }

  @Trace
  @NoPreAuthorize
  @Transactional(propagation = Propagation.MANDATORY)
  open fun deleteByMilestoneIdentifier(milestoneIdentifier: UUID) {
    deleteBySourceOrTarget(milestoneIdentifier)
  }

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional(propagation = Propagation.MANDATORY)
  open fun deleteByTaskIdentifier(taskIdentifier: UUID) {
    deleteBySourceOrTarget(taskIdentifier)
  }

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional(propagation = Propagation.MANDATORY)
  open fun deleteByProjectId(projectId: Long) {
    val relations = relationRepository.findAllByProjectId(projectId)
    if (relations.isNotEmpty()) {
      relationRepository.deleteAllInBatch(relations)
    }
  }

  private fun assertDoesNotExist(relation: Relation) {
    val exists =
        with(relation) {
          relationRepository
              .existsByTypeAndSourceIdentifierAndSourceTypeAndTargetIdentifierAndTargetType(
                  type, source.identifier, source.type, target.identifier, target.type)
        }
    if (exists) {
      throw DuplicateEntityException(COMMON_VALIDATION_ERROR_ENTITY_DUPLICATED)
    }
  }

  private fun assertDatesExistForSourceAndTarget(relation: Relation) {
    val dates = dateLookupComponent.findDates(relation)
    Assert.isTrue(
        dates.size == 2,
        "The source or target is missing a date. " +
            "A finish-to-start relation must have dates set on both.")
    dates.forEach {
      Assert.isTrue(it.start != null, "The start date is missing on the source or target.")
      Assert.isTrue(it.end != null, "The end date is missing on the source or target.")
    }
  }

  private fun assertDatesExistForSourceAndTarget(relations: List<Relation>) {
    val dates = dateLookupComponent.findDates(relations)
    val datesByIdentifier = dates.associateBy { it.identifier }

    // Verify that milestones or tasks exist on a specified date.
    // Could be simplified by just checking the amount of dates matches the number
    // of relation x 2 (source + target)
    relations.forEach { relation ->
      val sourceDate = datesByIdentifier[relation.source.identifier]
      requireNotNull(sourceDate)
      requireNotNull(sourceDate.start) { "The start date is missing on the source." }
      requireNotNull(sourceDate.end) { "The end date is missing on the source." }

      val targetDate = datesByIdentifier[relation.target.identifier]
      requireNotNull(targetDate)
      requireNotNull(targetDate.start) { "The start date is missing on the target." }
      requireNotNull(targetDate.end) { "The end date is missing on the target." }
    }
  }

  private fun assertSourceAndTargetDifferent(relationDto: RelationDto) =
      Assert.isTrue(
          relationDto.source != relationDto.target,
          "The relation source and target must be different.")

  private fun assertValidPartOfRelation(relationDto: RelationDto) =
      Assert.isTrue(
          relationDto.source.type == TASK && relationDto.target.type == MILESTONE,
          "A part-of relation must have a task source and a milestone target.")

  private fun existsOrFail(element: RelationElementDto, projectIdentifier: ProjectId) {
    if (!relationElementLookupComponent.exists(element.id, element.type, projectIdentifier)) {
      throw PreconditionViolationException(RELATION_VALIDATION_ERROR_ELEMENT_NOT_FOUND)
    }
  }

  private fun allExistOrFail(relationElementFilter: RelationElementFilter) {
    val foundIdentifiers = relationElementLookupComponent.find(relationElementFilter).toSet()

    if (relationElementFilter.identifiers != foundIdentifiers) {
      val missing = relationElementFilter.identifiers - foundIdentifiers
      throw AggregateNotFoundException(
          RELATION_VALIDATION_ERROR_NOT_FOUND, missing.first().toString())
    }
  }

  private fun findRelationOrFail(identifier: UUID, projectIdentifier: ProjectId) =
      relationRepository.findOneByIdentifierAndProjectIdentifier(identifier, projectIdentifier)
          ?: throw AggregateNotFoundException(
              RELATION_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  private fun findRelationWithDetailsOrFail(identifier: UUID, projectIdentifier: ProjectId) =
      relationRepository.findOneWithDetailsByIdentifierAndProjectIdentifier(
          identifier, projectIdentifier)
          ?: throw AggregateNotFoundException(
              RELATION_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

  private fun findAllRelationsWithDetails(
      identifiers: Collection<UUID>,
      projectIdentifier: ProjectId
  ) =
      relationRepository.findAllWithDetailsByIdentifierInAndProjectIdentifier(
          identifiers, projectIdentifier)

  private fun findProjectOrFail(projectIdentifier: ProjectId): Project =
      projectRepository.findOneByIdentifier(projectIdentifier)
          ?: throw PreconditionViolationException(COMMON_VALIDATION_ERROR_PROJECT_NOT_FOUND)

  private fun deleteBySourceOrTarget(identifier: UUID) {
    val relations = relationRepository.findAllBySourceOrTarget(identifier)
    if (relations.isNotEmpty()) {
      relationRepository.deleteAll(relations, DELETED)
    }
  }
}
