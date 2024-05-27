/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.repository.existencecache

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.model.AbstractEntity
import com.bosch.pt.iot.smartsite.common.model.AbstractReplicatedEntity
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import java.util.UUID
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@EnableAllKafkaListeners
open class ExistenceCacheTest : AbstractIntegrationTestV2() {

  @Autowired lateinit var existenceCache: ExistenceCache

  @Autowired private lateinit var testRepository: TestRepository

  @BeforeEach
  fun init() {
    // Set test repository call count back to zero
    TestRepository.callCount = 0
  }

  @Transactional
  @Test
  open fun `existence cache works properly for single snapshot entity`() {

    val resultSnapshotEntity = TestSnapshotEntity()

    // Check if cache is populated
    testRepository.populateSingle(resultSnapshotEntity)
    assertThat(existenceCache.size() == 1)

    // Call method annotated with UseExistenceCache and check that cache is used
    testRepository.use(resultSnapshotEntity.identifier)
    assertThat(TestRepository.callCount == 0)

    // Call method annotated with UseExistenceCache with other identifier and check that cache is
    // not used
    testRepository.use(ProjectId())
    assertThat(TestRepository.callCount == 1)

    // Delete the entity and check that is removed from the cache
    testRepository.delete(resultSnapshotEntity)
    assertThat(existenceCache.size() == 0)
  }

  @Transactional
  @Test
  open fun `existence cache works properly for collection of snapshot entities`() {

    val resultSnapshotEntity = TestSnapshotEntity()

    // Check if cache is populated
    testRepository.populateMultiple(resultSnapshotEntity)
    assertThat(existenceCache.size() == 1)

    // Call method annotated with UseExistenceCache and check that cache is used
    testRepository.use(resultSnapshotEntity.identifier)
    assertThat(TestRepository.callCount == 0)

    // Call method annotated with UseExistenceCache with other identifier and check that cache is
    // not used
    testRepository.use(ProjectId())
    assertThat(TestRepository.callCount == 1)

    // Delete the entity and check that is removed from the cache
    testRepository.deleteAll(listOf(resultSnapshotEntity))
    assertThat(existenceCache.size() == 0)
  }

  @Transactional
  @Test
  open fun `existence cache works properly for single entity`() {

    val resultEntity = TestEntity()

    // Check if cache is populated
    testRepository.populateSingle(resultEntity)
    assertThat(existenceCache.size() == 1)

    // Call method annotated with UseExistenceCache and check that cache is used
    testRepository.use(resultEntity.identifier!!)
    assertThat(TestRepository.callCount == 0)

    // Call method annotated with UseExistenceCache with other identifier and check that cache is
    // not used
    testRepository.use(randomUUID())
    assertThat(TestRepository.callCount == 1)

    // Delete the entity and check that is removed from the cache
    testRepository.delete(resultEntity)
    assertThat(existenceCache.size() == 0)
  }

  @Transactional
  @Test
  open fun `existence cache works properly for collection of entities`() {

    val resultEntity = TestEntity()

    // Check if cache is populated
    testRepository.populateMultiple(resultEntity)
    assertThat(existenceCache.size() == 1)

    // Call method annotated with UseExistenceCache and check that cache is used
    testRepository.use(resultEntity.identifier!!)
    assertThat(TestRepository.callCount == 0)

    // Call method annotated with UseExistenceCache with other identifier and check that cache is
    // not used
    testRepository.use(randomUUID())
    assertThat(TestRepository.callCount == 1)

    // Delete the entity and check that is removed from the cache
    testRepository.deleteAll(listOf(resultEntity))
    assertThat(existenceCache.size() == 0)
  }

