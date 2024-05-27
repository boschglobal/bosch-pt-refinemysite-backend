/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2019 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.resource.factory.PageLinks
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.company.model.Company
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.ParticipantCompanyReferenceListResource
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
open class ParticipantCompanyListResourceFactory {

  @PageLinks
  open fun build(
      companies: Page<Company>,
      includeInactive: Boolean
  ): ParticipantCompanyReferenceListResource {
    val companyResources: Collection<ResourceReference> =
        companies.content.map { company -> ResourceReference.from(company) }

    return ParticipantCompanyReferenceListResource(
        companyResources,
        companies.number,
        companies.size,
        companies.totalPages,
        companies.totalElements)
  }
}
