/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.facade.graphql.resource.response.MilestonePayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.model.Milestone
import org.springframework.stereotype.Component

@Component
class MilestonePayloadAssembler {

  fun assemble(milestone: Milestone, critical: Boolean?): MilestonePayloadV1 =
      MilestonePayloadMapper.INSTANCE.fromMilestone(milestone, critical)
}
