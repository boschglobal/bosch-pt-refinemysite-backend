/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.shared.model

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneListId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderColumn
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(
    name = "milestone_list",
    indexes =
        [
            Index(name = "UK_MilestoneList_Identifier", columnList = "identifier", unique = true),
            // This index is used for accessing the milestone list with the "slot key" which is a
            // unique position in the calender.
            Index(
                name = "UK_MilestoneList_SlotKey",
                columnList = "project_id, date, header, work_area_id",
                unique = true),
            // The null handling of mysql in the index above allows multiple entries for the same
            // combination of project_id, date and header together with a "null" for the
            // work_area_id. To avoid this, we defined an additional index using the derived column
            // work_area_id_constraint. This unique index is only used as a constraint.
            Index(
                name = "UK_MilestoneList_SlotKey",
                columnList = "project_id, date, header, workAreaIdConstraint",
                unique = true),
        ])
class MilestoneList(

    // Project
    @JoinColumn(foreignKey = ForeignKey(name = "FK_MilestoneList_Project"), nullable = false)
    @ManyToOne(fetch = LAZY, optional = false)
    var project: Project,

    // Date
    @Column(nullable = false) var date: LocalDate,

    // Header
    @Column(nullable = false) var header: Boolean,

    // Work Area
    // note: this cannot be a constructor parameter because a setter is defined
    workArea: WorkArea?,

    // List of Milestones
    @OneToMany(fetch = LAZY)
    @JoinColumn(name = "milestone_list_id")
    @OrderColumn(name = "position")
    var milestones: MutableList<Milestone> = mutableListOf(),
) : AbstractSnapshotEntity<Long, MilestoneListId>() {

  // This is a field only used to define a unique key index
  @Column(nullable = false) private var workAreaIdConstraint: Long = -1

  // Work area
  @JoinColumn(foreignKey = ForeignKey(name = "FK_MilestoneList_Workarea"))
  @ManyToOne(fetch = LAZY)
  var workArea: WorkArea? = null
    set(workArea) {
      field = workArea
      this.workAreaIdConstraint = workArea?.id ?: -1
    }

  override fun getDisplayName(): String = identifier.toString()
  override fun toString() = "MilestoneList(identifier='$identifier')"

  companion object {
    @JvmStatic
    fun newInstance() = MilestoneList(Project(), LocalDate.now(), false, null, mutableListOf())
  }
}
