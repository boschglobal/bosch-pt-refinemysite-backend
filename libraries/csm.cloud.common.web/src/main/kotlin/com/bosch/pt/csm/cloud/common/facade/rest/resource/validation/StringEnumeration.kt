/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.facade.rest.resource.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/** Bean validator for enumerations. */
@MustBeDocumented
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FIELD,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [StringEnumerationValidator::class])
annotation class StringEnumeration(

    /**
     * Default validation violation message.
     *
     * @return the message
     */
    val message: String =
        "{com.bosch.pt.iot.smartsite.common.validation.StringEnumeration.message}",

    /**
     * Validation groups.
     *
     * @return groups
     */
    val groups: Array<KClass<*>> = [],

    /**
     * Validation payload.
     *
     * @return the payload
     */
    val payload: Array<KClass<out Payload>> = [],

    /**
     * The target enumeration class.
     *
     * @return enum class
     */
    val enumClass: KClass<out Enum<*>>,

    /**
     * Enumeration values (for documentation purposes).
     *
     * @return the enum values
     */
    val enumValues: String = ""
)
