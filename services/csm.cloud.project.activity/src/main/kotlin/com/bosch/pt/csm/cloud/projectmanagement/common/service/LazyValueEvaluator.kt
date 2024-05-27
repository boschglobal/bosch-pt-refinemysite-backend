/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.service

import com.bosch.pt.csm.cloud.projectmanagement.activity.model.LazyValue
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType
import com.bosch.pt.csm.cloud.projectmanagement.common.service.resolver.LazyValueEvaluator
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
        evaluators[AggregateType.valueOf(value.type).name] = value
      }

  fun evaluate(projectIdentifier: UUID, lazyValue: LazyValue): String {
    val strategy =
        evaluators[lazyValue.type]
            ?: throw IllegalStateException(
                "No lazy value evaluator found for type ${lazyValue.type}")
    return strategy.evaluate(projectIdentifier, lazyValue)
  }
}
