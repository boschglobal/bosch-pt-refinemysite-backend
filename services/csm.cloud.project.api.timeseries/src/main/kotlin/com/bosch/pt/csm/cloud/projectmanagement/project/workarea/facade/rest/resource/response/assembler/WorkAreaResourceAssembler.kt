/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.rest.resource.response.WorkAreaResource
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.model.WorkArea
import org.springframework.stereotype.Component

@Component
class WorkAreaResourceAssembler {

  fun assemble(workArea: WorkArea, latestOnly: Boolean): List<WorkAreaResource> =
      if (latestOnly) {
        listOf(
            WorkAreaResourceMapper.INSTANCE.fromWorkAreaVersion(
                workArea.history.last(), workArea.project, workArea.identifier))
      } else {
        workArea.history.map {
          WorkAreaResourceMapper.INSTANCE.fromWorkAreaVersion(
              it, workArea.project, workArea.identifier)
        }
      }
}
