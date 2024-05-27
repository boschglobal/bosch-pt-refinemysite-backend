/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.rest.resource.response.MilestoneResource
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.model.Milestone
import org.springframework.stereotype.Component

@Component
class MilestoneResourceAssembler {

  fun assemble(milestone: Milestone, latestOnly: Boolean): List<MilestoneResource> =
      if (latestOnly) {
        listOf(
            MilestoneResourceMapper.INSTANCE.fromMilestoneVersion(
                milestone.history.last(), milestone.project, milestone.identifier))
      } else {
        milestone.history.map { milestoneVersion ->
          MilestoneResourceMapper.INSTANCE.fromMilestoneVersion(
              milestoneVersion, milestone.project, milestone.identifier)
        }
      }
}
