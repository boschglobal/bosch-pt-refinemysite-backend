/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.rest.resource.response.MilestoneListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.model.Milestone
import org.springframework.stereotype.Component

@Component
class MilestoneListResourceAssembler(
    private val milestoneResourceAssembler: MilestoneResourceAssembler
) {

  fun assemble(milestones: List<Milestone>, latestOnly: Boolean): MilestoneListResource =
      MilestoneListResource(
          milestones
              .flatMap { milestoneResourceAssembler.assemble(it, latestOnly) }
              .sortedWith(compareBy({ it.id.value }, { it.version }, { it.eventTimestamp })))
}
