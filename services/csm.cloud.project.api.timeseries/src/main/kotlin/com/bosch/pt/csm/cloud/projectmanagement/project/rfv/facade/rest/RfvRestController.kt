/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.projects
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service.ParticipantQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.rest.resource.response.RfvListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.rest.resource.response.assembler.RfvListResourceAssembler
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.service.RfvQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@ApiVersion
class RfvRestController(
    private val participantQueryService: ParticipantQueryService,
    private val rfvListResourceAssembler: RfvListResourceAssembler,
    private val rfvQueryService: RfvQueryService,
) {

  companion object {
    const val RFVS_ENDPOINT = "/projects/rfvs"
  }

  @GetMapping(RFVS_ENDPOINT)
  fun findRfvs(
      @RequestParam(required = false) latestOnly: Boolean
  ): ResponseEntity<RfvListResource> {
    val projectIds = participantQueryService.findActiveParticipantsOfCurrentUser().projects()
    val rfvs =
        when (latestOnly) {
          true -> rfvQueryService.findAllByProjectsAndDeletedFalseWithMissing(projectIds)
          else -> rfvQueryService.findAllByProjectsWithMissing(projectIds)
        }

    return ResponseEntity.ok().body(rfvListResourceAssembler.assemble(rfvs, latestOnly))
  }
}
