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
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.Size
import java.util.UUID

@Entity
@Table(
    name = "work_area",
    indexes = [Index(name = "UK_WorkArea_Identifier", columnList = "identifier", unique = true)],
    uniqueConstraints =
        [
            UniqueConstraint(
                name = "UK_WorkAreaName_Project_Parent",
                columnNames = ["name", "project_id", "parent"])])
class WorkArea : AbstractSnapshotEntity<Long, WorkAreaId> {

  /**
   * Only for the db model generation, to prevent join table but having position based ordering *
   */
  @Suppress("Unused", "UnusedPrivateMember")
  @ManyToOne(fetch = LAZY)
  @JoinColumn(
      foreignKey = ForeignKey(name = "FK_WorkArea_WorkAreaList"),
      insertable = false,
      updatable = false)
  private val workAreaList: WorkAreaList? = null

  @ManyToOne(optional = false)
  @JoinColumn(foreignKey = ForeignKey(name = "FK_WorkArea_Project"))
  lateinit var project: Project

  /** Associated tasks. */
  @OneToMany(fetch = LAZY, mappedBy = "workArea", targetEntity = Task::class)
  var tasks: Set<Task> = HashSet()

  @field:Size(min = MIN_WORKAREA_NAME_LENGTH, max = MAX_WORKAREA_NAME_LENGTH)
  @Column(nullable = false, length = MAX_WORKAREA_NAME_LENGTH)
  lateinit var name: String

  @Column(insertable = false, updatable = false)
  var position: Int? = null

  @AttributeOverrides(
      AttributeOverride(name = "identifier", column = Column(name = "parent", length = 36)))
  var parent: WorkAreaId? = null

  /** For JPA. */
  constructor()

  constructor(
      identifier: WorkAreaId,
      project: Project,
      name: String,
      position: Int? = null,
      parent: WorkAreaId? = null
  ) {
    this.identifier = identifier
    this.project = project
    this.name = name
    this.position = position
    this.parent = parent
  }

  override fun getDisplayName(): String = name

  override fun getIdentifierUuid(): UUID = identifier.toUuid()

  companion object {
    private const val serialVersionUID: Long = 3272137396755808361

    const val MIN_WORKAREA_NAME_LENGTH = 1
    const val MAX_WORKAREA_NAME_LENGTH = 100
    const val MAX_WORKAREA_POSITION_VALUE = 999
  }
}
