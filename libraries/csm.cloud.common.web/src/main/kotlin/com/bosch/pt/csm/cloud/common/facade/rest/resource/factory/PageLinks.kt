/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.facade.rest.resource.factory

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER

/**
 * This annotation can be applied to build methods of factories for paged resources. The return type
 * of the method must extend [org.springframework.hateoas.RepresentationModel] and the first
 * parameter of the method must be a [org.springframework.data.domain.Page]. The annotation will
 * cause the method call to be intercepted. The original build method will be called in the same
 * manner with identical arguments. The resource which the annotated method returns will be extended
 * by page links.
 */
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER) @Retention(RUNTIME) annotation class PageLinks
