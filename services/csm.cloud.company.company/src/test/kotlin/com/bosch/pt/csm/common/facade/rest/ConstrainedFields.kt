/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.facade.rest

import org.springframework.restdocs.constraints.ConstraintDescriptions
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.PayloadDocumentation
import org.springframework.restdocs.snippet.Attributes
import org.springframework.util.StringUtils

/**
 * Spring Rest Docs fields extension to include constraints for documentation snippets of request
 * fields.
 */
class ConstrainedFields
/**
 * Constructor.
 *
 * @param input input class containing constraints
 */
constructor(input: Class<*>) {

  private val constraintDescriptions: ConstraintDescriptions

  init {
    constraintDescriptions = ConstraintDescriptions(input)
  }

  /**
   * Creator for field.
   *
   * @param path field path
   * @return the field descriptor
   */
  fun withPath(path: String): FieldDescriptor =
      PayloadDocumentation.fieldWithPath(path)
          .attributes(
              Attributes.key("constraints")
                  .value(
                      StringUtils.collectionToDelimitedString(
                          constraintDescriptions.descriptionsForProperty(path), ". ")))
}
