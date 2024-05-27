/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.rest.resource.response.WorkAreaListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.model.WorkArea
import org.springframework.stereotype.Component

@Component
class WorkAreaListResourceAssembler(
    private val workAreaResourceAssembler: WorkAreaResourceAssembler
) {

  fun assemble(workAreas: List<WorkArea>, latestOnly: Boolean): WorkAreaListResource =
      WorkAreaListResource(
          workAreas
              .flatMap { workAreaResourceAssembler.assemble(it, latestOnly) }
              .sortedWith(compareBy({ it.id.value }, { it.version }, { it.eventTimestamp })))
}
