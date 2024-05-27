/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.domain.WorkAreaId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.rest.resource.response.WorkAreaResource
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.model.WorkAreaVersion
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface WorkAreaResourceMapper {

  companion object {
    val INSTANCE: WorkAreaResourceMapper = Mappers.getMapper(WorkAreaResourceMapper::class.java)
  }

  @Mappings(
      Mapping(source = "identifier", target = "id"),
      Mapping(
          expression =
              "java(com.bosch.pt.csm.cloud.common.extensions.JavaApiKt.toEpochMilli(workAreaVersion.getEventDate()))",
          target = "eventTimestamp"))
  fun fromWorkAreaVersion(
      workAreaVersion: WorkAreaVersion,
      project: ProjectId,
      identifier: WorkAreaId
  ): WorkAreaResource
}
