/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.facade.graphql.resource.response.WorkAreaListPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.query.model.WorkAreaList
import org.springframework.stereotype.Component

@Component
class WorkAreaListPayloadAssembler {

  fun assemble(workAreaList: WorkAreaList): WorkAreaListPayloadV1 =
      WorkAreaListPayloadMapper.INSTANCE.fromWorkAreaList(
          workAreaList, workAreaList.workAreas.map { it.value })
}
