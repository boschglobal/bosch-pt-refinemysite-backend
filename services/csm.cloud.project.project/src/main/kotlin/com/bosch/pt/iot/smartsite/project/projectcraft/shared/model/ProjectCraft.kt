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
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "project_craft",
    indexes = [Index(name = "UK_ProjCraft_Identifier", columnList = "identifier", unique = true)],
    uniqueConstraints =
        [
            UniqueConstraint(
                name = "UK_ProjCraftName_ProjCraftProj", columnNames = ["name", "project_id"])])
class ProjectCraft(

    // Project
    @JoinColumn(foreignKey = ForeignKey(name = "FK_ProjCraft_Project"), nullable = false)
    @ManyToOne(fetch = LAZY)
    var project: Project,

    // Craft name
    @Column(nullable = false, length = MAX_NAME_LENGTH) var name: String,

    // Craft color
    @Column(nullable = false, length = MAX_COLOR_LENGTH) var color: String,

    // Project craft List back reference - just for easier queries
    @Suppress("Unused", "UnusedPrivateMember")
    @JoinColumn(
        foreignKey = ForeignKey(name = "FK_ProjectCraft_ProjectCraftList"),
        insertable = false,
        updatable = false)
    @ManyToOne(fetch = LAZY)
    private val projectCraftList: ProjectCraftList? = null,

    // Position (attribute belongs to ProjectCraftList)
    @Column(insertable = false, updatable = false) var position: Int? = null
) : AbstractSnapshotEntity<Long, ProjectCraftId>() {

  override fun getDisplayName(): String = name

  companion object {
    private const val serialVersionUID: Long = 2456330536961815689

    const val MAX_COLOR_LENGTH = 32
    const val MAX_NAME_LENGTH = 100
  }
}
