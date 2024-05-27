/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.model

import java.util.UUID
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class AbstractPersistableEntityTest {

  @Test
  fun equalForSameObject() {
    val entity = TestEntity(randomUUID())

    assertThat(entity).isEqualTo(entity)
  }

  @Test
  fun equalForObjectNull() {
    val entity = TestEntity(randomUUID())
    assertThat(entity).isNotEqualTo(null)
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
  fun notEqualForDifferentIdentifiers() {
    val entity1 = TestEntity(randomUUID())
    val entity2 = TestEntity(randomUUID())

    assertThat(entity1).isNotEqualTo(entity2)
    assertThat(entity2).isNotEqualTo(entity1)
  }

  @Test
  fun notEqualForWrongArgumentType() {
    val entity = TestEntity(randomUUID())

    assertThat(entity).isNotEqualTo("notAnEntity")
  }

  @Test
  fun isNewTrueForNewlyCreatedEntity() {
    val entity = TestEntity(randomUUID())
    assertThat(entity.isNew).isTrue
  }

  @Test
  fun identifierIsSetCorrectly() {
    val id = randomUUID()
    val entity = TestEntity(id)
    assertThat(entity.id).isEqualTo(id)
    val otherId = randomUUID()
    entity.modifyMyId(otherId)
    assertThat(entity.id).isEqualTo(otherId)
  }

  @Test
  fun hashOfId() {
    val entity = TestEntity(randomUUID())
    assertThat(entity.hashCode()).isEqualTo(entity.id.hashCode())
  }

  private open class TestEntity(identifier: UUID) : AbstractPersistableEntity<UUID>(identifier) {
    override fun getId(): UUID? = id

    fun modifyMyId(otherId: UUID) {
      this.id = otherId
    }
  }
}
