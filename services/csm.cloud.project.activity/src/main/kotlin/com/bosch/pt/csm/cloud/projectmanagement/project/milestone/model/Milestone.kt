/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2021
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.milestone.model

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.PROJECT_STATE
import com.bosch.pt.csm.cloud.projectmanagement.project.common.model.ShardedByProjectIdentifier
import java.time.LocalDate
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

@Document(PROJECT_STATE)
@TypeAlias("Milestone")
class Milestone(
    @Id val identifier: AggregateIdentifier,
    override val projectIdentifier: UUID,
    val name: String,
    val type: MilestoneTypeEnum,
    val date: LocalDate,
    val header: Boolean,
    val projectCraftIdentifier: UUID? = null,
    val workAreaIdentifier: UUID? = null,
    val description: String? = null,
    var position: Int? = null
) : ShardedByProjectIdentifier

enum class MilestoneTypeEnum {
  CRAFT,
  INVESTOR,
  PROJECT
}
