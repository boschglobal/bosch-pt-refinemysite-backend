/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.test

import org.springframework.restdocs.constraints.ConstraintDescriptions
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.snippet.Attributes.key

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
    fun withPath(path: String): FieldDescriptor {
        return fieldWithPath(path)
            .attributes(
                key("constraints")
                    .value(
                        org.springframework.util.StringUtils.collectionToDelimitedString(
                            this.constraintDescriptions.descriptionsForProperty(path), ". "
                        )
                    )
            )
    }
}
