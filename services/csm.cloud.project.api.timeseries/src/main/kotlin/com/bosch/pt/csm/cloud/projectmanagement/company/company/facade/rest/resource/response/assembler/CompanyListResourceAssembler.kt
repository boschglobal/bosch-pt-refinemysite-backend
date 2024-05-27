/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.rest.resource.response.CompanyListResource
import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.model.Company
import org.springframework.stereotype.Component

@Component
class CompanyListResourceAssembler(private val companyResourceAssembler: CompanyResourceAssembler) {

  fun assemble(companies: List<Company>): CompanyListResource =
      CompanyListResource(
          companies
              .map { companyResourceAssembler.assemble(it) }
              .sortedWith(compareBy({ it.id.value }, { it.version }, { it.eventTimestamp })))
}
