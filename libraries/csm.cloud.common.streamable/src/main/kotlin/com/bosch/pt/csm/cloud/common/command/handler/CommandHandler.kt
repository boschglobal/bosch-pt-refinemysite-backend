/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.handler

import com.bosch.pt.csm.cloud.common.command.exceptions.EntityOutdatedException
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler.CheckType.AUTHORIZATION
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler.CheckType.PRECONDITION
import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler.CheckType.UNDEFINED
import com.bosch.pt.csm.cloud.common.command.mapper.AvroSnapshotMapper
import com.bosch.pt.csm.cloud.common.command.mapper.NotImplementedAvroSnapshotMapper
import com.bosch.pt.csm.cloud.common.command.snapshotstore.ObjectCopier
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.csm.cloud.common.eventstore.BaseLocalEventBus
import com.bosch.pt.csm.cloud.common.eventstore.BaseLocalEventBusWithTombstoneSupport
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.translation.CommonStreamableKey
import org.springframework.security.access.AccessDeniedException

/**
 * This class is used in command handler methods to provide a fluent API for typical command
 * handling steps based on a given implementation of [AvroSnapshotMapper]. The methods can be
 * chained for easy usage.
 */
class CommandHandler<T : VersionedSnapshot>
private constructor(
    val snapshot: T,
    val snapshotMapper: AvroSnapshotMapper<T>,
    private val original: T,
    private val eventType: Enum<*>? = null,
    private var isDirty: Boolean = false,
    private var checkDirty: Boolean = false,
    private var sendTombstone: Boolean = false,
    private var checkType: CheckType = UNDEFINED,
    private var currentCheck: (T) -> Boolean = { false },
    private val events: List<Event>?,
) :
    CommandHandlerChangeDefinition<T>,
    CommandHandlerEmitEventDefinition<T>,
    CommandHandlerEmitTombstoneDefinition<T>,
    CommandHandlerExceptionDefinition<T>,
    CommandHandlerSideEffectDefinition<T>,
    CommandHandlerDirtyCheckDefinition<T>,
    CommandHandlerBusDefinition<T>,
    CommandHandlerReturnSnapshotDefinition<T> {

  private constructor(
      snapshot: T,
      snapshotMapper: AvroSnapshotMapper<T> = NotImplementedAvroSnapshotMapper(),
  ) : this(
      snapshot, snapshotMapper, ObjectCopier.deepCopy(snapshot, snapshot.javaClass), events = null)

  override fun assertVersionMatches(version: Long): CommandHandler<T> =
      this.apply {
        if (this.snapshot.version != version) {
          throw EntityOutdatedException(CommonStreamableKey.COMMON_VALIDATION_ERROR_ENTITY_OUTDATED)
        }
      }

  override fun checkAuthorization(precondition: (T) -> Boolean): CommandHandler<T> =
      this.apply {
        currentCheck = precondition
        checkType = AUTHORIZATION
      }

  override fun checkPrecondition(precondition: (T) -> Boolean): CommandHandler<T> =
      this.apply {
        currentCheck = precondition
        checkType = PRECONDITION
      }

  override fun onFailureThrow(exceptionProducer: () -> Exception): CommandHandler<T> =
      this.apply { if (!currentCheck.invoke(snapshot)) throw exceptionProducer.invoke() }

  override fun onFailureThrow(failureMessageKey: String): CommandHandler<T> =
      this.apply {
        if (!currentCheck.invoke(snapshot))
            when (checkType) {
              UNDEFINED -> error("Command handler defined a check with undefined type.")
              PRECONDITION -> throw PreconditionViolationException(failureMessageKey)
              AUTHORIZATION -> throw AccessDeniedException(failureMessageKey)
            }
      }

  override fun applyChanges(block: (T) -> Unit): CommandHandler<T> = applyBlock(block)

  override fun update(block: (T) -> T): CommandHandlerEmitEventDefinition<T> {
    val modifiedSnapshot = block.invoke(snapshot)
    return this.copy(snapshot = modifiedSnapshot)
  }

  override fun withSideEffects(block: (T) -> Unit): CommandHandler<T> = applyBlock(block)

  override fun emitEvent(eventType: Enum<*>): CommandHandler<T> = this.copy(eventType = eventType)

  override fun emitEvent(event: Any): CommandHandler<T> = this.copy(events = listOf(event))

  override fun emitTombstone(): CommandHandler<T> = this.apply { sendTombstone = true }

  override fun emitEvents(block: (snapshot: T) -> List<Event>): CommandHandlerBusDefinition<T> =
      this.copy(events = block.invoke(original))

  override fun ifSnapshotWasChanged(): CommandHandler<T> =
      this.apply {
        this.checkDirty = true
        this.isDirty = original != snapshot
      }

  override fun to(eventBus: BaseLocalEventBusWithTombstoneSupport<*, *>): CommandHandler<T> {
    check(listOf(events != null, sendTombstone, eventType != null).single { it }) {
      "Only one of events, sendTombstone or eventType must be set"
    }

    when {
      events != null -> {
        if (!checkDirty || isDirty) {
          for (i in events.indices) {
            eventBus.emit(events[i], snapshot.version + i + 1)
          }
        }
      }
      sendTombstone -> {
        eventBus.emitTombstone(snapshotMapper.toMessageKeyWithCurrentVersion(snapshot))
      }
      eventType != null -> {
        if (!checkDirty || isDirty) {
          eventBus.emit(
              snapshotMapper.toMessageKeyWithNewVersion(snapshot),
              snapshotMapper.toAvroMessageWithNewVersion(snapshot, eventType))
        }
      }
    }
    return this
  }

  override fun to(eventBus: BaseLocalEventBus<*, *>): CommandHandler<T> {
    check(listOf(events != null, sendTombstone, eventType != null).single { it }) {
      "Only one of events, sendTombstone or eventType must be set"
    }

    when {
      events != null -> {
        if (!checkDirty || isDirty) {
          for (i in events.indices) {
            eventBus.emit(events[i], snapshot.version + i + 1)
          }
        }
      }
      sendTombstone -> {
        throw IllegalArgumentException("Event bus of type doesn't supported tombstone messages")
      }
      else -> {
        if (!checkDirty || isDirty) {
          eventBus.emit(
              snapshotMapper.toMessageKeyWithNewVersion(snapshot),
              snapshotMapper.toAvroMessageWithNewVersion(snapshot, eventType as Enum<*>))
        }
      }
    }
    return this
  }

  override fun andReturnSnapshot(): T = snapshot

  private fun applyBlock(block: (T) -> Unit): CommandHandler<T> =
      with(snapshot) {
        block.invoke(snapshot)
        return@with this@CommandHandler
      }

  private fun copy(
      snapshot: T = this.snapshot,
      original: T = this.original,
      eventType: Enum<*>? = this.eventType,
      isDirty: Boolean = this.isDirty,
      checkDirty: Boolean = this.checkDirty,
      sendTombstone: Boolean = this.sendTombstone,
      checkType: CheckType = this.checkType,
      currentCheck: (T) -> Boolean = this.currentCheck,
      events: List<Event>? = this.events
  ) =
      CommandHandler(
          snapshot,
          snapshotMapper,
          original,
          eventType,
          isDirty,
          checkDirty,
          sendTombstone,
          checkType,
          currentCheck,
          events)

  companion object {
    fun <T : VersionedSnapshot> of(
        snapshot: T,
        snapshotMapper: AvroSnapshotMapper<T> = NotImplementedAvroSnapshotMapper()
    ): CommandHandlerChangeDefinition<T> = CommandHandler(snapshot, snapshotMapper)
  }

  private enum class CheckType {
    UNDEFINED,
    PRECONDITION,
    AUTHORIZATION
  }
}
