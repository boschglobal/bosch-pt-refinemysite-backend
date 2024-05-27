/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.domain.DayCardId
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.rest.resource.response.DayCardListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCard
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.model.TaskSchedule
import org.springframework.stereotype.Component

@Component
class DayCardListResourceAssembler(private val dayCardResourceAssembler: DayCardResourceAssembler) {

  fun assemble(
      dayCards: List<DayCard>,
      schedules: Map<DayCardId, TaskSchedule?>,
      latestOnly: Boolean
  ): DayCardListResource =
      if (latestOnly) {
        DayCardListResource(
            dayCards
                .mapNotNull {
                  dayCardResourceAssembler.assembleLatest(it, schedules[it.identifier])
                }
                .sortedWith(compareBy({ it.id.value }, { it.version }, { it.eventTimestamp })))
      } else {
        DayCardListResource(
            dayCards
                .flatMap { dayCardResourceAssembler.assemble(it, schedules[it.identifier]) }
                .sortedWith(compareBy({ it.id.value }, { it.version }, { it.eventTimestamp })))
      }
}
