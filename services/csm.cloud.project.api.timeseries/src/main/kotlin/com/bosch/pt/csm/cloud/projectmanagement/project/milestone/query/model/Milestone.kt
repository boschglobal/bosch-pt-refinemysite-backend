/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.craft.domain.ProjectCraftId
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.domain.MilestoneId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.domain.WorkAreaId
import com.bosch.pt.csm.cloud.projectmanagement.translation.shared.model.TranslatableEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

const val MILESTONE_PROJECTION = "MilestoneProjection"

@Document(MILESTONE_PROJECTION)
@TypeAlias(MILESTONE_PROJECTION)
data class Milestone(
    @Id val identifier: MilestoneId,
    val version: Long,
    val project: ProjectId,
    val name: String,
    val type: MilestoneTypeEnum,
    val date: LocalDate,
    val header: Boolean,
    val description: String?,
    val craft: ProjectCraftId? = null,
    val workArea: WorkAreaId? = null,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
    val history: List<MilestoneVersion>
)

data class MilestoneVersion(
    val version: Long,
    val name: String,
    val type: MilestoneTypeEnum,
    val date: LocalDate,
    val header: Boolean,
    val description: String?,
    val craft: ProjectCraftId? = null,
    val workArea: WorkAreaId? = null,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
)

enum class MilestoneTypeEnum(private val type: String) : TranslatableEnum {
  CRAFT("CRAFT"),
  INVESTOR("INVESTOR"),
  PROJECT("PROJECT");

  companion object {
    const val KEY_PREFIX: String = "MILESTONE_TYPE_"
  }

  val shortKey: String
    get() = this.type

  override val key: String
    get() = "${KEY_PREFIX}${this.type}"

  override val messageKey: String
    get() = "${MilestoneTypeEnum::class.simpleName}_$this"
}
