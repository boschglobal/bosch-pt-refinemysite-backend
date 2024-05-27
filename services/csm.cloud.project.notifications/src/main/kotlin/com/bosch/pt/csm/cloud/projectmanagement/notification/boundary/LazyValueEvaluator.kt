/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.boundary

import com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.resolver.LazyValueEvaluator
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.LazyValue
import java.util.UUID
import javax.annotation.PostConstruct
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

@Service
class LazyValueEvaluator(private val applicationContext: ApplicationContext) {

  private val evaluators = mutableMapOf<String, LazyValueEvaluator>()

  @PostConstruct
  internal fun wireStrategies() =
      applicationContext.getBeansOfType(LazyValueEvaluator::class.java).forEach { (_, value) ->
        evaluators[value.type] = value
      }

  fun evaluate(projectIdentifier: UUID, lazyValue: LazyValue): String {
    val strategy =
        evaluators[lazyValue.type]
            ?: throw IllegalStateException(
                "No lazy value evaluator found for type ${lazyValue.type}")
    return strategy.evaluate(projectIdentifier, lazyValue)
  }
}
