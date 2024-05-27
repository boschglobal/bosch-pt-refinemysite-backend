/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.kafka.streamable

import com.bosch.pt.iot.smartsite.common.repository.existencecache.EvictExistenceCache
import com.bosch.pt.iot.smartsite.common.repository.FindOneByIdentifierRepository
import java.io.Serializable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface KafkaStreamableRepository<
    T : AbstractKafkaStreamable<*, *, *>, K : Serializable, V : Enum<*>> :
    JpaRepository<T, K>, FindOneByIdentifierRepository {

  fun <S : T> saveAndFlush(entity: S, eventType: V): S

  fun <S : T> save(entity: S, eventType: V): S

  @EvictExistenceCache
  fun delete(entity: T, eventType: V)

  @EvictExistenceCache
  fun deleteAll(entities: Collection<T>, eventType: V)

  fun deleteAll(eventType: V)
}
