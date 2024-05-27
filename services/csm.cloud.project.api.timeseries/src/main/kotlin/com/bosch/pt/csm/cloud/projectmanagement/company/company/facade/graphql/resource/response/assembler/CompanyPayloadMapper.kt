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
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface CompanyPayloadMapper {

  companion object {
    val INSTANCE: CompanyPayloadMapper = Mappers.getMapper(CompanyPayloadMapper::class.java)
  }

  @Mappings(Mapping(source = "identifier.value", target = "id"))
  fun fromCompany(company: Company): CompanyPayloadV1
}
