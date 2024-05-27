/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.common.repository

import com.bosch.pt.csm.cloud.common.LibraryCandidate
import org.springframework.data.domain.PageRequest

@LibraryCandidate
object PageableDefaults {
  const val DEFAULT_PAGE_SIZE = 20
  @JvmField val DEFAULT_PAGE_REQUEST: PageRequest = PageRequest.of(0, DEFAULT_PAGE_SIZE)
}
