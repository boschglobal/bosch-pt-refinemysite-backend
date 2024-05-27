/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.workarea.shared.model

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaListId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.EAGER
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderColumn
import jakarta.persistence.Table

@Entity
@Table(
    name = "work_area_list",
    indexes =
        [
            Index(name = "UK_WorkAreaList_Identifier", columnList = "identifier", unique = true),
            Index(name = "UK_WorkAreaList_Project", columnList = "project_id", unique = true)])
class WorkAreaList : AbstractSnapshotEntity<Long, WorkAreaListId> {

  @OneToMany(fetch = LAZY)
  @JoinColumn(name = "work_area_list_id")
  @OrderColumn(name = "position")
  var workAreas: MutableList<WorkArea> = mutableListOf()

  @OneToOne(fetch = EAGER, optional = false)
  @JoinColumn(foreignKey = ForeignKey(name = "FK_WorkAreaList_Project"), nullable = false)
  lateinit var project: Project

  constructor()

  constructor(
      identifier: WorkAreaListId,
      project: Project,
      workAreas: MutableList<WorkArea> = mutableListOf()
  ) {
    this.identifier = identifier
    this.project = project
    this.workAreas = workAreas
  }

  fun addWorkArea(position: Int, workArea: WorkArea) {
    workAreas.add(position, workArea)
  }

  override fun getDisplayName(): String = identifier.toString()

  companion object {
    private const val serialVersionUID: Long = -7209092012196521145
  }
}
