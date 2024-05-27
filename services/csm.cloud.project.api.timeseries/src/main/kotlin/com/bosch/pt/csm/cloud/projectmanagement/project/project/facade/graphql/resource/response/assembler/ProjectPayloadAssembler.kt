/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.graphql.resource.response.ProjectPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.model.Project
import org.springframework.stereotype.Component

@Component
class ProjectPayloadAssembler {

  fun assemble(project: Project): ProjectPayloadV1 =
      ProjectPayloadMapper.INSTANCE.fromProject(project, project.category?.shortKey)
}
