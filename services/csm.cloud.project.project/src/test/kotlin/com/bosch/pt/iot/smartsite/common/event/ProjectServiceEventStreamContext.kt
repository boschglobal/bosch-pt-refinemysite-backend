/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.event

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.streamable.event.HibernateEventStreamContext
import com.bosch.pt.csm.cloud.common.streamable.event.HibernateListenerAspect
import com.bosch.pt.csm.cloud.common.test.KafkaListenerFunction
import com.bosch.pt.csm.cloud.common.test.TimeLineGenerator
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.simulateKafkaListener
import org.apache.avro.specific.SpecificRecordBase
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component

open class ProjectServiceEventStreamContext(
    events: MutableMap<String, SpecificRecordBase>,
    lastIdentifierPerType: MutableMap<String, AggregateIdentifierAvro>,
    timeLineGenerator: TimeLineGenerator,
    private val onlineListener: MutableMap<String, List<KafkaListenerFunction>> = mutableMapOf(),
    private val restoreListener: MutableMap<String, List<KafkaListenerFunction>> = mutableMapOf(),
    hibernateListenerAspect: HibernateListenerAspect
) :
    HibernateEventStreamContext(
        events, lastIdentifierPerType, timeLineGenerator, mutableMapOf(), hibernateListenerAspect) {

  override fun send(runnable: Runnable) {
    simulateKafkaListener { runnable.run() }
  }

  fun useOnlineListener(): ProjectServiceEventStreamContext {
    this.listeners.apply {
      clear()
      putAll(onlineListener)
    }
    return this
  }

  fun useRestoreListener(): ProjectServiceEventStreamContext {
    this.listeners.apply {
      clear()
      putAll(restoreListener)
    }
    return this
  }
}

@Aspect
@Component
open class ProjectServiceHibernateListenerAspect : HibernateListenerAspect {

  private var listenersEnabled = true

  override fun enableListeners(enabled: Boolean) {
    listenersEnabled = enabled
  }

  @Around("execution(* org.springframework.data.auditing.AuditingHandlerSupport+.mark*(..))")
  fun disableAuditing(joinPoint: ProceedingJoinPoint): Any? =
      when {
        listenersEnabled -> joinPoint.proceed()
        else -> joinPoint.args[0]
      }

  @Around(
      "execution(* com.bosch.pt.iot.smartsite.common.kafka.eventlistener.HibernateInsertEventListener.*(..))")
  fun disableInsertListener(joinPoint: ProceedingJoinPoint) = conditionalProceed(joinPoint)

  @Around(
      "execution(* com.bosch.pt.iot.smartsite.common.kafka.eventlistener.HibernateUpdateEventListener.*(..))")
  fun disableUpdateListener(joinPoint: ProceedingJoinPoint) = conditionalProceed(joinPoint)

  @Around(
      "execution(* com.bosch.pt.iot.smartsite.common.kafka.eventlistener.HibernateDeleteEventListener.*(..))")
  fun disableDeleteListener(joinPoint: ProceedingJoinPoint) = conditionalProceed(joinPoint)

  fun conditionalProceed(joinPoint: ProceedingJoinPoint): Any? {
    if (listenersEnabled) {
      joinPoint.proceed()
    }
    val methodSignature = joinPoint.signature as MethodSignature
    return when (methodSignature.returnType) {
      Boolean::class.java -> false
      else -> Unit
    }
  }
}
