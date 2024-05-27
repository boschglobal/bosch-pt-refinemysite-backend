/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.facade.graphql.resource.response.WorkAreaListPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.query.model.WorkAreaList
import java.util.UUID
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface WorkAreaListPayloadMapper {

  companion object {
    val INSTANCE: WorkAreaListPayloadMapper =
        Mappers.getMapper(WorkAreaListPayloadMapper::class.java)
  }

  @Mappings(Mapping(source = "workAreaList.identifier.value", target = "id"))
  fun fromWorkAreaList(workAreaList: WorkAreaList, items: List<UUID>): WorkAreaListPayloadV1
}
