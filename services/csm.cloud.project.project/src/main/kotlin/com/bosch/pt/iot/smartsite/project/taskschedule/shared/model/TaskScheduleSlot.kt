/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskschedule.shared.model

import com.bosch.pt.csm.cloud.common.api.LocalEntity
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.ForeignKey
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "taskschedule_taskscheduleslot",
    uniqueConstraints =
        [UniqueConstraint(name = "UK_TaskScheduleSlot_DayCard", columnNames = ["day_card_id"])])
class TaskScheduleSlot : LocalEntity<Long> {

  @Column(name = "day_card_date", nullable = false) lateinit var date: LocalDate

  @MapsId
  @OneToOne(fetch = LAZY, optional = false)
  @JoinColumn(foreignKey = ForeignKey(name = "FK_TaskScheduleSlot_DayCard_DayCardId"))
  lateinit var dayCard: DayCard

  constructor() {
    // Just for JPA
  }

  constructor(date: LocalDate, dayCard: DayCard) {
    this.date = date
    this.dayCard = dayCard
  }

  companion object {
    private const val serialVersionUID: Long = 8906191395812807153
  }
}
