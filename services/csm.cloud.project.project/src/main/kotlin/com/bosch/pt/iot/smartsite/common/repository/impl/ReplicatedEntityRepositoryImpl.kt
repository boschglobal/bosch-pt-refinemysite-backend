/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.repository.impl

import com.bosch.pt.iot.smartsite.common.model.AbstractReplicatedEntity
import com.bosch.pt.iot.smartsite.common.model.AbstractReplicatedEntity_
import com.bosch.pt.iot.smartsite.common.model.VersionedEntity
import com.bosch.pt.iot.smartsite.common.repository.ReplicatedEntityRepository
import com.bosch.pt.iot.smartsite.common.repository.existencecache.EvictExistenceCache
import java.io.Serializable
import java.util.Optional
import java.util.UUID
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository

class ReplicatedEntityRepositoryImpl<T : AbstractReplicatedEntity<K>, K : Serializable>(
    entityInformation: JpaEntityInformation<T, *>,
    entityManager: EntityManager
) : SimpleJpaRepository<T, K>(entityInformation, entityManager), ReplicatedEntityRepository<T, K> {

  @Suppress("UNCHECKED_CAST")
  override fun findOne(identifier: UUID): Optional<VersionedEntity> =
      super.findOne(
          Specification { root: Root<T>, _: CriteriaQuery<*>, criteriaBuilder: CriteriaBuilder ->
            criteriaBuilder.equal(root.get(AbstractReplicatedEntity_.identifier), identifier)
          }
              as Specification<T>) as Optional<VersionedEntity>

  @EvictExistenceCache
  override fun delete(entity: T) {
    super.delete(entity)
  }

  @EvictExistenceCache
  override fun deleteAll(entities: MutableIterable<T>) {
    super.deleteAll(entities)
  }
}
