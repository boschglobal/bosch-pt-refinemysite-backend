/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.kafka

import java.security.Security.setProperty

class JvmDnsCachingSettings {

  /**
   * Recommended Confluent JVM settings
   *
   * @see [Confluent]
   * (https://docs.confluent.io/cloud/current/faq.html#what-are-the-recommended-jvm-settings)
   * @see [Oracle](https://docs.oracle.com/javase/7/docs/technotes/guides/net/properties.html)
   */
  fun setDnsCachingSettings() {
    setProperty("networkaddress.cache.ttl", CACHING_TIME_LIMIT_IN_SECONDS)
    setProperty("networkaddress.cache.negative.ttl", NEVER_CACHE)
  }

  companion object {
    private const val CACHING_TIME_LIMIT_IN_SECONDS = "30"
    private const val NEVER_CACHE = "0"
  }
}
