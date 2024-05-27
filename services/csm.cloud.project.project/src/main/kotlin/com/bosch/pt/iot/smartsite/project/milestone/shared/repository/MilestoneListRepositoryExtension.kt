/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.shared.repository

import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneList
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import java.time.LocalDate

interface MilestoneListRepositoryExtension {

  fun findOneByKey(
      projectIdentifier: ProjectId,
      date: LocalDate,
      header: Boolean,
      workAreaIdentifier: WorkAreaId?
  ): MilestoneList?
}
