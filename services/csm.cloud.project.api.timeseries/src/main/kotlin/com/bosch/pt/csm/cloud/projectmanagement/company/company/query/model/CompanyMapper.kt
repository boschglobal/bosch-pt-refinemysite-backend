/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.company.query.model

import com.bosch.pt.csm.cloud.projectmanagement.company.company.domain.CompanyId
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface CompanyMapper {

  companion object {
    val INSTANCE: CompanyMapper = Mappers.getMapper(CompanyMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "identifier"),
      Mapping(source = "history", target = "history"))
  fun fromCompanyVersion(
      companyVersion: CompanyVersion,
      identifier: CompanyId,
      history: List<CompanyVersion>
  ): Company
}
