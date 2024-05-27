/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.shared.model

import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaListId
import com.bosch.pt.iot.smartsite.user.model.User
import java.time.LocalDateTime
import java.time.LocalDateTime.now

class WorkAreaListBuilder private constructor() {

  private var workAreas: MutableList<WorkArea> = mutableListOf()
  private lateinit var project: Project
  private var identifier: WorkAreaListId = WorkAreaListId()
  private lateinit var createdBy: User
  private lateinit var lastModifiedBy: User
  private var createdDate = now()
  private var lastModifiedDate = now()
  private var version: Long = 0

  fun withWorkAreas(workAreas: MutableList<WorkArea>): WorkAreaListBuilder = apply {
    this.workAreas = workAreas
  }

  fun withProject(project: Project): WorkAreaListBuilder = apply { this.project = project }

  fun withIdentifier(identifier: WorkAreaListId): WorkAreaListBuilder = apply {
    this.identifier = identifier
  }

  fun withCreatedBy(createdBy: User): WorkAreaListBuilder = apply { this.createdBy = createdBy }

  fun withLastModifiedBy(lastModifiedBy: User): WorkAreaListBuilder = apply {
    this.lastModifiedBy = lastModifiedBy
  }

  fun withCreatedDate(createdDate: LocalDateTime): WorkAreaListBuilder = apply {
    this.createdDate = createdDate
  }

  fun withLastModifiedDate(lastModifiedDate: LocalDateTime): WorkAreaListBuilder = apply {
    this.lastModifiedDate = lastModifiedDate
  }

  fun build(): WorkAreaList =
      WorkAreaList(identifier, project, workAreas).apply {
        setCreatedBy(this@WorkAreaListBuilder.createdBy.getAuditUserId())
        setLastModifiedBy(this@WorkAreaListBuilder.lastModifiedBy.getAuditUserId())
        setCreatedDate(this@WorkAreaListBuilder.createdDate)
        setLastModifiedDate(this@WorkAreaListBuilder.lastModifiedDate)
        version = this@WorkAreaListBuilder.version
      }

  companion object {

    @JvmStatic
    fun workAreaList(): WorkAreaListBuilder =
        WorkAreaListBuilder().withIdentifier(WorkAreaListId()).withWorkAreas(ArrayList())
  }
}
