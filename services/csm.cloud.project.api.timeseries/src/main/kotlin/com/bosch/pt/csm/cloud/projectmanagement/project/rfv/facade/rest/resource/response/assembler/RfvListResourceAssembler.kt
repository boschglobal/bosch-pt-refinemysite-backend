/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.rest.resource.response.RfvListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.model.ProjectRfvs
import org.springframework.stereotype.Component

@Component
class RfvListResourceAssembler(private val rfvResourceAssembler: RfvResourceAssembler) {

  fun assemble(rfvs: List<ProjectRfvs>, latestOnly: Boolean): RfvListResource =
      if (latestOnly) {
        RfvListResource(
            rfvs
                .flatMap { rfvResourceAssembler.assembleLatest(it) }
                .sortedWith(
                    compareBy(
                        { it.reason }, { it.version }, { it.id.value }, { it.eventTimestamp })))
      } else {
        RfvListResource(
            rfvs
                .flatMap { rfvResourceAssembler.assemble(it) }
                .sortedWith(
                    compareBy(
                        { it.reason }, { it.version }, { it.id.value }, { it.eventTimestamp })))
      }
}
