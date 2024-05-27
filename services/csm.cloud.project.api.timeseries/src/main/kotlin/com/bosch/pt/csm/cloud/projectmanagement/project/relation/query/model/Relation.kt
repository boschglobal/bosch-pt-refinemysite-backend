/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.domain.RelationId
import com.bosch.pt.csm.cloud.projectmanagement.translation.shared.model.TranslatableEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

const val RELATION_PROJECTION = "RelationProjection"

@Document(RELATION_PROJECTION)
@TypeAlias(RELATION_PROJECTION)
data class Relation(
    @Id val identifier: RelationId,
    val version: Long,
    val project: ProjectId,
    val critical: Boolean,
    val type: RelationTypeEnum,
    val source: RelationReference,
    val target: RelationReference,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
    val history: List<RelationVersion>
)

data class RelationVersion(
    val version: Long,
    val critical: Boolean,
    val type: RelationTypeEnum,
    val source: RelationReference,
    val target: RelationReference,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
)

enum class RelationTypeEnum(private val type: String) : TranslatableEnum {
  FINISH_TO_START("FINISH_TO_START"),
  PART_OF("PART_OF");

  companion object {
    const val KEY_PREFIX: String = "RELATION_TYPE_"
  }

  override val key: String
    get() = "${KEY_PREFIX}${this.type}"

  override val messageKey: String
    get() = "${RelationTypeEnum::class.simpleName}_$this"
}

data class RelationReference(@JsonProperty("id") val identifier: UUID, val type: String)
