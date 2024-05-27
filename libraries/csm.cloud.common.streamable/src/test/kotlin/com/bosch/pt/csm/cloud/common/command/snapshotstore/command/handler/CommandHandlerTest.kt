/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.snapshotstore.command.handler

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.exceptions.EntityOutdatedException
import com.bosch.pt.csm.cloud.common.command.snapshotstore.command.handler.TestEventEnum.CREATED
import com.bosch.pt.csm.cloud.common.eventstore.LocalEventBus
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import java.time.LocalDateTime.now
import java.util.UUID.randomUUID
import org.apache.avro.specific.SpecificRecordBase
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.access.AccessDeniedException

@ExtendWith(MockKExtension::class)
class CommandHandlerTest {

  private val expectedRootContextIdentifier = randomUUID()
  private val expectedAggregateIdentifier = UserId()
  private val testSnapshot =
      TestSnapshot(
          expectedAggregateIdentifier,
          2,
          now(),
          now(),
          UserId(),
          UserId(),
          expectedRootContextIdentifier)
  private val commandHandler = testSnapshot.toCommandHandler()

  @MockK(relaxed = true) private lateinit var eventBus: TestEventBus
  @MockK(relaxed = true)
  private lateinit var eventBusWithTombstoneSupport: TestEventBusWithTombstoneSupport

  private val tombstoneMessageSlot = mutableListOf<AggregateEventMessageKey>()

  @AfterEach
  fun clear() {
    clearAllMocks()
  }

  @Test
  fun `verify assert version succeeds when matching`() {
    commandHandler.assertVersionMatches(2)
  }

  @Test
  fun `verify assert version fails when smaller`() {
    assertThatExceptionOfType(EntityOutdatedException::class.java).isThrownBy {
      commandHandler.assertVersionMatches(0)
    }
    assertThatExceptionOfType(EntityOutdatedException::class.java).isThrownBy {
      commandHandler.assertVersionMatches(1)
    }
  }

  @Test
  fun `verify assert version fails when larger`() {
    assertThatExceptionOfType(EntityOutdatedException::class.java).isThrownBy {
      commandHandler.assertVersionMatches(3)
    }
  }

  @Test
  fun `verify throws AccessDeniedException when authorization check fails`() {
    assertThatExceptionOfType(AccessDeniedException::class.java).isThrownBy {
      commandHandler.checkAuthorization { false }.onFailureThrow("Message")
    }
  }

  @Test
  fun `verify throws PreconditionViolationException when precondition check fails`() {
    assertThatExceptionOfType(PreconditionViolationException::class.java).isThrownBy {
      commandHandler.checkPrecondition { false }.onFailureThrow("Message")
    }
  }

  @Test
  fun `verify emit event to bus is executed when snapshot changed and check was required `() {
    every { eventBus.emit(any(), any<SpecificRecordBase>()) }.returns(Unit)
    commandHandler
        .assertVersionMatches(2)
        .applyChanges { it.name = "changed" }
        .emitEvent(CREATED)
        .ifSnapshotWasChanged()
        .to(eventBus)
    verify(exactly = 1) { eventBus.emit(any(), any<SpecificRecordBase>()) }
    verify(exactly = 0) { eventBus.emitTombstone(any()) }
  }

  @Test
  fun `verify emit tombstone to bus is executed successfully when bus supports tombstones`() {
    every { eventBusWithTombstoneSupport.emitTombstone(capture(tombstoneMessageSlot)) }
        .returns(Unit)
    commandHandler.assertVersionMatches(2).emitTombstone().to(eventBusWithTombstoneSupport)
    verify(exactly = 0) { eventBusWithTombstoneSupport.emit(any(), any<SpecificRecordBase>()) }
    verify(exactly = 1) { eventBusWithTombstoneSupport.emitTombstone(any()) }
    verifyAggregateMessageKey(tombstoneMessageSlot[0], 2L)
  }

  private fun verifyAggregateMessageKey(
      messageKeyToVerify: AggregateEventMessageKey,
      expectedVersion: Long
  ) {
    messageKeyToVerify.apply {
      assertThat(rootContextIdentifier).isEqualTo(expectedRootContextIdentifier)
      aggregateIdentifier.apply {
        assertThat(type).isEqualTo("USER")
        assertThat(identifier).isEqualTo(expectedAggregateIdentifier.toUuid())
        assertThat(version).isEqualTo(expectedVersion)
      }
    }
  }

