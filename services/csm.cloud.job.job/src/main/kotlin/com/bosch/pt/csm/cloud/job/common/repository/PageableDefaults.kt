/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.common.repository

import org.springframework.data.domain.PageRequest

object PageableDefaults {
  const val DEFAULT_PAGE_SIZE = 20
  val DEFAULT_PAGE_REQUEST: PageRequest = PageRequest.of(0, DEFAULT_PAGE_SIZE)
}
