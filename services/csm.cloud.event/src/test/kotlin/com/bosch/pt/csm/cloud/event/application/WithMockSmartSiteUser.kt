/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2016
 *
 * *************************************************************************
 */
package com.bosch.pt.csm.cloud.event.application

import java.lang.annotation.Inherited
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import org.springframework.security.test.context.support.WithSecurityContext

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, CLASS)
@Retention(RUNTIME)
@Inherited
@MustBeDocumented
@WithSecurityContext(factory = WithMockSmartSiteUserSecurityContextFactory::class)
annotation class WithMockSmartSiteUser(

    /**
     * Convenience mechanism for specifying the user identifier. The default is "". If
     * [ ][.identifier] is specified it will be used instead of [.value]
     *
     * @return identifier
     */
    val value: String = "",

    /**
     * The UUID to be used.
     *
     * @return the UUID as string
     */
    val identifier: String = ""
)
