/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.facade.rest

import java.net.URI
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.hateoas.IanaLinkRelations.SELF
import org.springframework.hateoas.Link
import org.springframework.hateoas.LinkRelation
import org.springframework.hateoas.server.LinkBuilder

/**
 * This is a simple replacement for Spring's linkTo() method. Compared to Spring's implementation,
 * this class is optimized for performance, but sacrifies the type safety provided by Spring.
 */
class CustomLinkBuilder(linkPrefix: String) : LinkBuilder {

  private val sortSupport = CustomLinkBuilderSortSupport()

  private var parameters: Map<String, Any> = emptyMap()

  private var queryParameters: Map<String, Any> = emptyMap()

  private val stringBuilder: StringBuilder = StringBuilder()

  init {
    stringBuilder.append(linkPrefix.removeSuffix("/"))
  }

  override fun slash(path: Any?): CustomLinkBuilder {
    val pathString = path?.toString()
    if (pathString == null || pathString.isEmpty()) {
      return this
    }
    if (pathString.first() != '/') {
      stringBuilder.append("/")
    }
    stringBuilder.append(pathString.removeSuffix("/"))
    return this
  }

  override fun toUri() = URI.create(expandParameters(stringBuilder.toString()))

  override fun withRel(rel: LinkRelation) = Link.of(expandParameters(stringBuilder.toString()), rel)

  override fun withSelfRel() = withRel(SELF)

  fun withParameters(parameters: Map<String, Any>): CustomLinkBuilder {
    require(this.parameters.isEmpty()) { "This method must not be invoked multiple times on the same builder." }
    this.parameters = parameters
    return this
  }

  fun withQueryParameters(queryParameters: Map<String, Any>): CustomLinkBuilder {
    require(this.queryParameters.isEmpty()) { "This method must not be invoked multiple times on the same builder." }
    this.queryParameters = queryParameters
    return this
  }

  private fun expandParameters(path: String): String {
    var expandedPath = path
    for (param in parameters) {
      expandedPath = expandedPath.replace("{${param.key}}", param.value.toString())
    }
    return appendQueryParameters(expandedPath)
  }

  private fun MutableList<String>.addPageablePairs(pageable: Pageable) {

    if (pageable.isPaged) {
      // adds something like "size=3"
      this.add(PAGEABLE_SIZE + pageable.pageSize)
      // adds something like "page=2"
      this.add(PAGEABLE_PAGE + pageable.pageNumber)
    }
    // adds something like "sort=prop1,prop2,desc" for each sort order specified
    if (pageable.sort != Sort.unsorted()) {
      sortSupport.resolveSort(pageable.sort).forEach { sortValue ->
        this.add(PAGEABLE_SORT + sortValue)
      }
    }
  }
  private fun appendQueryParameters(path: String): String {
    val parameterPairs = mutableListOf<String>()
    for (queryParam in queryParameters) {
      if (queryParam.value is Pageable) {
        parameterPairs.addPageablePairs(queryParam.value as Pageable)
      } else {
        parameterPairs.add(queryParam.key + "=" + queryParam.value.toString())
      }
    }
    return if (parameterPairs.isEmpty()) {
      path
    } else {
      "$path?" + parameterPairs.joinToString("&")
    }
  }
  companion object {
    private const val PAGEABLE_SIZE = "size="
    private const val PAGEABLE_PAGE = "page="
    private const val PAGEABLE_SORT = "sort="
  }
}
