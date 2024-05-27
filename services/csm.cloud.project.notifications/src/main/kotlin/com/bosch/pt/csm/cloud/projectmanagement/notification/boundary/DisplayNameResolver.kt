/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.boundary

import com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.resolver.DisplayNameResolverStrategy
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.ObjectReferenceWithContextRoot
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.util.HashMap
import javax.annotation.PostConstruct

@Service
class DisplayNameResolver(private val applicationContext: ApplicationContext) {

    private val resolvers = HashMap<String, DisplayNameResolverStrategy>()

    @PostConstruct
    internal fun wireStrategies() {
        val strategies = applicationContext.getBeansOfType(DisplayNameResolverStrategy::class.java)
        strategies.forEach { (_, value) -> resolvers[value.type] = value }
    }

    fun resolve(objectReference: ObjectReferenceWithContextRoot): String {
        val strategy = resolvers[objectReference.type] ?: throw IllegalStateException(
            "No display name resolver strategy found for type " + objectReference.type
        )
        return strategy.getDisplayName(objectReference)
    }
}
