/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Objects
import java.util.function.Supplier
import org.springframework.hateoas.Links
import org.springframework.hateoas.RepresentationModel

/** Represents a resource on the wire, with potential embedded resources. */
abstract class AbstractResource(
    @field:JsonProperty("_embedded")
    @field:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val embeddedResources: HashMap<String, RepresentationModel<AbstractResource>> = HashMap()
) : RepresentationModel<AbstractResource>() {

  private val embeddedResourcesProducers:
      HashMap<String, Supplier<out RepresentationModel<AbstractResource>>> =
      HashMap()

  /** Links property is always added (as "_links"), even if there are no links. */
  @JsonProperty("_links")
  @JsonInclude(JsonInclude.Include.ALWAYS)
  override fun getLinks(): Links {
    return super.getLinks()
  }

  /**
   * Embed a resource using the given name. If a resource with this name already exists, it will be
   * replaced.
   *
   * @param name name to use for the embedded resource
   * @param resource resource to embed
   */
  @ExcludeFromCodeCoverage
  fun embed(name: String, resource: RepresentationModel<AbstractResource>?) {
    if (resource != null) {
      embeddedResources[name] = resource
    }
  }

  /**
   * Embed a resource provided by a supplier function.
   *
   * @param name name
   */
  @ExcludeFromCodeCoverage
  fun embed(name: String) {
    check(embeddedResourcesProducers.containsKey(name)) {
      "Requested resource $name but there is no resource supplier."
    }
    embed(name, embeddedResourcesProducers[name]!!.get())
  }

  /**
   * Add a resource supplier which will be called in case the resource with the specified name is
   * asked to be embedded. The resource supplier is any function that takes no args and returns at
   * least a [RepresentationModel]. The supplier may return null in which case the embedded resource
   * will not be added. Exceptions are passed through.
   *
   * @param name name of the provided embedded resource
   * @param resourceSupplier the resource supplier
   */
  fun addResourceSupplier(
      name: String,
      resourceSupplier: Supplier<out RepresentationModel<AbstractResource>>
  ) {
    embeddedResourcesProducers[name] = resourceSupplier
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
