/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.model

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.PROJECT_STATE
import com.bosch.pt.csm.cloud.projectmanagement.project.common.model.ShardedByProjectIdentifier
import java.math.BigDecimal
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

@Document(PROJECT_STATE)
@TypeAlias("DayCard")
data class DayCard(
    @Id val identifier: AggregateIdentifier,
    override val projectIdentifier: UUID,
    val taskIdentifier: UUID,
    val status: DayCardStatusEnum,
    val title: String,
    val manpower: BigDecimal?,
    val notes: String?,
    val reason: DayCardReasonEnum?
) : ShardedByProjectIdentifier

enum class DayCardReasonEnum {
  DELAYED_MATERIAL,
  NO_CONCESSION,
  CONCESSION_NOT_RECOGNIZED,
  CHANGED_PRIORITY,
  MANPOWER_SHORTAGE,
  OVERESTIMATION,
  TOUCHUP,
  MISSING_INFOS,
  MISSING_TOOLS,
  BAD_WEATHER,
  CUSTOM1,
  CUSTOM2,
  CUSTOM3,
  CUSTOM4;

  @ExcludeFromCodeCoverage
  fun isCustom(): Boolean = this == CUSTOM1 || this == CUSTOM2 || this == CUSTOM3 || this == CUSTOM4
}

enum class DayCardStatusEnum {
  OPEN,
  NOTDONE,
  DONE,
  APPROVED
}
