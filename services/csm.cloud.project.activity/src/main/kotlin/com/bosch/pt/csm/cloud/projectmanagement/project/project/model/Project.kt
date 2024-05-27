/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.model

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.PROJECT_STATE
import com.bosch.pt.csm.cloud.projectmanagement.project.common.model.ShardedByProjectIdentifier
import java.time.LocalDate
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

@Document(PROJECT_STATE)
@TypeAlias("Project")
data class Project(
    @Id val identifier: AggregateIdentifier,
    override val projectIdentifier: UUID,
    val title: String,
    val start: LocalDate,
    val end: LocalDate,
    val projectNumber: String,
    val client: String? = null,
    val description: String? = null,
    val category: ProjectCategoryEnum? = null,
    val projectAddress: ProjectAddress
) : ShardedByProjectIdentifier

data class ProjectAddress(
    val city: String,
    val houseNumber: String,
    val street: String,
    val zipCode: String
)

enum class ProjectCategoryEnum {
  NB,
  OB,
  RB
}
