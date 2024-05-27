/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.shared.model

import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.user.model.User
import java.time.LocalDateTime
import java.time.LocalDateTime.now

class WorkAreaBuilder private constructor() {

  private lateinit var project: Project
  private lateinit var name: String
  private var position: Int? = null
  private var identifier: WorkAreaId = WorkAreaId()
  private lateinit var createdBy: User
  private lateinit var lastModifiedBy: User
  private var createdDate = now()
  private var lastModifiedDate = now()

  fun withName(name: String): WorkAreaBuilder = apply { this.name = name }

  fun withPosition(position: Int?): WorkAreaBuilder = apply { this.position = position }

  fun withProject(project: Project): WorkAreaBuilder = apply { this.project = project }

  fun withIdentifier(identifier: WorkAreaId): WorkAreaBuilder = apply {
    this.identifier = identifier
  }

  fun withCreatedBy(createdBy: User): WorkAreaBuilder = apply { this.createdBy = createdBy }

  fun withLastModifiedBy(lastModifiedBy: User): WorkAreaBuilder = apply {
    this.lastModifiedBy = lastModifiedBy
  }

  fun withCreatedDate(createdDate: LocalDateTime): WorkAreaBuilder = apply {
    this.createdDate = createdDate
  }

  fun withLastModifiedDate(lastModifiedDate: LocalDateTime): WorkAreaBuilder = apply {
    this.lastModifiedDate = lastModifiedDate
  }

  fun build(): WorkArea =
      WorkArea(identifier, project, name, position).apply {
        setCreatedBy(this@WorkAreaBuilder.createdBy.getAuditUserId())
        setLastModifiedBy(this@WorkAreaBuilder.lastModifiedBy.getAuditUserId())
        setCreatedDate(this@WorkAreaBuilder.createdDate)
        setLastModifiedDate(this@WorkAreaBuilder.lastModifiedDate)
      }

  companion object {

    @JvmStatic
    fun workArea(): WorkAreaBuilder =
        WorkAreaBuilder().withIdentifier(WorkAreaId()).withName("Elektrizität").withPosition(null)
  }
}
