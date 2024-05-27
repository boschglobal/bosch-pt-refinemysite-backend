/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.daycard.repository

import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.model.DayCard
import java.util.UUID

interface DayCardRepositoryExtension {

  fun find(identifier: UUID, version: Long, projectIdentifier: UUID): DayCard

  fun findDayCards(projectIdentifier: UUID): List<DayCard>

  fun deleteDayCard(identifier: UUID, projectIdentifier: UUID)
}
