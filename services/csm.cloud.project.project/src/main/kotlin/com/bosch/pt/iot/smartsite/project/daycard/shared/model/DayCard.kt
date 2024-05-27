/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.daycard.shared.model

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.TaskSchedule
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import org.apache.commons.lang3.StringUtils.isBlank

@Entity
@Table(indexes = [Index(name = "UK_DayCard_Identifier", columnList = "identifier", unique = true)])
class DayCard : AbstractSnapshotEntity<Long, DayCardId> {

  @JoinColumn(foreignKey = ForeignKey(name = "FK_DayCard_TaskSchedule"))
  @ManyToOne(fetch = LAZY, optional = false)
  lateinit var taskSchedule: TaskSchedule

  @field:Size(max = MAX_TITLE_LENGTH)
  @Column(length = MAX_TITLE_LENGTH, nullable = false)
  lateinit var title: String

  @Column(scale = 2, nullable = false) var manpower: BigDecimal = BigDecimal.ONE

  @field:Size(min = 1, max = MAX_NOTES_LENGTH)
  @Column(length = MAX_NOTES_LENGTH)
  var notes: String? = null
    set(value) {
      field = if (isBlank(value)) null else value
    }

  @Enumerated(STRING)
  @Column(nullable = false, length = MAX_STATUS_LENGTH, columnDefinition = "varchar(10)")
  lateinit var status: DayCardStatusEnum

  @Enumerated(STRING)
  @Column(length = MAX_REASON_LENGTH, columnDefinition = "varchar(30)")
  var reason: DayCardReasonEnum? = null

  /** Constructor for JPA. */
  constructor() {
    // empty
  }

  constructor(
      identifier: DayCardId?,
      taskSchedule: TaskSchedule,
      title: String,
      manpower: BigDecimal,
      notes: String?
  ) {
    this.identifier = identifier ?: DayCardId()
    this.taskSchedule = taskSchedule
    this.title = title
    this.manpower = manpower
    this.notes = if (isBlank(notes)) null else notes
    status = OPEN
    reason = null
  }

  constructor(
      identifier: DayCardId?,
      taskSchedule: TaskSchedule,
      title: String,
      manpower: BigDecimal,
      notes: String?,
      status: DayCardStatusEnum,
      reason: DayCardReasonEnum?
  ) {
    this.identifier = identifier ?: DayCardId()
    this.taskSchedule = taskSchedule
    this.title = title
    this.manpower = manpower
    this.notes = notes
    this.status = status
    this.reason = reason
  }

  override fun getDisplayName(): String = title

  companion object {
    private const val serialVersionUID: Long = 6823199688007142719

    const val MAX_TITLE_LENGTH = 100
    const val MAX_NOTES_LENGTH = 500
    const val MIN_MANPOWER = "00.00"
    const val MAX_MANPOWER = "99.99"
    private const val MAX_STATUS_LENGTH = 10
    private const val MAX_REASON_LENGTH = 30
  }
}
