/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.graphql.resource.response.WorkAreaPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.model.WorkArea
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface WorkAreaPayloadMapper {

  companion object {
    val INSTANCE: WorkAreaPayloadMapper = Mappers.getMapper(WorkAreaPayloadMapper::class.java)
  }

  @Mappings(Mapping(source = "identifier.value", target = "id"))
  fun fromWorkArea(workArea: WorkArea): WorkAreaPayloadV1
}
