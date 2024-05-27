/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.graphql.resource.response.WorkAreaPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.model.WorkArea
import org.springframework.stereotype.Component

@Component
class WorkAreaPayloadAssembler {

  fun assemble(workArea: WorkArea): WorkAreaPayloadV1 =
      WorkAreaPayloadMapper.INSTANCE.fromWorkArea(workArea)
}
