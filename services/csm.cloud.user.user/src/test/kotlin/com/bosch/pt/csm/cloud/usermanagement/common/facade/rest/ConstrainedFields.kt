/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.common.facade.rest

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

  private val constraintDescriptions: ConstraintDescriptions = ConstraintDescriptions(input)

  /**
   * Creator for field.
   *
   * @param path field path
   * @return the field descriptor
   */
  fun withPath(path: String): FieldDescriptor =
      fieldWithPath(path)
          .attributes(
              key("constraints")
                  .value(
                      collectionToDelimitedString(
                          constraintDescriptions.descriptionsForProperty(path), ". ")))
}
