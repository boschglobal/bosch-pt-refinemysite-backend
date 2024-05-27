/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.shared.model

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftListId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderColumn
import jakarta.persistence.Table

@Entity
@Table(
    name = "project_craft_list",
    indexes =
        [
            Index(
                name = "UK_ProjectCraftList_Identifier", columnList = "identifier", unique = true),
            Index(name = "UK_ProjectCraftList_Project", columnList = "project_id", unique = true)])
class ProjectCraftList(

    // Project
    @JoinColumn(foreignKey = ForeignKey(name = "FK_ProjectCraftList_Project"), nullable = false)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    var project: Project,

    // List of project crafts
    @JoinColumn(name = "project_craft_list_id")
    @OneToMany(fetch = FetchType.LAZY)
    @OrderColumn(name = "position")
    var projectCrafts: MutableList<ProjectCraft> = mutableListOf(),
) : AbstractSnapshotEntity<Long, ProjectCraftListId>() {

  override fun getDisplayName(): String = identifier.toString()

  companion object {
    private const val serialVersionUID: Long = 1695494348542125582

    const val MAX_CRAFTS_ALLOWED = 1000
  }
}
