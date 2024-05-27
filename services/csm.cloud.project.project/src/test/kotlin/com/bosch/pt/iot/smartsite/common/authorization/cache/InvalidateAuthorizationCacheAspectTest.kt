/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.authorization.cache

import com.bosch.pt.csm.cloud.common.test.randomString
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.authorization.cache.TestComponent.Companion.EXPECTED_RESULT
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.repository.AbstractCache
import java.util.UUID
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@EnableAllKafkaListeners
internal class InvalidateAuthorizationCacheAspectTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var testCache: TestCache

  @Autowired private lateinit var testComponent: TestComponent

  /** this identifier should be invalidated by the tests */
  private val invalidatedIdentifier = randomUUID()

  /** this identifier should not be invalidated by the tests; it must survive the invalidation */
  private val notInvalidatedIdentifier = randomUUID()

  @BeforeEach
  fun init() {
    // populate cache (this access will cause a cache misses and the UUIDs will be cached)
    testCache.get(setOf(invalidatedIdentifier, notInvalidatedIdentifier))
  }

  @AfterEach
  fun clear() {
    testCache.clear()
  }

  @Test
  fun `calling annotated method with multiple arguments invalidates cache entry`() {
    val result =
        testComponent.multipleParameters(randomString(), invalidatedIdentifier, randomString())

    assertThat(testCache.keys()).containsExactly(notInvalidatedIdentifier)
    assertThat(result).isEqualTo(EXPECTED_RESULT)
  }

  @Test
  fun `calling annotated method with single argument invalidates cache entry`() {
    val result = testComponent.singleParameter(invalidatedIdentifier)

    assertThat(testCache.keys()).containsExactly(notInvalidatedIdentifier)
    assertThat(result).isEqualTo(EXPECTED_RESULT)
  }

  @Test
  fun `calling annotated method with collection argument invalidates all cache entries from collection`() {
    // add one more identifier to the cache
    val additionalIdentifier = randomUUID()
    testCache.get(setOf(additionalIdentifier))

    val result =
        testComponent.collectionParameter(listOf(invalidatedIdentifier, additionalIdentifier))

    assertThat(testCache.keys()).containsExactly(notInvalidatedIdentifier)
    assertThat(result).isEqualTo(EXPECTED_RESULT)
  }

  @Test
  fun `calling annotated method with DTO argument invalidates cache entry`() {
    val result = testComponent.dtoParameter(TestDto(invalidatedIdentifier))

    assertThat(testCache.keys()).containsExactly(notInvalidatedIdentifier)
    assertThat(result).isEqualTo(EXPECTED_RESULT)
  }

  @Test
  fun `calling annotated method with nested DTO argument invalidates cache entry`() {
    val result = testComponent.nestedDtoParameter(NestedTestDto(TestDto(invalidatedIdentifier)))

    assertThat(testCache.keys()).containsExactly(notInvalidatedIdentifier)
    assertThat(result).isEqualTo(EXPECTED_RESULT)
  }

  @Test
  fun `calling annotated method with DTO collection argument invalidates all cache entries from collection`() {
    // add one more identifier to the cache
    val additionalIdentifier = randomUUID()
    testCache.get(setOf(additionalIdentifier))

    val result =
        testComponent.dtoCollectionParameter(
            setOf(TestDto(invalidatedIdentifier), TestDto(additionalIdentifier)))

    assertThat(testCache.keys()).containsExactly(notInvalidatedIdentifier)
    assertThat(result).isEqualTo(EXPECTED_RESULT)
  }

  @Test
  fun `calling annotated method with single null argument does not fail`() {
    val result = testComponent.singleParameter(null)

    assertThat(testCache.keys()).contains(invalidatedIdentifier, notInvalidatedIdentifier)
    assertThat(result).isEqualTo(EXPECTED_RESULT)
  }

  @Test
  fun `calling annotated method without parameter annotation fails`() {
    assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
      testComponent.missingParameterAnnotation(invalidatedIdentifier)
    }
  }

  @Test
  fun `calling annotated method with DTO parameter fails if SpEl expression is missing`() {
    assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
      testComponent.missingSpelExpressionForDtoParameter(TestDto((invalidatedIdentifier)))
    }
  }

  @Test
  fun `calling annotated method with DTO parameter fails if SpEl expression evaluates to non-UUID`() {
    assertThatExceptionOfType(IllegalStateException::class.java).isThrownBy {
      testComponent.notUuidSpelExpressionForDtoParameter(TestDto((invalidatedIdentifier)))
    }
  }
}

@Component
open class TestCache : AbstractCache<UUID, UUID>() {
  override fun loadFromDatabase(keys: Set<UUID>): Set<UUID> {
    return keys
  }

  override fun getCacheKey(value: UUID): UUID {
    return value
  }

  open fun keys() = cache.keys

  open fun clear() = cache.clear()
}

@Component
open class TestComponent {

  @InvalidatesAuthorizationCache
  open fun multipleParameters(foo: String, @AuthorizationCacheKey identifier: UUID, bar: String) =
      EXPECTED_RESULT

  @InvalidatesAuthorizationCache
  open fun singleParameter(@AuthorizationCacheKey identifier: UUID?) = EXPECTED_RESULT

  @InvalidatesAuthorizationCache
  open fun collectionParameter(@AuthorizationCacheKey identifiers: List<UUID>) = EXPECTED_RESULT

  @InvalidatesAuthorizationCache
  open fun dtoParameter(@AuthorizationCacheKey("identifier") dto: TestDto) = EXPECTED_RESULT

  @InvalidatesAuthorizationCache
  open fun nestedDtoParameter(@AuthorizationCacheKey("dto.identifier") nestedDto: NestedTestDto) =
      EXPECTED_RESULT

  @InvalidatesAuthorizationCache
  open fun dtoCollectionParameter(@AuthorizationCacheKey("identifier") dtos: Set<TestDto>) =
      EXPECTED_RESULT

  @InvalidatesAuthorizationCache
  open fun missingParameterAnnotation(identifier: UUID) = EXPECTED_RESULT

  @InvalidatesAuthorizationCache
  open fun missingSpelExpressionForDtoParameter(@AuthorizationCacheKey dto: TestDto) =
      EXPECTED_RESULT

  @InvalidatesAuthorizationCache
  open fun nullSpelExpressionForDtoParameter(@AuthorizationCacheKey("nullField") dto: TestDto) =
      EXPECTED_RESULT

  @InvalidatesAuthorizationCache
  open fun notUuidSpelExpressionForDtoParameter(
      @AuthorizationCacheKey("stringField") dto: TestDto
  ) = EXPECTED_RESULT

  companion object {
    const val EXPECTED_RESULT = "result"
  }
}

data class TestDto(
    val identifier: UUID,
    val nullField: String? = null,
    val stringField: String = "not a UUID"
)

data class NestedTestDto(val dto: TestDto)
