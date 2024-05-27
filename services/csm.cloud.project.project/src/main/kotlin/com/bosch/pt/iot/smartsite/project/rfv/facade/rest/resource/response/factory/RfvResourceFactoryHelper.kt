/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.rfv.boundary.dto.RfvDto
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.RfvController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.RfvController.Companion.RFVS_BY_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.response.RfvResource
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.response.RfvResource.Companion.LINK_RFV_ACTIVATE
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.response.RfvResource.Companion.LINK_RFV_DEACTIVATE
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.response.RfvResource.Companion.LINK_RFV_UPDATE
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component

@Component
open class RfvResourceFactoryHelper(
    private val linkFactory: CustomLinkBuilderFactory,
    private val messageSource: MessageSource
) : AbstractResourceFactoryHelper(messageSource) {

  open fun build(projectIdentifier: ProjectId, rfvs: List<RfvDto>): List<RfvResource> =
      rfvs.map { buildRfvResource(projectIdentifier, it) }

  private fun buildRfvResource(projectIdentifier: ProjectId, rfv: RfvDto): RfvResource {
    val translatedName =
        rfv.name
            ?: messageSource.getMessage(
                "DayCardReasonEnum_" + rfv.key.name, null, LocaleContextHolder.getLocale())

    return RfvResource(rfv.key, rfv.active, translatedName).apply {
      val updateRel = if (this.active) LINK_RFV_DEACTIVATE else LINK_RFV_ACTIVATE
      this.add(
          linkFactory
              .linkTo(RFVS_BY_PROJECT_ID)
              .withParameters(mapOf(PATH_VARIABLE_PROJECT_ID to projectIdentifier))
              .withRel((updateRel)))

      this.addIf(rfv.key.isCustom) {
        linkFactory
            .linkTo(RFVS_BY_PROJECT_ID)
            .withParameters(mapOf(PATH_VARIABLE_PROJECT_ID to projectIdentifier))
            .withRel(LINK_RFV_UPDATE)
      }
    }
  }
}
