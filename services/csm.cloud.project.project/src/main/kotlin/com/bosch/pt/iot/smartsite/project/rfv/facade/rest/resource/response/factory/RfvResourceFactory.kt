/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.rfv.boundary.dto.RfvDto
import org.springframework.stereotype.Component

@Component
open class RfvResourceFactory(private val factoryHelper: RfvResourceFactoryHelper) {

  open fun build(projectIdentifier: ProjectId, rfv: RfvDto) =
      factoryHelper.build(projectIdentifier, listOf(rfv)).first()
}
