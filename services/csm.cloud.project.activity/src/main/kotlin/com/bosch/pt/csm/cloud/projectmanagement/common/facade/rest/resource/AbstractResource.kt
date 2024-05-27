/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.facade.rest.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Objects
import java.util.function.Supplier
import org.springframework.hateoas.Links
import org.springframework.hateoas.RepresentationModel

/** Represents a resource on the wire, with potential embedded resources. */
abstract class AbstractResource : RepresentationModel<AbstractResource>() {
  private val embeddedResources: HashMap<String, RepresentationModel<AbstractResource>> = HashMap()
  private val embeddedResourcesProducers:
      HashMap<String, Supplier<out RepresentationModel<AbstractResource>>> =
      HashMap()

  /**
   * Embed a resource using the given name. If a resource with this name already exists, it will be
   * replaced.
   *
   * @param name name to use for the embedded resource
   * @param resource resource to embed
   */
  fun embed(name: String, resource: RepresentationModel<AbstractResource>?) {
    resource?.let { embeddedResources[name] = it }
  }

  /**
   * Returns embedded resources.
   *
   * @return map of embedded resources.
   */
  @JsonProperty("_embedded")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  fun getEmbeddedResources(): Map<String, RepresentationModel<AbstractResource>> {
    return embeddedResources
  }

  @JsonProperty("_links")
  @JsonInclude(JsonInclude.Include.ALWAYS)
  override fun getLinks(): Links {
    return super.getLinks()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    if (!super.equals(other)) {
      return false
    }
    val that = other as AbstractResource
    return embeddedResources == that.embeddedResources &&
        embeddedResourcesProducers == that.embeddedResourcesProducers
  }

  override fun hashCode() =
      Objects.hash(super.hashCode(), embeddedResources, embeddedResourcesProducers)
}
