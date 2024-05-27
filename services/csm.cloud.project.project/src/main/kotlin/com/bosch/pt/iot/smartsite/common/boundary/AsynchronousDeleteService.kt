/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.boundary

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import java.util.UUID

/**
 * This interface defines methods (s a kind of protocol) to delete an entity with all nested objects
 * asynchronously. This should be used to delete entities with potentially lots of child entities.
 * The REST controller should only trigger the markAsDeletedAndSendEvent method to mark the project
 * for deletion and to send an event to the delete topic. A listener will pickup the message and
 * call the delete method to actually delete the entity with all nested objects.
 */
@Deprecated("Use V2 when migrating aggregates to arch 2.0")
interface AsynchronousDeleteService {

  /**
   * This operations marks an entity as deleted. This flag can be used to filter the entity in
   * queries so it is no longer available.
   *
   * @param resourceIdentifier The UUID identifying the entity to be deleted
   */
  fun markAsDeletedAndSendEvent(resourceIdentifier: UUID)

  /**
   * This operations only marks an entity as deleted. It is usually called from an event listener as
   * a safety net in case a delete message was previously sent without marking the entity to be
   * deleted.
   *
   * @param entityId The technical id of the entity to be deleted
   */
  @DenyWebRequests fun markAsDeleted(entityId: Long)
}
