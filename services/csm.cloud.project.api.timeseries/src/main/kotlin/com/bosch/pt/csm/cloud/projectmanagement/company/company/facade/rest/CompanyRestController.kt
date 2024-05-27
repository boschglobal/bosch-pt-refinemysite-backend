/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.rest.resource.response.CompanyListResource
import com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.rest.resource.response.assembler.CompanyListResourceAssembler
import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.service.CompanyQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.model.projects
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.service.ParticipantQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@ApiVersion
class CompanyRestController(
    private val companyListResourceAssembler: CompanyListResourceAssembler,
    private val companyQueryService: CompanyQueryService,
    private val participantQueryService: ParticipantQueryService
) {

  companion object {
    const val COMPANIES_ENDPOINT = "/companies"
  }

  @GetMapping(COMPANIES_ENDPOINT)
  fun findCompanies(): ResponseEntity<CompanyListResource> {
    val projectIds = participantQueryService.findActiveParticipantsOfCurrentUser().projects()
    val projectParticipants = participantQueryService.findParticipantsOfProjects(projectIds)

    val companies = companyQueryService.findAllByIdentifiers(projectParticipants.map { it.company })
    return ResponseEntity.ok().body(companyListResourceAssembler.assemble(companies))
  }
}
