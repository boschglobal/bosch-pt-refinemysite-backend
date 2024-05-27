/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command.snapshotstore

import com.bosch.pt.csm.cloud.common.eventstore.EventSourceEnum
import org.springframework.dao.OptimisticLockingFailureException

object EventVersionValidator {

  /**
   * This method determines, if a given event can be applied to given snapshot of an aggregate. It
   * compares the version of the referenced aggregate in the given event with the version of the
   * aggregate in the given snapshot. Depending on the source of the event (if received from a
   * command handler or from the kafka event stream (when restoring the store) the evaluation is
   * different.
   *
   * If the event is received as the result from a command handler, the logic can determine
   * concurrent access to an aggregate and block an event from being applied to the snapshot. This
   * must happen in a transaction to roll back the event. In this case, an exception is thrown to
   * abort a running transaction.
   *
   * If the event is received from the kafka event stream, the logic determines if an event is a
   * duplicate and therefore has to be skipped. In this case, the method returns false.
   *
   * @param aggregateVersionFromSnapshot: Version of the current snapshot of an aggregate. Can be
   *   null if no snapshot exists yet.
   * @param aggregateVersionFromEvent: Version of the referenced aggregate of an event.
   */
  fun canApply(
      aggregateVersionFromSnapshot: Long?,
      aggregateVersionFromEvent: Long,
      source: EventSourceEnum
  ) =
      if (aggregateVersionFromSnapshot == null) {
        if (aggregateVersionFromEvent == 0L) {
          true
        } else {
          error("Aggregate version in event is larger than 0 but no snapshot exist yet")
        }
      } else if (aggregateVersionFromEvent == aggregateVersionFromSnapshot.plus(1)) {
        true
      } else if (aggregateVersionFromEvent < aggregateVersionFromSnapshot.plus(1)) {
        if (source == EventSourceEnum.RESTORE) {
          false
        } else {
          throw OptimisticLockingFailureException(
              "Aggregate version in snapshot is higher than expected. Concurrent access possible")
        }
      } else if (aggregateVersionFromEvent > aggregateVersionFromSnapshot.plus(1)) {
        error("Aggregate version in snapshot is lower than expected.")
      } else {
        error("Unforeseen combination of aggregate version in snapshot and event")
      }
}
