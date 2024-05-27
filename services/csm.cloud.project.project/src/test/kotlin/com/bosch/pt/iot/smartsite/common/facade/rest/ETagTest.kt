/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.facade.rest

import com.bosch.pt.csm.cloud.common.command.exceptions.EntityOutdatedException
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag.Companion.from
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.from
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.getVersionInformation
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.toEtag
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.verify
import com.bosch.pt.iot.smartsite.common.model.AbstractEntity
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.lang3.StringUtils.wrap
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/** Unit test to verify correct validations of [ETag]. */
internal class ETagTest {

  /** Verify that expected ETag value is created for given string value. */
  @Test
  fun verifyETagFromString() {
    val cut = from("1")

    assertThat(cut).isNotNull
    assertThat(cut.toString()).isNotNull.isEqualTo(wrap("1", '"'))
  }

  /** Verify that expected ETag value is created for given long value. */
  @Test
  fun verifyETagFromLong() {
    val cut = ETag.from(1L)

    assertThat(cut).isNotNull
    assertThat(cut.toVersion()).isNotNull.isEqualTo(1L)
  }

  /** Verify that expected ETag value is created for given entity value. */
  @Test
  fun verifyETagFromEntity() {
    val entity = mockk<AbstractEntity<*, *>>()
    every { entity.version } returns 1L

    val cut = entity.toEtag()

    assertThat(cut).isNotNull
    assertThat(cut.toString()).isNotNull.isEqualTo(wrap("1", '"'))
  }

  /** Verify that ETag value creation reports error for entity without version value. */
  @Test
  fun verifyETagFromEntityWithoutVersionReportsError() {
    val entity = mockk<AbstractEntity<*, *>>()
    every { entity.version } returns null

    assertThatThrownBy { entity.toEtag() }.isInstanceOf(IllegalArgumentException::class.java)
  }

  /** Verify that expected version information is returned from [ETag]. */
  @Test
  fun getVersionInformation() {
    val entity = mockk<AbstractEntity<*, *>>()
    every { entity.version } returns 1L

    val versionInformation = ETag.getVersionInformation(entity)

    assertThat(versionInformation).isNotNull.isEqualTo("1")
  }

  /** Verify that entity is reported as outdated. */
  @Test
  fun verifyEntityIsOutDatedForETag() {
    val entity = mockk<AbstractEntity<*, *>>(relaxed = true)
    every { entity.version } returns 2L

    val eTag1 = from("1")

    assertThatThrownBy { eTag1.verify(entity) }.isInstanceOf(EntityOutdatedException::class.java)
  }

  /** Verify that entity is not reported as outdated. */
  @Test
  fun verifyEntityIsNotOutDatedForETag() {
    val entity = mockk<AbstractEntity<*, *>>()
    every { entity.version } returns 1L

    val eTag1 = from("1")

    eTag1.verify(entity)
  }
}