  @Test
  fun `verify emit tombstone to bus fails when bus does not support tombstones`() {
    every { eventBus.emitTombstone(any()) }.returns(Unit)
    assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
      commandHandler.assertVersionMatches(2).emitTombstone().to(eventBus)
    }
  }

  @Test
  fun `snapshot event is not emitted if snapshot did not change`() {
    commandHandler.assertVersionMatches(2).emitEvent(CREATED).ifSnapshotWasChanged().to(eventBus)
    `verify nothing emitted on event bus`(eventBus)
  }

  @Test
  fun `snapshot event is not emitted if snapshot did not change (tombstone event bus)`() {
    commandHandler
        .assertVersionMatches(2)
        .emitEvent(CREATED)
        .ifSnapshotWasChanged()
        .to(eventBusWithTombstoneSupport)
    `verify nothing emitted on event bus`(eventBusWithTombstoneSupport)
  }

  @Test
  fun `event is not emitted if snapshot did not change`() {
    commandHandler
        .assertVersionMatches(2)
        .emitEvent("testEvent")
        .ifSnapshotWasChanged()
        .to(eventBus)
    `verify nothing emitted on event bus`(eventBus)
  }

  @Test
  fun `event is not emitted if snapshot did not change (tombstone event bus)`() {
    commandHandler
        .assertVersionMatches(2)
        .emitEvent("testEvent")
        .ifSnapshotWasChanged()
        .to(eventBusWithTombstoneSupport)
    `verify nothing emitted on event bus`(eventBusWithTombstoneSupport)
  }

  // Just to verify compilation works - not test execution required
  fun `verify chained invocation based on a new snapshot compiles`() {
    testSnapshot.toCommandHandler().emitEvent(CREATED).to(eventBus)
  }

  // Just to verify compilation works - not test execution required
  fun `verify chained invocation based on an existing snapshot compiles`() {
    commandHandler.assertVersionMatches(1)
    commandHandler.checkPrecondition { true }.onFailureThrow("blah")
    commandHandler.applyChanges {}
    commandHandler
        .assertVersionMatches(1)
        .checkAuthorization { true }
        .onFailureThrow("dummy")
        .checkPrecondition { true }
        .onFailureThrow("dummy")
    commandHandler.assertVersionMatches(1).applyChanges {}
    commandHandler
        .checkPrecondition { true }
        .onFailureThrow { PreconditionViolationException("blah") }
        .applyChanges {}
    commandHandler.applyChanges {}
    commandHandler.applyChanges {}.emitEvent(CREATED)
    commandHandler.applyChanges {}.emitEvent(CREATED).to(eventBus)
    commandHandler.applyChanges {}.emitEvent(CREATED).ifSnapshotWasChanged().to(eventBus)
    commandHandler
        .applyChanges {}
        .emitEvent(CREATED)
        .ifSnapshotWasChanged()
        .to(eventBus)
        .withSideEffects {}
  }

  // Just to verify compilation works - not test execution required
  fun `verify chained invocation for tombstone compiles`() {
    commandHandler.emitTombstone().to(eventBusWithTombstoneSupport)
    commandHandler
        .checkPrecondition { true }
        .onFailureThrow("dummy")
        .emitTombstone()
        .to(eventBusWithTombstoneSupport)
    commandHandler
        .assertVersionMatches(1)
        .emitTombstone()
        .to(eventBusWithTombstoneSupport)
        .withSideEffects {}
        .andReturnSnapshot()
  }

  @Test
  fun `verify that emit gets called with incremented version (for event bus with tombstone)`() {
    every { eventBusWithTombstoneSupport.emit(any(), any<Long>()) } just runs

    testSnapshot.toCommandHandler().emitEvent("some event").to(eventBusWithTombstoneSupport)

    verify { eventBusWithTombstoneSupport.emit("some event", 3) }
  }

  @Test
  fun `verify that emit gets called with incremented version`() {
    every { eventBus.emit(any(), any<Long>()) } just runs

    testSnapshot.toCommandHandler().emitEvent("some event").to(eventBus)

    verify { eventBus.emit("some event", 3) }
  }

  @Test
  fun `verify that after emitEvents with 2 events, emit gets called 2 times`() {
    every { eventBus.emit(any(), any<Long>()) } just runs

    testSnapshot.toCommandHandler().emitEvents { listOf("event1", "event2") }.to(eventBus)

    verify(exactly = 2) { eventBus.emit(any(), any<Long>()) }
  }

  @Test
  fun `verify that emitSingleEvent emits 1 event and emit gets called 1 time`() {
    every { eventBus.emit(any(), any<Long>()) } just runs

    testSnapshot.toCommandHandler().emitEvent { "event1" }.to(eventBus)

    verify(exactly = 1) { eventBus.emit(any(), any<Long>()) }
  }

  private fun `verify nothing emitted on event bus`(eventBus: LocalEventBus) {
    verify(exactly = 0) { eventBus.emit(any(), any<SpecificRecordBase>()) }
    verify(exactly = 0) { eventBus.emit(any(), any<Long>()) }
    verify(exactly = 0) { eventBus.emitTombstone(any()) }
  }
}
