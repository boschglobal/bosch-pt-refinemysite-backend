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
 * This interface defines methods (s a kind of protocol) to deletes multiple entities with all
 * nested objects asynchronously. This should be used to delete entities with potentially lots of
 * child entities. The REST controller should only trigger the markAsDeletedAndSendEvents method to
 * mark the tasks for deletion and to sends an event to the delete daycards. A listener will pickup
 * the message and call the delete method to actually delete entities with all nested objects.
 */
interface AsynchronousBatchDeleteService<ID : UuidIdentifiable> {

  /**
   * This operations marks aggregates as deleted. This flag can be used to filter aggregates in
   * queries so they are no longer available.
   *
   * @param identifiers The List<ID> identifying the aggregates to be deleted
   */
  fun markAsDeletedAndSendEvents(identifiers: List<ID>)

  /**
   * This operations marks multiple aggregates as deleted. It is usually called from an event
   * listener as a safety net in case delete messages were previously sent without marking entities
   * to be deleted.
   *
   * @param identifiers The <ID> identifying aggregates to be deleted
   */
  @DenyWebRequests fun markAsDeleted(identifiers: List<ID>)
}
