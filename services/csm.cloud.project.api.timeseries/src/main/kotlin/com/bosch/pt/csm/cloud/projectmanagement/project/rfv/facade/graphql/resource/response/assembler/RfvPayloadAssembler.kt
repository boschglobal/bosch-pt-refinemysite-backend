/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.graphql.resource.response.RfvPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.model.ProjectRfvs
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.model.Rfv
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component

@Component
class RfvPayloadAssembler(private val messageSource: MessageSource) {

  fun assemble(projectRfvs: ProjectRfvs): List<RfvPayloadV1> {
    val missingRfvs = getMissingRfvs(projectRfvs)

    return (missingRfvs +
            projectRfvs.rfvs.map {
              RfvPayloadMapper.INSTANCE.fromRfv(it, translate(it.reason, it))
            })
        .sortedBy { it.reason }
  }

  private fun getMissingRfvs(projectRfvs: ProjectRfvs): List<RfvPayloadV1> {
    val missingRfvs =
        DayCardReasonEnum.values().toSet() - projectRfvs.rfvs.map { it.reason }.toSet()

    return missingRfvs.map {
      RfvPayloadV1(
          id = it.id,
          version = -1L,
          reason = it.shortKey,
          name = translate(it, null),
          active = !it.isCustom,
          eventDate = it.timestamp.toLocalDateTimeByMillis())
    }
  }

  private fun translate(reason: DayCardReasonEnum, rfv: Rfv?): String =
      rfv?.name
          ?: messageSource.getMessage(reason.messageKey, null, LocaleContextHolder.getLocale())
}
