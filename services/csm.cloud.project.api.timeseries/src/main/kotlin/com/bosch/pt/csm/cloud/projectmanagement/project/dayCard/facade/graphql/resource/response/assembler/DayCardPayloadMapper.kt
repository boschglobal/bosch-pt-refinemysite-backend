/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.graphql.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.graphql.resource.response.DayCardPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.facade.graphql.resource.response.DayCardReasonPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCard
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers

@Mapper
interface DayCardPayloadMapper {

  companion object {
    val INSTANCE: DayCardPayloadMapper = Mappers.getMapper(DayCardPayloadMapper::class.java)
  }

  @Mappings(
      Mapping(source = "dayCard.identifier.value", target = "id"),
      Mapping(source = "dayCard.task", target = "taskId"),
      // Map parameters explicitly to instruct mapstruct to take
      // the parameter value instead of the attribute
      Mapping(expression = "java(dayCard.getStatus().getShortKey())", target = "status"),
      Mapping(source = "reason", target = "reason"))
  fun fromDayCard(dayCard: DayCard, reason: DayCardReasonPayloadV1?): DayCardPayloadV1
}
