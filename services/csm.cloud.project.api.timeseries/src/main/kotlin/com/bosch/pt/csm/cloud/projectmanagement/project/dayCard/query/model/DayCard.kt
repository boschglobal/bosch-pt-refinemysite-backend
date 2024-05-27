/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.domain.DayCardId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.translation.shared.model.TranslatableEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

const val DAY_CARD_PROJECTION = "DayCardProjection"

@Document(DAY_CARD_PROJECTION)
@TypeAlias(DAY_CARD_PROJECTION)
data class DayCard(
    @Id val identifier: DayCardId,
    val version: Long,
    val project: ProjectId,
    val task: TaskId,
    val status: DayCardStatusEnum,
    val title: String,
    val manpower: BigDecimal,
    val notes: String? = null,
    val reason: DayCardReasonEnum? = null,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
    val history: List<DayCardVersion>
)

data class DayCardVersion(
    val version: Long,
    val status: DayCardStatusEnum,
    val title: String,
    val manpower: BigDecimal,
    val notes: String? = null,
    val reason: DayCardReasonEnum? = null,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
)

enum class DayCardStatusEnum(private val status: String) : TranslatableEnum {
  OPEN("OPEN"),
  NOTDONE("NOT_DONE"),
  DONE("DONE"),
  APPROVED("APPROVED");

  companion object {
    const val KEY_PREFIX: String = "DAY_CARD_STATUS_"
  }

  val shortKey: String
    get() = this.status

  override val key: String
    get() = "${KEY_PREFIX}${this.status}"

  override val messageKey: String
    get() = "${DayCardStatusEnum::class.simpleName}_$this"
}

enum class DayCardReasonEnum(private val reason: String, val id: UUID) {
  BAD_WEATHER("WEATHER", "0189709c-90cd-4ea8-925c-596b57247355".toUUID()),
  CHANGED_PRIORITY("CLIENT_DESIGN_CHANGE", "da1110fd-ec48-4efa-9e69-bbc2d622acdc".toUUID()),
  CONCESSION_NOT_RECOGNIZED(
      "PRELIMINARY_WORK_NOT_RECOGNIZED", "22b2e1b0-c277-4332-88a4-999a5098ad68".toUUID()),
  CUSTOM1("CUSTOM_REASON1", "8d277af1-688e-486d-aa2d-b99a9f5d939d".toUUID()),
  CUSTOM2("CUSTOM_REASON2", "6b51d46d-5544-4b53-9c7e-5b4ef5eae887".toUUID()),
  CUSTOM3("CUSTOM_REASON3", "c717f589-a652-4d0f-81cb-66d29e288dd7".toUUID()),
  CUSTOM4("CUSTOM_REASON4", "e896f1a9-c40a-41d0-8dfe-19cb5298e415".toUUID()),
  DELAYED_MATERIAL("DELAYED_DEFECTIVE_MATERIAL", "86926418-40b3-48ef-b212-0a8adef02d33".toUUID()),
  MANPOWER_SHORTAGE("WORKER_SHORTAGE", "fc8b8ff0-7571-4920-8e4e-9823b04ebfd1".toUUID()),
  MISSING_INFOS("MISSING_INFORMATION", "f1abaeca-99d9-43ac-b906-163a21a05af9".toUUID()),
  MISSING_TOOLS("MISSING_TOOLS", "cb7ee74b-fd79-4fbc-8d32-ffa10dbf45cf".toUUID()),
  NO_CONCESSION("PRELIMINARY_WORK_NOT_DONE", "459f27c4-3cea-44ad-a8d7-24995799f1e9".toUUID()),
  OVERESTIMATION("OVERESTIMATION_OWN_PERFORMANCE", "86617d32-c1a2-41c4-85e9-b365320b30c3".toUUID()),
  TOUCHUP("REWORK_REQUIRED", "b63031f2-fd73-4aab-90c5-94653b0e323a".toUUID());

  companion object {
    const val KEY_PREFIX: String = "DAY_CARD_REASON_"
  }

  val isCustom: Boolean
    get() = this == CUSTOM1 || this == CUSTOM2 || this == CUSTOM3 || this == CUSTOM4

  val shortKey: String
    get() = this.reason

  val key: String
    get() = "${KEY_PREFIX}${this.reason}"

  val messageKey: String
    get() = "${DayCardReasonEnum::class.simpleName}_$this"

  val timestamp: Long
    get() = 0L
}
