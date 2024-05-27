/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.application.security

import com.bosch.pt.csm.cloud.common.LibraryCandidate
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER

/**
 * Annotation that can be used for method-level security, whenever no security is explicitly wished
 * This is useful to differentiate between intended unauthorized access and simply forgotten
 * security.
 */
@LibraryCandidate
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
@Retention(RUNTIME)
annotation class NoPreAuthorize(val usedByController: Boolean = false)
