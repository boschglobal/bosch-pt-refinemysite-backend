/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.participant.model

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.PROJECT_STATE
import com.bosch.pt.csm.cloud.projectmanagement.project.common.model.ShardedByProjectIdentifier
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(PROJECT_STATE)
@TypeAlias("Participant")
data class Participant(
    @Id val identifier: UUID,
    override val projectIdentifier: UUID,
    val role: ParticipantRoleEnum,
    @Deprecated("remove with participant g1")
    val employeeIdentifier: UUID? = null,
    val companyIdentifier: UUID,
    val userIdentifier: UUID,
    val active: Boolean = true
) : ShardedByProjectIdentifier

enum class ParticipantRoleEnum {
    CR,
    CSM,
    FM
}
