/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.eventstore

/** This enumeration defines sources for events. */
enum class EventSourceEnum {
  /** used when events are coming from a command handler */
  ONLINE,

  /** used when events are coming from kafka while replaying events */
  RESTORE
}
