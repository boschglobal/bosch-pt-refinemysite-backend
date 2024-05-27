/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.model

import java.util.UUID
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AbstractEntityTest {

  @Test
  fun equalForSameObject() {
    val entity = TestEntity(null)

    assertThat(entity).isEqualTo(entity)
  }

  @Test
  fun equalForSameIdentifier() {
    val identifier = randomUUID()
    val entity1 = TestEntity(identifier)
    val entity2 = TestEntity(identifier)

    assertThat(entity1).isEqualTo(entity2)
    assertThat(entity2).isEqualTo(entity1)
  }

  @Test
  fun equalForSubclassWithSameIdentifier() {
    val identifier = randomUUID()
    val entity1 = TestEntity(identifier)
    val entity2 = SubclassedTestEntity(identifier)

    assertThat(entity1).isEqualTo(entity2)
    assertThat(entity2).isEqualTo(entity1)
  }

  @Test
  fun notEqualForDifferentIdentifiers() {
    val entity1 = TestEntity(randomUUID())
    val entity2 = TestEntity(randomUUID())

    assertThat(entity1).isNotEqualTo(entity2)
    assertThat(entity2).isNotEqualTo(entity1)
  }

  @Test
  fun notEqualForNullIdentifiers() {
    val entity1 = TestEntity(null)
    val entity2 = TestEntity(null)

    assertThat(entity1).isNotEqualTo(entity2)
    assertThat(entity2).isNotEqualTo(entity1)
  }

  @Test
  fun notEqualForWrongArgumentType() {
    val entity = TestEntity(randomUUID())

    assertThat(entity).isNotEqualTo("notAnEntity")
  }

  private open class TestEntity(override var identifier: UUID?) :
      AbstractEntity<Long, TestEntity>() {

    override fun getDisplayName(): String? = null

    override fun getAggregateType(): String = "null"
  }

  private class SubclassedTestEntity(identifier: UUID?) : TestEntity(identifier)
}
