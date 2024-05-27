/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.eventstore.exception

import org.opentest4j.AssertionFailedError

class WrongNumberOfEventsException : AssertionFailedError {

  constructor(
      eventType: String,
      eventName: String,
      expectedNumber: Int,
      actualNumber: Int
  ) : super(
      "Event type $eventType with name $eventName should be published $expectedNumber time(s), " +
          "but was published $actualNumber time(s)",
      expectedNumber,
      actualNumber)

  constructor(
      eventType: String,
      expectedNumber: Int,
      actualNumber: Int
  ) : super(
      "Event type $eventType should be published $expectedNumber time(s), " +
          "but was published $actualNumber time(s)",
      expectedNumber,
      actualNumber)

  constructor(
      expectedNumber: Int,
      actualNumber: Int
  ) : super(
      "Expected $expectedNumber event(s) but found $actualNumber events.",
      expectedNumber,
      actualNumber)
}
