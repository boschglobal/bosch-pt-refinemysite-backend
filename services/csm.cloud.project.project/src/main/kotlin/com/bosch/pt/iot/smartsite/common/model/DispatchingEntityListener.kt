/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.model

import com.bosch.pt.iot.smartsite.common.model.AbstractCallbackAwarePersistable.CallbackType
import com.bosch.pt.iot.smartsite.common.model.AbstractCallbackAwarePersistable.CallbackType.POST_PERSIST
import com.bosch.pt.iot.smartsite.common.model.AbstractCallbackAwarePersistable.CallbackType.POST_UPDATE
import com.bosch.pt.iot.smartsite.common.model.AbstractCallbackAwarePersistable.CallbackType.PRE_REMOVE
import com.bosch.pt.iot.smartsite.common.model.AbstractCallbackAwarePersistable.CallbackType.PRE_UPDATE
import java.io.Serializable
import jakarta.persistence.PostPersist
import jakarta.persistence.PostUpdate
import jakarta.persistence.PreRemove
import jakarta.persistence.PreUpdate

/**
 * An entity listener that dispatches JPA lifecycle events to entities by invoking a callback on the
 * the corresponding entity.
 */
class DispatchingEntityListener {

  @Suppress("UnusedPrivateMember")
  @PostPersist
  private fun <T : Serializable, U : AbstractEntity<T, U>> postPersist(entity: U) =
      invokeCallbackOnEntity(entity, POST_PERSIST)

  @Suppress("UnusedPrivateMember")
  @PreUpdate
  private fun <T : Serializable, U : AbstractEntity<T, U>> preUpdate(entity: U) =
      invokeCallbackOnEntity(entity, PRE_UPDATE)

  @Suppress("UnusedPrivateMember")
  @PostUpdate
  private fun <T : Serializable, U : AbstractEntity<T, U>> postUpdate(entity: U) =
      invokeCallbackOnEntity(entity, POST_UPDATE)

  @Suppress("UnusedPrivateMember")
  @PreRemove
  private fun <T : Serializable, U : AbstractEntity<T, U>> preRemove(entity: U) =
      invokeCallbackOnEntity(entity, PRE_REMOVE)

  private fun <T : Serializable, U : AbstractEntity<T, U>> invokeCallbackOnEntity(
      entity: U,
      callbackType: CallbackType
  ) {
    entity.removeCallback(callbackType)?.accept(entity)
  }
}
