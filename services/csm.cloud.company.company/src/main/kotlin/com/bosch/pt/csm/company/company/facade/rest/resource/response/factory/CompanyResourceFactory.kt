/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.facade.rest.resource.response.factory

import com.bosch.pt.csm.company.company.facade.rest.resource.response.CompanyResource
import com.bosch.pt.csm.company.company.shared.model.Company
import org.springframework.stereotype.Component

@Component
class CompanyResourceFactory(
    private val companyResourceFactoryHelper: CompanyResourceFactoryHelper
) {
  fun build(company: Company): CompanyResource =
      companyResourceFactoryHelper.build(listOf(company)).first()
}
