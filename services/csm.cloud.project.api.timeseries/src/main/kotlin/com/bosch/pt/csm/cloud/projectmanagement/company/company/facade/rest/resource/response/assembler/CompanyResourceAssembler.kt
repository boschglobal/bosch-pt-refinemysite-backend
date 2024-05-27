/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.rest.resource.response.CompanyResource
import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.model.Company
import org.springframework.stereotype.Component

@Component
class CompanyResourceAssembler {

  fun assemble(company: Company): CompanyResource =
      company.history.last().let {
        CompanyResourceMapper.INSTANCE.fromCompanyVersion(it, company.identifier)
      }
}
