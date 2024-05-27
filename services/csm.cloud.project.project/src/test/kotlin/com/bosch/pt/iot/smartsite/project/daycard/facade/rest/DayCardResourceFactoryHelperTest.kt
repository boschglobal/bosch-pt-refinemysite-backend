/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.daycard.facade.rest

import com.bosch.pt.iot.smartsite.application.SmartSiteSpringBootTest
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.DayCardResource
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.factory.DayCardResourceFactoryHelper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@SmartSiteSpringBootTest
class DayCardResourceFactoryHelperTest {

  @Autowired private lateinit var cut: DayCardResourceFactoryHelper

  @Test
  fun `handles empty lists correctly`() {
    val result = cut.build(emptyList())
    Assertions.assertEquals(listOf<DayCardResource>(), result)
  }

  @Test
  fun `handles empty DTO lists correctly`() {
    val result = cut.buildFromDtos(emptyList())
    Assertions.assertEquals(listOf<DayCardResource>(), result)
  }
}
