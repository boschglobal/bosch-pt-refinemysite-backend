/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.facade.rest.resource

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.iot.smartsite.common.model.AbstractEntity
import com.bosch.pt.iot.smartsite.common.model.AbstractReplicatedEntity
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.UserBuilder.Companion.user
import java.time.LocalDateTime
import java.util.UUID.randomUUID
import java.util.function.Supplier
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AbstractAuditableResourceTest {

  private lateinit var user: User
  private lateinit var deletedUser: User
  private lateinit var deletedUserReference: ResourceReference

  @BeforeEach
  fun setUp() {
    deletedUserReference = ResourceReference(randomUUID(), RandomStringUtils.random(10))
    user = user().build()
    deletedUser = user().asDeleted(true).build()
  }

  @Test
  fun verifyEntityWithDeletedLastModifiedByUser() {
    TestEntity()
        .apply { setLastModifiedBy(deletedUser) }
        .let { TestAuditableResource(it) { deletedUserReference } }
        .also { assertThat(it.lastModifiedBy).isEqualTo(deletedUserReference) }
  }

  @Test
  fun verifyEntityWithDeletedCreatedByUser() {
    TestEntity()
        .apply { setCreatedBy(deletedUser) }
        .let { TestAuditableResource(it) { deletedUserReference } }
        .also { assertThat(it.createdBy).isEqualTo(deletedUserReference) }
  }

  @Test
  fun verifyReplicatedEntityWithDeletedLastModifiedByUser() {
    TestReplicatedEntity()
        .apply { setLastModifiedBy(deletedUser) }
        .let { TestAuditableResource(it) { deletedUserReference } }
        .also { assertThat(it.lastModifiedBy).isEqualTo(deletedUserReference) }
  }

  @Test
  fun verifyReplicatedEntityWithDeletedCreatedByUser() {
    TestReplicatedEntity()
        .apply { setCreatedBy(deletedUser) }
        .let { TestAuditableResource(it) { deletedUserReference } }
        .also { assertThat(it.createdBy).isEqualTo(deletedUserReference) }
  }

  private class TestAuditableResource : AbstractAuditableResource {
    constructor(
        entity: AbstractEntity<Long, *>,
        deletedUserReference: Supplier<ResourceReference>
    ) : super(entity, deletedUserReference)

    constructor(
        entity: AbstractReplicatedEntity<Long>,
        deletedUserReference: Supplier<ResourceReference>
    ) : super(entity, deletedUserReference)
  }

  private class TestEntity : AbstractEntity<Long, TestEntity>() {

    init {
      identifier = randomUUID()
      version = 0
      setCreatedDate(LocalDateTime.now())
      setCreatedBy(user().build())
      setLastModifiedBy(user().build())
      setLastModifiedDate(LocalDateTime.now())
    }

    override fun getDisplayName() = "Test"
    override fun getAggregateType() = "Test"
  }

  private class TestReplicatedEntity : AbstractReplicatedEntity<Long>() {

    init {
      identifier = randomUUID()
      version = 0
      setCreatedDate(LocalDateTime.now())
      setCreatedBy(user().build())
      setLastModifiedBy(user().build())
      setLastModifiedDate(LocalDateTime.now())
    }

    override fun getDisplayName() = "Test"
    override fun getAggregateType() = "Test"
  }
}
