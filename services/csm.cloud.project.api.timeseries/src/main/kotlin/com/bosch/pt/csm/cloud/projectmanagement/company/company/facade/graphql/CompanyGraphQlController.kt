/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.graphql

import com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.graphql.resource.response.CompanyPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.graphql.resource.response.assembler.CompanyPayloadAssembler
import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.service.CompanyQueryService
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.facade.graphql.resource.response.ParticipantPayloadV1
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.stereotype.Controller

@Controller
class CompanyGraphQlController(
    private val companyPayloadAssembler: CompanyPayloadAssembler,
    private val companyQueryService: CompanyQueryService
) {

  @BatchMapping
  fun company(
      participants: List<ParticipantPayloadV1>
  ): Map<ParticipantPayloadV1, CompanyPayloadV1?> {
    val companies =
        companyQueryService
            .findAllByIdentifiersAndDeletedFalse(participants.map { it.companyId })
            .associateBy { it.identifier }

    return participants.associateWith {
      companies[it.companyId]?.let { companyPayloadAssembler.assemble(it) }
    }
  }
}
