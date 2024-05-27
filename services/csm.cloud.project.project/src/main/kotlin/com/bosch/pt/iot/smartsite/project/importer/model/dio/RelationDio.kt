/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.model.dio

import com.bosch.pt.iot.smartsite.project.external.model.ObjectType
import com.bosch.pt.iot.smartsite.project.importer.boundary.ImportContext
import com.bosch.pt.iot.smartsite.project.importer.model.DataImportObject
import com.bosch.pt.iot.smartsite.project.importer.model.DataImportObjectIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.MilestoneIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.RelationIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.TaskIdentifier
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResult
import com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto
import com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto.RelationElementDto
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.MILESTONE
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.TASK
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum
import java.util.UUID
import java.util.UUID.randomUUID

data class RelationDio(
    override val id: RelationIdentifier,
    override val guid: UUID?,
    override val uniqueId: Int,
    override val fileId: Int,
    val sourceId: DataImportObjectIdentifier,
    val targetId: DataImportObjectIdentifier
) : DataImportObject<RelationDto> {

  // This identifier is useless
  override val identifier: UUID = randomUUID()

  override val wbs: String? = null

  override val externalIdentifier: UUID
    get() = throw UnsupportedOperationException("Not implemented")

  override val objectType: ObjectType
    get() = throw UnsupportedOperationException("Not implemented")

  override val activityId: String
    get() = throw UnsupportedOperationException("Not implemented")

  override fun toTargetType(context: ImportContext): RelationDto {
    val isSourceTask = sourceId is TaskIdentifier
    val isSourceMilestone = sourceId is MilestoneIdentifier

    val source =
        when {
          isSourceTask -> RelationElementDto(requireNotNull(context[sourceId]), TASK)
          isSourceMilestone -> RelationElementDto(requireNotNull(context[sourceId]), MILESTONE)
          else -> throw IllegalArgumentException("Relation source of unexpected type")
        }

    val isTargetTask = targetId is TaskIdentifier
    val isTargetMilestone = targetId is MilestoneIdentifier

    val target =
        when {
          isTargetTask -> RelationElementDto(requireNotNull(context[targetId]), TASK)
          isTargetMilestone -> RelationElementDto(requireNotNull(context[targetId]), MILESTONE)
          else -> throw IllegalArgumentException("Relation target of unexpected type")
        }

    return RelationDto(RelationTypeEnum.FINISH_TO_START, source, target)
  }

  override fun validate(context: ImportContext): List<ValidationResult> = emptyList()
}
