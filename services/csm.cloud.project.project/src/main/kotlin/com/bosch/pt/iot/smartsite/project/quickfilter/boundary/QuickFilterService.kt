/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.quickfilter.boundary

import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.iot.smartsite.application.security.AuthorizationUtils.getCurrentUser
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.verify
import com.bosch.pt.iot.smartsite.common.i18n.Key.QUICK_FILTER_VALIDATION_ERROR_MAX_NUMBER_REACHED
import com.bosch.pt.iot.smartsite.common.i18n.Key.QUICK_FILTER_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.iot.smartsite.company.repository.CompanyRepository
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftRepository
import com.bosch.pt.iot.smartsite.project.quickfilter.boundary.dto.QuickFilterDto
import com.bosch.pt.iot.smartsite.project.quickfilter.domain.QuickFilterId
import com.bosch.pt.iot.smartsite.project.quickfilter.model.AssigneesCriteria
import com.bosch.pt.iot.smartsite.project.quickfilter.model.MilestoneCriteria
import com.bosch.pt.iot.smartsite.project.quickfilter.model.MilestoneTypes
import com.bosch.pt.iot.smartsite.project.quickfilter.model.QuickFilter
import com.bosch.pt.iot.smartsite.project.quickfilter.model.QuickFilter.Companion.MAX_QUICK_FILTERS_PER_PARTICIPANT_IN_PROJECT
import com.bosch.pt.iot.smartsite.project.quickfilter.model.TaskCriteria
import com.bosch.pt.iot.smartsite.project.quickfilter.model.WorkAreas
import com.bosch.pt.iot.smartsite.project.quickfilter.repository.QuickFilterRepository
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaRepository
import datadog.trace.api.Trace
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class QuickFilterService(
    private val quickFilterRepository: QuickFilterRepository,
    private val participantRepository: ParticipantRepository,
    private val companyRepository: CompanyRepository,
    private val projectCraftRepository: ProjectCraftRepository,
    private val workAreaRepository: WorkAreaRepository
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@projectAuthorizationComponent.hasReadPermissionOnProject(#quickFilterDto.projectRef)")
  open fun save(quickFilterDto: QuickFilterDto): QuickFilterId {

    val participantIdentifier = getUserParticipantIdentifier(quickFilterDto.projectRef)

    if (quickFilterRepository.countAllByParticipantIdentifierAndProjectIdentifier(
        participantIdentifier, quickFilterDto.projectRef) >=
        MAX_QUICK_FILTERS_PER_PARTICIPANT_IN_PROJECT) {
      throw PreconditionViolationException(QUICK_FILTER_VALIDATION_ERROR_MAX_NUMBER_REACHED)
    }

    val quickFilter =
        quickFilterDto.toDocument(participantIdentifier).apply {
          removeInvalidFilterCriteriaFrom(this)
        }

    return quickFilterRepository.save(quickFilter).identifier
  }

  @Trace
  @Transactional(readOnly = true)
  @NoPreAuthorize(usedByController = true)
  open fun findOne(identifier: QuickFilterId, projectRef: ProjectId) =
      quickFilterRepository.findOneByIdentifierAndProjectIdentifier(identifier, projectRef)!!

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProject(#projectRef)")
  open fun findAllForCurrentUser(projectRef: ProjectId): List<QuickFilter> {
    val quickFilters =
        quickFilterRepository.findAllByParticipantIdentifierAndProjectIdentifierOrderByNameAsc(
            getUserParticipantIdentifier(projectRef), projectRef)

    return quickFilters.apply { removeInvalidFilterCriteriaFrom(this) }
  }

  @Trace
  @Transactional
  @PreAuthorize(
      "@quickFilterAuthorizationComponent" +
          ".hasUpdateAndDeletePermissionOnQuickFilter(#identifier, #quickFilterDto.projectRef)")
  open fun update(
      identifier: QuickFilterId,
      quickFilterDto: QuickFilterDto,
      etag: ETag
  ): QuickFilterId {

    val existingQuickFilter =
        quickFilterRepository.findOneByIdentifierAndProjectIdentifier(
            identifier, quickFilterDto.projectRef)
            ?: throw AggregateNotFoundException(
                QUICK_FILTER_VALIDATION_ERROR_NOT_FOUND, identifier.toString())

    etag.verify(existingQuickFilter.version!!)

    existingQuickFilter.updateFrom(quickFilterDto).apply { removeInvalidFilterCriteriaFrom(this) }

    return quickFilterRepository.save(existingQuickFilter).identifier
  }

  @Trace
  @Transactional
  @PreAuthorize(
      "@quickFilterAuthorizationComponent.hasUpdateAndDeletePermissionOnQuickFilter(#identifier, #projectRef)")
  open fun delete(identifier: QuickFilterId, projectRef: ProjectId) =
      quickFilterRepository.deleteByIdentifierAndProjectIdentifier(identifier, projectRef)

  @Trace
  @NoPreAuthorize
  @Transactional
  open fun deleteAllByProjectIdentifier(projectRef: ProjectId) =
      quickFilterRepository.deleteAllByProjectIdentifier(projectRef)

  private fun removeInvalidFilterCriteriaFrom(quickFilters: QuickFilter) =
      removeInvalidFilterCriteriaFrom(listOf(quickFilters))

  private fun removeInvalidFilterCriteriaFrom(quickFilters: Collection<QuickFilter>) {
    val assigneeIdentifiers =
        quickFilters.flatMap { it.taskCriteria.assignees.participantIds }.distinct().toSet()
    val companyIdentifiers =
        quickFilters.flatMap { it.taskCriteria.assignees.companyIds }.distinct()
    val craftIdentifiers =
        quickFilters
            .flatMap {
              it.taskCriteria.projectCraftIds.plus(
                  it.milestoneCriteria.milestoneTypes.projectCraftIds)
            }
            .distinct()
    val workAreaIdentifiers =
        quickFilters
            .flatMap {
              it.taskCriteria.workAreaIds.plus(it.milestoneCriteria.workAreas.workAreaIds)
            }
            .filterNot(WorkAreaIdOrEmpty::isEmpty)
            .map { it.identifier!! }
            .distinct()

    val validAssigneeIdentifiers =
        participantRepository.validateExistingIdentifiersFor(assigneeIdentifiers)
    val validCompanyIdentifiers =
        companyRepository.validateExistingIdentifiersFor(companyIdentifiers)
    val validCraftIdentifiers =
        projectCraftRepository.validateExistingIdentifiersFor(craftIdentifiers)
    val validWorkAreaIdentifiers =
        workAreaRepository
            .validateExistingIdentifiersFor(workAreaIdentifiers)
            .map(::WorkAreaIdOrEmpty)

    quickFilters.forEach {

      // At the moment the web client sends the same work area identifier for the task and milestone
      // criteria but there is nothing prohibiting it to be different in our actual data model
      val cleanedWorkAreasForTask =
          it.taskCriteria.workAreaIds.intersect(validWorkAreaIdentifiers.toSet()).toMutableSet()
      if (it.taskCriteria.workAreaIds.any(WorkAreaIdOrEmpty::isEmpty)) {
        cleanedWorkAreasForTask.add(WorkAreaIdOrEmpty())
      }

      val cleanedWorkAreasForMilestone =
          it.milestoneCriteria.workAreas.workAreaIds
              .intersect(validWorkAreaIdentifiers.toSet())
              .toMutableSet()
      if (it.milestoneCriteria.workAreas.workAreaIds.any(WorkAreaIdOrEmpty::isEmpty)) {
        cleanedWorkAreasForMilestone.add(WorkAreaIdOrEmpty())
      }

      it.taskCriteria =
          it.taskCriteria.copy(
              assignees =
                  AssigneesCriteria(
                      participantIds =
                          it.taskCriteria.assignees.participantIds.intersect(
                              validAssigneeIdentifiers.toSet()),
                      companyIds =
                          it.taskCriteria.assignees.companyIds.intersect(
                              validCompanyIdentifiers.toSet())),
              projectCraftIds =
                  it.taskCriteria.projectCraftIds.intersect(validCraftIdentifiers.toSet()),
              workAreaIds = cleanedWorkAreasForTask)

      it.milestoneCriteria =
          it.milestoneCriteria.copy(
              milestoneTypes =
                  MilestoneTypes(
                      types = it.milestoneCriteria.milestoneTypes.types,
                      projectCraftIds =
                          it.milestoneCriteria.milestoneTypes.projectCraftIds.intersect(
                              validCraftIdentifiers.toSet())),
              workAreas =
                  WorkAreas(
                      header = it.milestoneCriteria.workAreas.header,
                      workAreaIds = cleanedWorkAreasForMilestone))
    }
  }

  private fun getUserParticipantIdentifier(projectRef: ProjectId) =
      participantRepository
          .findOneByUserIdentifierAndProjectIdentifierAndActiveTrue(
              getCurrentUser().identifier!!, projectRef)!!
          .identifier

  private fun QuickFilter.updateFrom(quickFilterDto: QuickFilterDto) =
      this.apply {
        name = quickFilterDto.name
        highlight = quickFilterDto.highlight
        projectIdentifier = quickFilterDto.projectRef
        useTaskCriteria = quickFilterDto.useTaskCriteria
        useMilestoneCriteria = quickFilterDto.useMilestoneCriteria
        milestoneCriteria =
            MilestoneCriteria(
                from = quickFilterDto.milestoneCriteria.from,
                to = quickFilterDto.milestoneCriteria.to,
                workAreas =
                    WorkAreas(
                        header = quickFilterDto.milestoneCriteria.workAreas.header,
                        workAreaIds =
                            quickFilterDto.milestoneCriteria.workAreas.workAreaIdentifiers),
                milestoneTypes =
                    MilestoneTypes(
                        types = quickFilterDto.milestoneCriteria.typesFilter.types,
                        projectCraftIds =
                            quickFilterDto.milestoneCriteria.typesFilter.craftIdentifiers))
        taskCriteria =
            TaskCriteria(
                from = quickFilterDto.taskCriteria.rangeStartDate,
                to = quickFilterDto.taskCriteria.rangeEndDate,
                workAreaIds = quickFilterDto.taskCriteria.workAreaIdentifiers.toSet(),
                projectCraftIds = quickFilterDto.taskCriteria.projectCraftIdentifiers.toSet(),
                allDaysInDateRange = quickFilterDto.taskCriteria.allDaysInDateRange,
                status = quickFilterDto.taskCriteria.taskStatus.toSet(),
                assignees =
                    AssigneesCriteria(
                        participantIds =
                            quickFilterDto.taskCriteria.assignees.participantIdentifiers.toSet(),
                        companyIds =
                            quickFilterDto.taskCriteria.assignees.companyIdentifiers.toSet()),
                hasTopics = quickFilterDto.taskCriteria.hasTopics,
                topicCriticality = quickFilterDto.taskCriteria.topicCriticality.toSet())
      }
}
