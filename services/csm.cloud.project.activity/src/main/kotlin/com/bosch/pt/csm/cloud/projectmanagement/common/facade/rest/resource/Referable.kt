/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.facade.rest.resource

import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI
import java.util.UUID

interface Referable {
  val identifier: UUID
  val displayName: String
}

data class ResourceReference(
    @get:JsonProperty("id") override val identifier: UUID,
    override val displayName: String
) : Referable

data class ResourceReferenceWithPicture(
    @get:JsonProperty("id") override val identifier: UUID,
    override val displayName: String,
    val picture: URI
) : Referable
