/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.common.exceptions

import org.opentest4j.AssertionFailedError

class WrongNumberOfEventsException : AssertionFailedError {

  constructor(
      eventType: String,
      eventName: String,
      expectedNumber: Int,
      actualNumber: Int
  ) : super(
      "EventType $eventType with name $eventName should be published $expectedNumber time(s), " +
          "but was published $actualNumber time(s)",
      expectedNumber,
      actualNumber)

  constructor(
      expectedNumber: Int,
      actualNumber: Int
  ) : super(
      "Expected number of event was $expectedNumber but is $actualNumber",
      expectedNumber,
      actualNumber)
}
