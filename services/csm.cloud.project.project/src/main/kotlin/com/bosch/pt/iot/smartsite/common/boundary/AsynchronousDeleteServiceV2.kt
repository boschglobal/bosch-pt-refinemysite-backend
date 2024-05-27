/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.boundary

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.common.command.DenyWebRequests

/**
 * This interface defines methods (s a kind of protocol) to delete an entity with all nested objects
 * asynchronously. This should be used to delete entities with potentially lots of child entities.
 * The REST controller should only trigger the markAsDeletedAndSendEvent method to mark the project
 * for deletion and to send an event to the delete topic. A listener will pickup the message and
 * call the delete method to actually delete the entity with all nested objects.
 */
interface AsynchronousDeleteServiceV2<ID : UuidIdentifiable> {

  /**
   * This operations marks an aggregate as deleted. This flag can be used to filter the aggregate in
   * queries so it is no longer available.
   *
   * @param identifier The <ID> identifying the aggregate to be deleted
   */
  fun markAsDeletedAndSendEvent(identifier: ID)

  /**
   * This operations only marks an aggregate as deleted. It is usually called from an event listener
   * as a safety net in case a delete message was previously sent without marking the entity to be
   * deleted.
   *
   * @param identifier The <ID> identifying the aggregate to be deleted
   */
  @DenyWebRequests fun markAsDeleted(identifier: ID)
}
