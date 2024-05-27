/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.boundary.dto

import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElement
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum
import java.util.UUID

data class RelationDto(
    val type: RelationTypeEnum,
    val source: RelationElementDto,
    val target: RelationElementDto
) {

  fun toRelation(project: Project) =
      Relation(
          type = type,
          source = source.toRelationElement(),
          target = target.toRelationElement(),
          project = project,
          // this DTO is used to create relations; at creation time, criticality is not yet
          // calculated, and therefore undefined
          critical = null)

  data class RelationElementDto(val id: UUID, val type: RelationElementTypeEnum) {

    fun toRelationElement() = RelationElement(id, type)
  }
}
