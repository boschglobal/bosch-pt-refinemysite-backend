/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.milestone.shared.model

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Entity
@Table(
    indexes = [Index(name = "UK_Milestone_Identifier", columnList = "identifier", unique = true)])
class Milestone(

    // Name
    @field:Size(min = 1, max = MAX_NAME_LENGTH)
    @Column(nullable = false, length = MAX_NAME_LENGTH)
    var name: String,

    // Type
    @Column(nullable = false) var type: MilestoneTypeEnum,

    // Date
    @Column(nullable = false) var date: LocalDate,

    // Header
    @Column(nullable = false) var header: Boolean,

    // Project
    @JoinColumn(foreignKey = ForeignKey(name = "FK_Milestone_Project"))
    @ManyToOne(fetch = LAZY, optional = false)
    var project: Project,

    // Craft
    @JoinColumn(foreignKey = ForeignKey(name = "FK_Milestone_Craft"))
    @ManyToOne(fetch = LAZY)
    var craft: ProjectCraft?,

    // Work area
    @JoinColumn(foreignKey = ForeignKey(name = "FK_Milestone_Workarea"))
    @ManyToOne(fetch = LAZY)
    var workArea: WorkArea?,

    // Description
    @field:Size(max = MAX_DESCRIPTION_LENGTH)
    @Column(length = MAX_DESCRIPTION_LENGTH)
    var description: String?,

    // Milestone List back reference - just for easier queries
    @Suppress("UnusedPrivateMember")
    @JoinColumn(
        foreignKey = ForeignKey(name = "FK_Milestone_MilestoneList"),
        insertable = false,
        updatable = false)
    @ManyToOne(fetch = LAZY)
    private val milestoneList: MilestoneList? = null,

    // Position (attribute belongs to MilestoneList)
    @Column(insertable = false, updatable = false) val position: Int? = null
) : AbstractSnapshotEntity<Long, MilestoneId>() {

  override fun getDisplayName(): String = name

  companion object {
    const val MAX_NAME_LENGTH = 100
    const val MAX_DESCRIPTION_LENGTH = 1000
    @JvmStatic
    fun newInstance() =
        Milestone("", MilestoneTypeEnum.PROJECT, LocalDate.now(), true, Project(), null, null, null)
  }
}
