/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.company.facade.rest.resource.request

import com.bosch.pt.csm.common.facade.rest.ETag
import com.bosch.pt.csm.company.company.CompanyId
import com.bosch.pt.csm.company.company.command.api.CreateCompanyCommand
import com.bosch.pt.csm.company.company.command.api.UpdateCompanyCommand
import com.bosch.pt.csm.company.company.facade.rest.resource.dto.PostBoxAddressDto
import com.bosch.pt.csm.company.company.facade.rest.resource.dto.StreetAddressDto
import com.bosch.pt.csm.company.company.shared.model.Company
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class SaveCompanyResource(
    @field:NotBlank @field:Size(min = 1, max = Company.MAX_NAME_LENGTH) var name: String,
    @field:Valid var streetAddress: StreetAddressDto? = null,
    @field:Valid var postBoxAddress: PostBoxAddressDto? = null
) {
  fun toCommand(identifier: CompanyId?) =
      CreateCompanyCommand(
          identifier = identifier,
          name = this.name,
          streetAddress = this.streetAddress?.toStreetAddressVo(),
          postBoxAddress = this.postBoxAddress?.toPostBoxAdressVo())

  fun toCommand(identifier: CompanyId, eTag: ETag) =
      UpdateCompanyCommand(
          identifier = identifier,
          version = eTag.toVersion(),
          name = name,
          streetAddress = this.streetAddress?.toStreetAddressVo(),
          postBoxAddress = this.postBoxAddress?.toPostBoxAdressVo())
}
