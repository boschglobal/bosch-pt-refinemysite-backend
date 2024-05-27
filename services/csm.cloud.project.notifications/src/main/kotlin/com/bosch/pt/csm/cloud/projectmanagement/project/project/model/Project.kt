/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.model

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.PROJECT_STATE
import com.bosch.pt.csm.cloud.projectmanagement.project.common.model.ShardedByProjectIdentifier
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate
import java.util.UUID

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
    val projectAddress: ProjectAddress? = null
) : ShardedByProjectIdentifier

data class ProjectAddress(
    val city: String? = null,
    val houseNumber: String? = null,
    val street: String? = null,
    val zipCode: String? = null
)

enum class ProjectCategoryEnum {
    /** New Building.  */
    NB,

    /** Old Building.  */
    OB,

    /** Reconstruction Building.  */
    RB
}
