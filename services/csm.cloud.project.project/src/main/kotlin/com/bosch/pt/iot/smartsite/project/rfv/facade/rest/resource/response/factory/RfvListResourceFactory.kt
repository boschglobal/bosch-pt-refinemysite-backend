/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.rfv.boundary.dto.RfvDto
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.response.RfvResource
import org.springframework.stereotype.Component

@Component
open class RfvListResourceFactory(private val rfvResourceFactoryHelper: RfvResourceFactoryHelper) {

  open fun build(
      projectIdentifier: ProjectId,
      rfvs: List<RfvDto>
  ): BatchResponseResource<RfvResource> =
      BatchResponseResource(rfvResourceFactoryHelper.build(projectIdentifier, rfvs))
}
