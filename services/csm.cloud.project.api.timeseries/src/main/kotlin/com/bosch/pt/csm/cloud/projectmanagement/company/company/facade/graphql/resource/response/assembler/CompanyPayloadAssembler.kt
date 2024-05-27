/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.graphql.resource.response.CompanyPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.model.Company
import org.springframework.stereotype.Component

@Component
class CompanyPayloadAssembler {

  fun assemble(company: Company): CompanyPayloadV1 =
      CompanyPayloadMapper.INSTANCE.fromCompany(company)
}
