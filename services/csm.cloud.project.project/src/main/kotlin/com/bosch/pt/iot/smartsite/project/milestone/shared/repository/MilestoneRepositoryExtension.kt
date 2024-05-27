/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.shared.repository

import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.dto.MilestoneFilterDto
import org.springframework.data.domain.Pageable

interface MilestoneRepositoryExtension {

  fun findMilestoneIdentifiersForFilters(
      filters: MilestoneFilterDto,
      pageable: Pageable
  ): List<MilestoneId>

  fun countAllForFilters(filters: MilestoneFilterDto): Long
}
