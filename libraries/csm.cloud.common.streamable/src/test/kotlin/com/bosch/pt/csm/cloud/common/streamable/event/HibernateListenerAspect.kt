/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.streamable.event

@Deprecated("not used in Architecture 2.0 anymore")
interface HibernateListenerAspect {
  fun enableListeners(enabled: Boolean)
}
