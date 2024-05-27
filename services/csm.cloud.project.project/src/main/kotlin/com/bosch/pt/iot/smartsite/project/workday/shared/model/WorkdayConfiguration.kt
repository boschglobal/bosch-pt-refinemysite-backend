/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.shared.model

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.workday.domain.WorkdayConfigurationId
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.FetchType.EAGER
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.DayOfWeek
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode.SUBSELECT

@Entity
@Table(
    indexes =
        [
            Index(
                name = "UK_WorkdayConfiguration_Identifier",
                columnList = "identifier",
                unique = true)])
class WorkdayConfiguration(

    // startOfWeek
    @Enumerated(STRING)
    @Column(nullable = false, length = MAX_DAY_OF_WEEK_LENGTH, columnDefinition = "varchar(12)")
    var startOfWeek: DayOfWeek,

    // workingDays
    @Enumerated(STRING)
    @ElementCollection(fetch = EAGER)
    @Fetch(value = SUBSELECT) // avoids cartesian product as suggested in HHH-1718
    @Column(nullable = false, length = MAX_DAY_OF_WEEK_LENGTH, columnDefinition = "varchar(12)")
    @CollectionTable(
        name = "workday_configuration_working_days",
        joinColumns =
            [
                JoinColumn(
                    foreignKey = ForeignKey(name = "FK_WorkdayConfiguration_WorkingDays"),
                    nullable = false)])
    var workingDays: MutableSet<DayOfWeek>,

    // holidays
    @ElementCollection(fetch = EAGER)
    @Fetch(value = SUBSELECT) // avoids cartesian product as suggested in HHH-1718
    @CollectionTable(
        name = "workday_configuration_holidays",
        joinColumns =
            [
                JoinColumn(
                    foreignKey = ForeignKey(name = "FK_WorkdayConfiguration_Holidays"),
                    nullable = false)])
    var holidays: MutableSet<Holiday>,

    // allowWorkOnNonWorkingDays
    @Column(nullable = false) var allowWorkOnNonWorkingDays: Boolean,

    // project
    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = ForeignKey(name = "FK_WorkdayConfiguration_Project"), nullable = false)
    var project: Project
) : AbstractSnapshotEntity<Long, WorkdayConfigurationId>() {

  override fun getDisplayName(): String = "Project ${project.identifier} workday configuration"

  companion object {
    private const val serialVersionUID: Long = -747433308267582199

    const val MAX_DAY_OF_WEEK_LENGTH = 12
    const val MAX_WORKDAYS_NUMBER = 7
  }
}
