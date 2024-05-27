/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.facade.rest

import org.springframework.restdocs.constraints.ConstraintDescriptions
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.snippet.Attributes.key
import org.springframework.util.StringUtils.collectionToDelimitedString

/**
 * Spring Rest Docs fields extension to include constraints for documentation snippets of request
 * fields.
 */
class ConstrainedFields(input: Class<*>) {

  private val constraintDescriptions: ConstraintDescriptions

  init {
    constraintDescriptions = ConstraintDescriptions(input)
  }

  fun withPath(path: String): FieldDescriptor =
      fieldWithPath(path)
          .attributes(
              key("constraints")
                  .value(
                      collectionToDelimitedString(
                          constraintDescriptions.descriptionsForProperty(
                              path.replace("[\\[\\]]".toRegex(), "")),
                          ". ")))

  fun withPath(prefix: String, path: String): FieldDescriptor =
      if (prefix.isNotBlank()) withPath("$prefix.$path") else withPath(path)
}
