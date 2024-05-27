/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.company.company.domain.CompanyId
import com.bosch.pt.csm.cloud.projectmanagement.company.company.facade.rest.resource.response.CompanyResource
import com.bosch.pt.csm.cloud.projectmanagement.company.company.query.model.CompanyVersion
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface CompanyResourceMapper {

  companion object {
    val INSTANCE: CompanyResourceMapper = Mappers.getMapper(CompanyResourceMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "id"),
      Mapping(
          expression =
              "java(com.bosch.pt.csm.cloud.common.extensions.JavaApiKt.toEpochMilli(company.getEventDate()))",
          target = "eventTimestamp"))
  fun fromCompanyVersion(company: CompanyVersion, identifier: CompanyId): CompanyResource
}
