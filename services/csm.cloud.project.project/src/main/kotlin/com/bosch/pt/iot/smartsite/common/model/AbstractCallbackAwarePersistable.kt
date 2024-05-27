/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.model

import com.bosch.pt.csm.cloud.common.api.AbstractPersistable
import com.bosch.pt.iot.smartsite.common.model.AbstractCallbackAwarePersistable.CallbackType.POST_PERSIST
import com.bosch.pt.iot.smartsite.common.model.AbstractCallbackAwarePersistable.CallbackType.POST_UPDATE
import com.bosch.pt.iot.smartsite.common.model.AbstractCallbackAwarePersistable.CallbackType.PRE_REMOVE
import com.bosch.pt.iot.smartsite.common.model.AbstractCallbackAwarePersistable.CallbackType.PRE_UPDATE
import java.io.Serializable
import java.util.EnumMap
import java.util.function.Consumer
import jakarta.persistence.Transient
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Abstract base class for entities which need to react on changes in their lifecycle state
 * (created, updated, deleted). The reaction in case of a state change is supplied by the client via
 * a callback.
 *
 * @param <T> type of primary key extending [Serializable].
 * @param <U> concrete type of this entity </U></T>
 */
abstract class AbstractCallbackAwarePersistable<T : Serializable, U : AbstractEntity<T, U>> :
    AbstractPersistable<T>() {

  @Transient
  private val callbacks: MutableMap<CallbackType, Consumer<U>> = EnumMap(CallbackType::class.java)

  /**
   * Sets the callback to be invoked right after this entity has been persisted, i.e. after the
   * INSERT statement(s) has been sent to the database, and before the enclosing transaction ends in
   * a commit or rollback.
   *
   * @param callback the callback to be invoked
   */
  fun afterPersist(callback: Consumer<U>) = registerCallback(callback, POST_PERSIST)

  /**
   * Sets the callback to invoked right before this entity is being updated, i.e. before sending the
   * UPDATE statement(s) to the database.
   *
   * The given callback will only be invoked if the entity is dirty. An entity is considered dirty
   * if one or more of its attributes actually changed compared to what's persisted in the database.
   *
   * Within the callback you usually want to set additional attributes that should change only if
   * the entity is dirty.
   *
   * @param callback the callback to be invoked
   */
  fun beforeUpdate(callback: Consumer<U>) = registerCallback(callback, PRE_UPDATE)

  /**
   * Sets the callback to be invoked right after this entity has been updated, i.e. after the UPDATE
   * statement(s) has been sent to the database, and before the enclosing transaction ends in a
   * commit or rollback.
   *
   * @param callback the callback to be invoked
   */
  fun afterUpdate(callback: Consumer<U>) = registerCallback(callback, POST_UPDATE)

  /**
   * Sets the callback to invoked right before this entity is being deleted, i.e. before sending the
   * DELETE statement(s) to the database.
   *
   * @param callback the callback to be invoked
   */
  fun beforeDelete(callback: Consumer<U>) = registerCallback(callback, PRE_REMOVE)

  private fun registerCallback(callback: Consumer<U>, callbackType: CallbackType) {
    require(!callbacks.containsKey(callbackType)) {
      "Cannot set the same callback type multiple times."
    }

    callbacks[callbackType] = callback

    // remove callback after transaction completion, if not removed already
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          object : TransactionSynchronization {
            override fun afterCompletion(status: Int) {
              callbacks.remove(callbackType)
            }
          })
    }
  }

  fun removeCallback(type: CallbackType): Consumer<U>? = callbacks.remove(type)

  enum class CallbackType {
    POST_PERSIST,
    PRE_UPDATE,
    POST_UPDATE,
    PRE_REMOVE
  }
}
