/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.repository

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.iot.smartsite.application.SmartSiteMockKTest
import io.mockk.every
import io.mockk.impl.annotations.SpyK
import io.mockk.verify
import java.util.UUID
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@SmartSiteMockKTest
internal class AbstractCacheTest {

  @SpyK private var cut: AbstractCache<UUID, String> = TestCache()

  @Test
  fun cacheReturnsCorrectDtos() {
    val identifiers = setOf(randomUUID(), randomUUID())
    val dtos = cut[identifiers]
    val expected = identifiers.map { it.toString() }.toSet()

    assertThat(dtos).containsAll(expected)
  }

  @Test
  fun dtosAreServedFromCacheAfterFirstLoading() {
    val identifiers = setOf(randomUUID(), randomUUID())

    // first call => cache miss, causing one call to loadDtos method
    cut[identifiers]

    // second call => should be served from cache, causing no call to loadDtos method
    cut[identifiers]

    verify(exactly = 1) { cut.loadFromDatabase(any()) }
  }

  @Test
  fun loadOnlyUncachedDtos() {
    val identifiers = setOf(randomUUID(), randomUUID())

    // fill cache with two initial elements
    cut[identifiers]

    // add one more element to cause a cache miss on the next call to loadDtosOrGetFromCache
    val oneMoreIdentifier: MutableSet<UUID> = HashSet(identifiers)
    oneMoreIdentifier.add(randomUUID())

    every { cut.loadFromDatabase(any()) } answers
        {
          val arguments = this.invocation.args[0] as Set<*>
          assertThat(arguments).hasSize(1)
          this.callOriginal()
        }

    cut[oneMoreIdentifier]
  }

  private class TestCache : AbstractCache<UUID, String>() {

    override fun loadFromDatabase(keys: Set<UUID>): Set<String> = keys.map { it.toString() }.toSet()

    override fun getCacheKey(value: String): UUID = value.toUUID()
  }
}
