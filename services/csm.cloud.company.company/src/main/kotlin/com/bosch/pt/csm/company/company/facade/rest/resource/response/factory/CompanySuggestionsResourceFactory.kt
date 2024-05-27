/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ListResponseResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.company.company.facade.rest.CompanyController.Companion.COMPANY_SUGGESTIONS_ENDPOINT_PATH
import com.bosch.pt.csm.company.company.shared.model.Company
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class CompanySuggestionsResourceFactory(private val linkFactory: CustomLinkBuilderFactory) {

  fun build(companies: Page<Company>): ListResponseResource<ResourceReference> {
    val suggestions: List<ResourceReference> = companies.map { ResourceReference.from(it) }.toList()

    val resource =
        ListResponseResource(
            suggestions,
            companies.number,
            companies.size,
            companies.totalPages,
            companies.totalElements)

    // Add self reference
    resource.add(linkFactory.linkTo(COMPANY_SUGGESTIONS_ENDPOINT_PATH).withSelfRel())

    return resource
  }
}
