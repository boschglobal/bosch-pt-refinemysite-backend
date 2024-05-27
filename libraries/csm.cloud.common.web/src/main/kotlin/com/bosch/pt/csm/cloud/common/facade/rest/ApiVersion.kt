/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.facade.rest

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, ANNOTATION_CLASS, CLASS)
@Retention(RUNTIME)
annotation class ApiVersion(

    /**
     * The version of the API from which this endpoint is supported on. Default is 0, meaning it is
     * supported from the current min version of the API.
     *
     * @return the from-value
     */
    val from: Int = 0,

    /**
     * The version of the API until this endpoint is supported. Default is 0, meaning it is
     * supported until the max version of the API. The number is inclusive, e.g.
     *
     * Endpoint A Version 1 has from: 1, to : 1
     *
     * Endpoint A Version 2 has from: 2
     *
     * @return the to-value
     */
    val to: Int = 0
)
