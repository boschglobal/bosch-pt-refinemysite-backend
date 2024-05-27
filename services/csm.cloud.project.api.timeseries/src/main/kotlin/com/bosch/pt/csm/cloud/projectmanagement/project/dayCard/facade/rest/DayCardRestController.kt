/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.rest.resource.response.DayCardListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.rest.resource.response.assembler.DayCardListResourceAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.service.DayCardQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.projects
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service.ParticipantQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.service.TaskScheduleQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@ApiVersion
class DayCardRestController(
    private val dayCardListResourceAssembler: DayCardListResourceAssembler,
    private val dayCardQueryService: DayCardQueryService,
    private val participantQueryService: ParticipantQueryService,
    private val taskScheduleQueryService: TaskScheduleQueryService
) {

  companion object {
    const val DAY_CARDS_ENDPOINT = "/projects/tasks/schedules/daycards"
  }

  @GetMapping(DAY_CARDS_ENDPOINT)
  fun findDayCards(
      @RequestParam(required = false) latestOnly: Boolean
  ): ResponseEntity<DayCardListResource> {
    val projectIds = participantQueryService.findActiveParticipantsOfCurrentUser().projects()

    val schedules = taskScheduleQueryService.findAllByProjects(projectIds).associateBy { it.task }
    val dayCards =
        when (latestOnly) {
          true -> dayCardQueryService.findAllByProjectsAndDeletedFalse(projectIds)
          else -> dayCardQueryService.findAllByProjects(projectIds)
        }
    val dayCardSchedules = dayCards.associate { it.identifier to schedules[it.task] }

    return ResponseEntity.ok()
        .body(dayCardListResourceAssembler.assemble(dayCards, dayCardSchedules, latestOnly))
  }
}
