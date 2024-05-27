/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.request.FilterMilestoneListResource
import org.junit.jupiter.api.BeforeEach
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

abstract class AbstractMilestoneIntegrationTest : AbstractIntegrationTestV2() {

  protected lateinit var defaultFilter: FilterMilestoneListResource

  @BeforeEach
  fun defineFilter() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(getIdentifier("userCsm2"))

    defaultFilter = FilterMilestoneListResource()
  }

  companion object {
    val DEFAULT_SORTING: PageRequest =
        PageRequest.of(0, 10, Sort.by("date", "header", "workArea", "position"))
  }
}
