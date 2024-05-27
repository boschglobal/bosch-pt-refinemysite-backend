/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.eventstore

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.common.command.mapper.AvroEventMapper
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJpa
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.csm.cloud.common.command.snapshotstore.command.handler.TestEventStore
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import java.io.Serializable
import org.apache.avro.specific.SpecificRecordBase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.ApplicationEventPublisher

@ExtendWith(MockKExtension::class)
class BaseLocalEventBusTest {

  @RelaxedMockK private lateinit var eventStore: TestEventStore
  @RelaxedMockK private lateinit var eventPublisher: ApplicationEventPublisher
  @RelaxedMockK
  private lateinit var snapshotStore:
      AbstractSnapshotStoreJpa<
          SpecificRecordBase,
          VersionedSnapshot,
          AbstractSnapshotEntity<Serializable, UuidIdentifiable>,
          UuidIdentifiable>

  private lateinit var eventBus: BaseLocalEventBus<TestEventStore, SnapshotStore>

  @BeforeEach
  fun init() {
    eventBus = BaseLocalEventBus(eventStore, listOf(snapshotStore), eventPublisher, listOf())
  }

  @Test
  fun `verify emit throws NoSuchElementException if no mapper is found`() {
    assertThrows<NoSuchElementException> { eventBus.emit("testEvent", 1) }
  }

  @Test
  fun `verify emit throws IllegalArgumentException if more than one mapper is found`() {
    val mapper1 = mockk<AvroEventMapper>()
    val mapper2 = mockk<AvroEventMapper>()
    every { mapper1.canMap("testEvent") } returns true
    every { mapper2.canMap("testEvent") } returns true

    eventBus =
        BaseLocalEventBus(
            eventStore, listOf(snapshotStore), eventPublisher, listOf(mapper1, mapper2))
    assertThrows<IllegalArgumentException> { eventBus.emit("testEvent", 1) }
  }

  @Test
  fun `verify emit maps event to avro`() {
    val mapper1 = mockk<AvroEventMapper>(relaxed = true)
    every { mapper1.canMap("testEvent") } returns true
    every { snapshotStore.handlesMessage(any(), any()) } returns true
    eventBus = BaseLocalEventBus(eventStore, listOf(snapshotStore), eventPublisher, listOf(mapper1))

    eventBus.emit("testEvent", 1)

    verify(exactly = 1) { mapper1.mapToKey("testEvent", 1) }
    verify(exactly = 1) { mapper1.mapToValue("testEvent", 1) }
  }
}
