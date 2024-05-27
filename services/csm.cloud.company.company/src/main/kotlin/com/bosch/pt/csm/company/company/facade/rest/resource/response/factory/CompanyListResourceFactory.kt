/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.factory.PageLinks
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ListResponseResource
import com.bosch.pt.csm.company.company.facade.rest.CompanyController.Companion.COMPANIES_ENDPOINT_PATH
import com.bosch.pt.csm.company.company.facade.rest.resource.response.CompanyResource
import com.bosch.pt.csm.company.company.shared.model.Company
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class CompanyListResourceFactory(
    private val companyResourceFactoryHelper: CompanyResourceFactoryHelper,
    private val linkFactory: CustomLinkBuilderFactory
) {

  @PageLinks
  fun build(companies: Page<Company>): ListResponseResource<CompanyResource> {
    val companyResources = companyResourceFactoryHelper.build(companies.content)

    return ListResponseResource(
            companyResources,
            companies.number,
            companies.size,
            companies.totalPages,
            companies.totalElements)
        .apply { add(linkFactory.linkTo(COMPANIES_ENDPOINT_PATH).withSelfRel()) }
  }
}
