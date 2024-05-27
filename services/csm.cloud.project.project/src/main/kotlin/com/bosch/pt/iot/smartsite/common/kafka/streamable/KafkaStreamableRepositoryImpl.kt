/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.kafka.streamable

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverageGenerated
import com.bosch.pt.iot.smartsite.common.model.AbstractEntity_
import com.bosch.pt.iot.smartsite.common.model.VersionedEntity
import com.bosch.pt.iot.smartsite.common.util.returnUnit
import java.io.Serializable
import java.lang.UnsupportedOperationException
import java.util.Optional
import java.util.UUID
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository

class KafkaStreamableRepositoryImpl<
    T : AbstractKafkaStreamable<K, *, V>, K : Serializable, V : Enum<*>>(
    entityInformation: JpaEntityInformation<T, *>,
    entityManager: EntityManager
) :
    SimpleJpaRepository<T, K>(entityInformation, entityManager),
    KafkaStreamableRepository<T, K, V> {

  override fun <S : T> save(entity: S, eventType: V): S =
      entity.apply { this.eventType = eventType }.also { super.save(it) }

  @ExcludeFromCodeCoverageGenerated
  override fun <S : T> save(entity: S): S = throw UnsupportedOperationException(EVENT_TYPE_MISSING)

  override fun <S : T> saveAndFlush(entity: S, eventType: V): S =
      save(entity, eventType).apply { flush() }

  @ExcludeFromCodeCoverageGenerated
  override fun <S : T?> saveAndFlush(entity: S): S =
      throw UnsupportedOperationException(EVENT_TYPE_MISSING)

  override fun delete(entity: T, eventType: V) =
      entity.apply { this.eventType = eventType }.also { super.delete(it) }.returnUnit()

  @ExcludeFromCodeCoverageGenerated
  override fun delete(entity: T) {
    if (entity.eventType == null) {
      throw UnsupportedOperationException(EVENT_TYPE_MISSING)
    } else {
      super.delete(entity)
    }
  }

  @ExcludeFromCodeCoverageGenerated
  override fun deleteAll(entities: Collection<T>, eventType: V) {
    entities.forEach { it.eventType = eventType }
    deleteAll(entities)
  }

  @ExcludeFromCodeCoverageGenerated
  override fun deleteAll(eventType: V) {
    for (element in findAll()) {
      delete(element, eventType)
    }
  }

  @ExcludeFromCodeCoverageGenerated
  override fun deleteAll(): Unit = throw UnsupportedOperationException(EVENT_TYPE_MISSING)

  @Suppress("UNCHECKED_CAST")
  override fun findOne(identifier: UUID): Optional<VersionedEntity> =
      this.findOne(
          Specification { root: Root<T>, _: CriteriaQuery<*>, criteriaBuilder: CriteriaBuilder ->
            criteriaBuilder.equal(root.get(AbstractEntity_.identifier), identifier)
          } as
              Specification<T>) as
          Optional<VersionedEntity>

  companion object {
    private const val EVENT_TYPE_MISSING = "Event Type is missing"
  }
}
