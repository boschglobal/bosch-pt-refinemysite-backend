/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.common.facade.rest

import org.springframework.core.MethodParameter
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.web.method.support.HandlerMethodArgumentResolver

fun <T : Annotation?> HandlerMethodArgumentResolver.findMethodAnnotation(
    annotationClass: Class<T>,
    parameter: MethodParameter
): T? =
    parameter.getParameterAnnotation(annotationClass)
        ?: run {
          for (parameterAnnotations in parameter.getParameterAnnotations()) {
            val annotation =
                AnnotationUtils.findAnnotation(
                    parameterAnnotations.annotationClass.java, annotationClass)
            if (annotation != null) {
              return annotation
            }
          }
          return null
        }
