/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.streamable.event

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator

@Deprecated("not used in Architecture 2.0 anymore")
fun EventStreamGenerator.runWithDisabledHibernateListeners(
    block: (EventStreamGenerator) -> EventStreamGenerator
): EventStreamGenerator {
  val context = getContext() as HibernateEventStreamContext
  context.hibernateListenerAspect.enableListeners(false)
  val generator = block.invoke(this)
  context.hibernateListenerAspect.enableListeners(true)
  return generator
}
