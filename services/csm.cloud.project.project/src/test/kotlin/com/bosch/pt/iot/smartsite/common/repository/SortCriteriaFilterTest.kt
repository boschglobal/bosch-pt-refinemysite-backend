/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

class SortCriteriaFilterTest {

  private val cut = SortCriteriaFilter

  @Test
  fun `verify returns unpaged if source was unpaged`() {
    val result = cut.filterAndTranslate(Pageable.unpaged(), emptyMap())

    assertThat(result.isUnpaged).isTrue
  }

  @Test
  fun `verify returns order by id if no other sorting criteria was provided`() {
    val result = cut.filterAndTranslate(PageRequest.of(1, 1), emptyMap())

    assertThat(result.sort.get().count()).isEqualTo(1)
    assertThat(result.sort).contains(Sort.Order(Sort.Direction.ASC, "id"))
  }

  @Test
  fun `verify returns correct mapped sort parameters`() {
    val result =
        cut.filterAndTranslate(
            PageRequest.of(1, 1, Sort.Direction.ASC, "date,createdDate"),
            mapOf("name" to "firstName", "date" to "date", "createdDate" to "createdDate"))

    assertThat(result.sort.get().count()).isEqualTo(3)
    assertThat(result.sort).contains(Sort.Order.asc("date"))
    assertThat(result.sort).contains(Sort.Order.asc("createdDate"))
    assertThat(result.sort).contains(Sort.Order.asc("id"))
  }
}
