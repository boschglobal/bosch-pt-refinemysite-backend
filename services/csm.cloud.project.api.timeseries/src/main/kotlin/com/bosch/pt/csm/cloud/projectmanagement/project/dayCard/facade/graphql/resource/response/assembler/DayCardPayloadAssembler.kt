/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.graphql.resource.response.DayCardPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.graphql.resource.response.DayCardReasonPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCard
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.model.Rfv
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.model.TaskSchedule
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component

@Component
class DayCardPayloadAssembler(private val messageSource: MessageSource) {

  fun assemble(
      dayCard: DayCard,
      rfvs: Map<ProjectId, Map<DayCardReasonEnum, Rfv>>,
      taskSchedule: TaskSchedule?
  ): DayCardPayloadV1? {
    if (taskSchedule == null) {
      return null
    }

    val translatedReason =
        dayCard.reason
            ?.let { Pair(it, rfvs[dayCard.project]?.get(it)) }
            ?.let { DayCardReasonPayloadV1(it.first.shortKey, translate(it.first, it.second)) }

    return DayCardPayloadMapper.INSTANCE.fromDayCard(dayCard, translatedReason)
  }

  private fun translate(reason: DayCardReasonEnum, rfv: Rfv?): String =
      rfv?.name
          ?: messageSource.getMessage(reason.messageKey, null, LocaleContextHolder.getLocale())
}
