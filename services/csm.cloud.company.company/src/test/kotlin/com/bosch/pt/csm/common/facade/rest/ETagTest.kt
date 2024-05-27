/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.facade.rest

import com.bosch.pt.csm.company.employee.shared.model.Employee
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.lang3.StringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Unit test to verify correct validations of [ETag]. */
class ETagTest {

  /** Verify that expected ETag value is created for given string value. */
  @Test
  fun verifyETagFromString() {
    val cut = ETag.from("1")
    assertThat(cut).isNotNull
    assertThat(cut.toString()).isNotNull.isEqualTo(StringUtils.wrap("1", '"'))
  }

  /** Verify that expected ETag value is created for given entity value. */
  @Test
  fun verifyETagFromEntity() {
    val entity = mockk<Employee>()
    every { entity.version } returns 1L

    val cut = ETag.from(entity.version)
    assertThat(cut).isNotNull
    assertThat(cut.toString()).isNotNull.isEqualTo(StringUtils.wrap("1", '"'))
  }
}