  @Transactional
  @Test
  open fun `existence cache works properly for single replicated entity`() {

    val resultReplicatedEntity = TestReplicatedEntity()

    // Check if cache is populated
    testRepository.populateSingle(resultReplicatedEntity)
    assertThat(existenceCache.size() == 1)

    // Call method annotated with UseExistenceCache and check that cache is used
    testRepository.use(resultReplicatedEntity.identifier!!)
    assertThat(TestRepository.callCount == 0)

    // Call method annotated with UseExistenceCache with other identifier and check that cache is
    // not used
    testRepository.use(randomUUID())
    assertThat(TestRepository.callCount == 1)

    // Delete the entity and check that is removed from the cache
    testRepository.delete(resultReplicatedEntity)
    assertThat(existenceCache.size() == 0)
  }

  @Transactional
  @Test
  open fun `existence cache works properly for collection of replicated entities`() {

    val resultReplicatedEntity = TestReplicatedEntity()

    // Check if cache is populated
    testRepository.populateMultiple(resultReplicatedEntity)
    assertThat(existenceCache.size() == 1)

    // Call method annotated with UseExistenceCache and check that cache is used
    testRepository.use(resultReplicatedEntity.identifier!!)
    assertThat(TestRepository.callCount == 0)

    // Call method annotated with UseExistenceCache with other identifier and check that cache is
    // not used
    testRepository.use(randomUUID())
    assertThat(TestRepository.callCount == 1)

    // Delete the entity and check that is removed from the cache
    testRepository.deleteAll(listOf(resultReplicatedEntity))
    assertThat(existenceCache.size() == 0)
  }

  @Transactional
  @Test
  open fun `existence cache populate fails for not supported class`() {
    assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
      testRepository.populateSingle(1)
    }
  }

  @Transactional
  @Test
  open fun `existence cache populate fails for collection of not supported class`() {
    assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
      testRepository.populateMultiple(1)
    }
  }

  @Transactional
  @Test
  open fun `existence cache use fails for method that does not return boolean`() {
    assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
      testRepository.useNotReturningBoolean(randomUUID())
    }
  }

  @Transactional
  @Test
  open fun `existence cache delete fails for not supported class`() {
    assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
      testRepository.delete(1)
    }
  }

  @Transactional
  @Test
  open fun `existence cache delete fails for collection of not supported class`() {
    assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
      testRepository.deleteAll(listOf(1))
    }
  }
}

@Component
open class TestRepository {

  // Methods to test existence cache

  @PopulateExistenceCache(cacheName = "test", keyFromResult = ["identifier"])
  open fun populateSingle(entityToReturn: Any): Any {
    return entityToReturn
  }

  @PopulateExistenceCache(cacheName = "test", keyFromResult = ["identifier"])
  open fun populateMultiple(entityToReturn: Any): Collection<Any> {
    return listOf(entityToReturn)
  }

  @UseExistenceCache(cacheName = "test", keyFromParameters = ["identifier"])
  open fun use(identifier: Any): Boolean {
    callCount = callCount.plus(1)
    return true
  }

  @EvictExistenceCache open fun delete(entityToDelete: Any) = true

  @EvictExistenceCache open fun deleteAll(snapshotEntities: Collection<Any>) = true

  // Methods to test expected error cases

  @UseExistenceCache(cacheName = "test", keyFromParameters = ["identifier"])
  open fun useNotReturningBoolean(identifier: UUID): Int {
    callCount = callCount.plus(1)
    return 1
  }

  companion object {
    var callCount = 0
  }
}

class TestSnapshotEntity : AbstractSnapshotEntity<Long, ProjectId>() {

  init {
    super.identifier = ProjectId()
  }

  override fun getDisplayName() = "TestSnapshotEntity"
}

class TestEntity : AbstractEntity<Long, TestEntity>() {

  init {
    super.identifier = randomUUID()
  }

  override fun getAggregateType() = "TestEntity"

  override fun getDisplayName() = "TestEntity"
}

class TestReplicatedEntity : AbstractReplicatedEntity<Long>() {

  init {
    this.identifier = randomUUID()
  }

  override fun getAggregateType() = "TestReplicatedEntity"

  override fun getDisplayName() = "TestReplicatedEntity"
}
