/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.graphql.resource.response.ProjectCraftPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.model.ProjectCraft
import org.springframework.stereotype.Component

@Component
class ProjectCraftPayloadAssembler {

  fun assemble(projectCraft: ProjectCraft): ProjectCraftPayloadV1 =
      ProjectCraftPayloadMapper.INSTANCE.fromProjectCraft(projectCraft)
}
