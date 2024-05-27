/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.reschedule.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.common.util.AbstractSnapshotEntityUtilities.sortByIdentifier
import com.bosch.pt.iot.smartsite.project.reschedule.command.dto.RescheduleResultDto
import com.bosch.pt.iot.smartsite.project.reschedule.command.dto.RescheduleResultDto.FailedDto
import com.bosch.pt.iot.smartsite.project.reschedule.command.dto.RescheduleResultDto.SuccessfulDto
import com.bosch.pt.iot.smartsite.project.reschedule.facade.rest.resource.response.RescheduleResultResource
import com.bosch.pt.iot.smartsite.project.reschedule.facade.rest.resource.response.RescheduleResultResource.FailedResource
import com.bosch.pt.iot.smartsite.project.reschedule.facade.rest.resource.response.RescheduleResultResource.SuccessfulResource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
open class RescheduleResourceFactory {

  open fun build(rescheduleResultDto: RescheduleResultDto) =
      buildRescheduleResource(rescheduleResultDto)

  private fun buildRescheduleResource(rescheduleResultDto: RescheduleResultDto) =
      RescheduleResultResource(
          buildSuccessfulResource(rescheduleResultDto.successful),
          buildFailedResource(rescheduleResultDto.failed))

  private fun buildSuccessfulResource(successfulDto: SuccessfulDto) =
      SuccessfulResource(
          successfulDto.milestones.sortByIdentifier(),
          successfulDto.tasks.map { it.identifier }.sorted())

  private fun buildFailedResource(failedDto: FailedDto) =
      FailedResource(
          failedDto.milestones.sortByIdentifier(), failedDto.tasks.map { it.identifier }.sorted())
}
