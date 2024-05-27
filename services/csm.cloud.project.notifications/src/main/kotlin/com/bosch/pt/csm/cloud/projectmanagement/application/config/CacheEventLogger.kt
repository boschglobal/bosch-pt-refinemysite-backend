/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.facade.listener.strategies.notifications.DayCardStatusChangeNotificationStrategy
import org.ehcache.event.CacheEvent
import org.ehcache.event.CacheEventListener
import org.slf4j.LoggerFactory

class CacheEventLogger : CacheEventListener<Any, Any> {

    override fun onEvent(
        cacheEvent: CacheEvent<out Any, out Any>
    ) {
        LOGGER.debug(
            "Cache event type = {},  Old value = {}, New value = {}",
            cacheEvent.type, cacheEvent.oldValue, cacheEvent.newValue
        )
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DayCardStatusChangeNotificationStrategy::class.java)
    }
}
