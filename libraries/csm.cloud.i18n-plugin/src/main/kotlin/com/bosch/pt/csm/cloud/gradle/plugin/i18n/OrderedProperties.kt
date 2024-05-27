/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.gradle.plugin.i18n

import java.util.Properties
import java.util.function.BiConsumer
import kotlin.collections.LinkedHashSet

open class OrderedProperties : Properties() {

  private val keyOrder = LinkedHashSet<Any>()

  @Synchronized
  override fun put(key: Any, value: Any?): Any? {
    keyOrder.add(key)
    return super.put(key, value)
  }

  override fun forEach(consumer: BiConsumer<in Any, in Any?>) {
    keyOrder.forEach { consumer.accept(it, get(it)) }
  }

  @Synchronized
  override fun equals(other: Any?): Boolean = super.equals(other)

  @Synchronized
  override fun hashCode(): Int = super.hashCode()
}