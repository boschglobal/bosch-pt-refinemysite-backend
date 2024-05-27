/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.service

import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType
import com.bosch.pt.csm.cloud.projectmanagement.common.service.resolver.DisplayNameResolver
import javax.annotation.PostConstruct
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

@Service
class DisplayNameResolver(private val applicationContext: ApplicationContext) {

  private val resolvers = mutableMapOf<String, DisplayNameResolver>()

  @PostConstruct
  internal fun wireStrategies() =
      applicationContext.getBeansOfType(DisplayNameResolver::class.java).forEach { (_, value) ->
        resolvers[AggregateType.valueOf(value.type).type] = value
      }

  fun resolve(objectReference: UnresolvedObjectReference): String {
    val strategy =
        resolvers[objectReference.type]
            ?: throw IllegalStateException(
                "No display name resolver strategy found for type ${objectReference.type}")
    return strategy.getDisplayName(objectReference)
  }
}
