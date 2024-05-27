/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.command.service

import com.bosch.pt.iot.smartsite.project.milestone.command.api.UpdateMilestoneCommand
import com.bosch.pt.iot.smartsite.project.milestone.command.handler.batch.UpdateMilestoneBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.MilestoneRescheduleResult
import com.bosch.pt.iot.smartsite.project.milestone.shared.dto.SearchMilestonesDto
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneRepository
import datadog.trace.api.Trace
import org.springframework.data.domain.Pageable.unpaged
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class MilestoneRescheduleService(
    private val updateMilestoneBatchCommandHandler: UpdateMilestoneBatchCommandHandler,
    private val milestoneRepository: MilestoneRepository
) {

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize(
      "@milestoneAuthorizationComponent.hasReschedulePermissionOnProject(#criteria.projectIdentifier)")
  open fun validateMilestones(criteria: SearchMilestonesDto): MilestoneRescheduleResult {
    val affectedMilestones = findAffectedMilestoneIdentifiers(criteria)

    return MilestoneRescheduleResult(successful = affectedMilestones)
  }

  @Trace
  @Transactional
  @PreAuthorize(
      "@milestoneAuthorizationComponent.hasReschedulePermissionOnProject(#criteria.projectIdentifier)")
  open fun rescheduleMilestones(
      shiftDays: Long,
      criteria: SearchMilestonesDto
  ): MilestoneRescheduleResult {
    val affectedMilestones = findAffectedMilestones(findAffectedMilestoneIdentifiers(criteria))

    val updateCommands =
        affectedMilestones.map {
          UpdateMilestoneCommand(
              it.identifier,
              it.version,
              it.name,
              it.type,
              it.date.plusDays(shiftDays),
              it.header,
              it.description,
              it.craft?.identifier,
              it.workArea?.identifier,
              it.position)
        }
    return MilestoneRescheduleResult(
        successful =
            updateMilestoneBatchCommandHandler.handle(criteria.projectIdentifier!!, updateCommands))
  }

  private fun findAffectedMilestoneIdentifiers(criteria: SearchMilestonesDto): List<MilestoneId> =
      milestoneRepository.findMilestoneIdentifiersForFilters(
          criteria.toMilestoneFilterDto(), unpaged())

  private fun findAffectedMilestones(milestoneIdentifiers: List<MilestoneId>): List<Milestone> =
      milestoneRepository.findAllWithDetailsByIdentifierIn(milestoneIdentifiers)
}
