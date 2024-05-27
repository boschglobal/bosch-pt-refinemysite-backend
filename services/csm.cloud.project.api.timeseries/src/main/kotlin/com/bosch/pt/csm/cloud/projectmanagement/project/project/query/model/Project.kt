/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.translation.shared.model.TranslatableEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

const val PROJECT_PROJECTION = "ProjectProjection"

@Document(PROJECT_PROJECTION)
@TypeAlias(PROJECT_PROJECTION)
data class Project(
    @Id val identifier: ProjectId,
    val version: Long,
    val title: String,
    val start: LocalDate,
    val end: LocalDate,
    val projectNumber: String,
    val client: String? = null,
    val description: String? = null,
    val category: ProjectCategoryEnum? = null,
    val projectAddress: ProjectAddress,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
    val history: List<ProjectVersion>
)

data class ProjectVersion(
    val version: Long,
    val title: String,
    val start: LocalDate,
    val end: LocalDate,
    val projectNumber: String,
    val client: String? = null,
    val description: String? = null,
    val category: ProjectCategoryEnum? = null,
    val projectAddress: ProjectAddress,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime
)

data class ProjectAddress(
    val city: String,
    val houseNumber: String,
    val street: String,
    val zipCode: String
)

enum class ProjectCategoryEnum(private val category: String) : TranslatableEnum {
  NB("NEW_BUILDING"),
  OB("RENOVATION"),
  RB("RECONSTRUCTION");

  companion object {
    const val KEY_PREFIX: String = "PROJECT_CATEGORY_"
  }

  val shortKey: String
    get() = this.category

  override val key: String
    get() = "${KEY_PREFIX}${this.category}"

  override val messageKey: String
    get() = "${ProjectCategoryEnum::class.simpleName}_$this"
}
