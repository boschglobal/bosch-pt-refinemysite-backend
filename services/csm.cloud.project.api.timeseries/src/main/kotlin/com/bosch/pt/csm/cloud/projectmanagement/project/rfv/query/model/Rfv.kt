/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.domain.RfvId
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

const val RFV_PROJECTION = "RfvProjection"

/** Customization of a RfV. */
@Document(RFV_PROJECTION)
@TypeAlias(RFV_PROJECTION)
data class Rfv(
    @Id val identifier: RfvId,
    val version: Long,
    val project: ProjectId,
    val reason: DayCardReasonEnum,
    val active: Boolean,
    val name: String?,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
    val history: List<RfvVersion>
)

data class RfvVersion(
    val version: Long,
    val reason: DayCardReasonEnum,
    val active: Boolean,
    val name: String?,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
)
