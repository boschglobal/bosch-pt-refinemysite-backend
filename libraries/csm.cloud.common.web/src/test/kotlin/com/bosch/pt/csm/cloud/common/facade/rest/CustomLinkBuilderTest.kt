/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.facade.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.hateoas.IanaLinkRelations.SELF

internal class CustomLinkBuilderTest {

  @Test
  fun `verify link built only from link prefix`() {
    val prefix = "https://example.org/api"
    val link = CustomLinkBuilder(prefix).withSelfRel()

    assertThat(link.toUri().toString()).isEqualTo(prefix)
    assertThat(link.rel).isEqualTo(SELF)
  }

  @Test
  fun `verify calling slash with empty path does not affect the link`() {
    val prefix = "https://example.org/api"
    val link = CustomLinkBuilder(prefix).slash("").withSelfRel()

    assertThat(link.toUri().toString()).isEqualTo(prefix)
    assertThat(link.rel).isEqualTo(SELF)
  }

  @Test
  fun `verify calling slash with null path does not affect the link`() {
    val prefix = "https://example.org/api"
    val link = CustomLinkBuilder(prefix).slash(null).withSelfRel()

    assertThat(link.toUri().toString()).isEqualTo(prefix)
    assertThat(link.rel).isEqualTo(SELF)
  }

  @Test
  fun `verify calling slash will remove redundant slashes`() {
    val prefix = "https://example.org/api/"
    val link = CustomLinkBuilder(prefix).slash("suffix").withSelfRel()

    assertThat(link.toUri().toString()).isEqualTo("https://example.org/api/suffix")
    assertThat(link.rel).isEqualTo(SELF)
  }

  @Test
  fun `verify calling slash will remove redundant slashes when path starts with slash`() {
    val prefix = "https://example.org/api/"
    val link = CustomLinkBuilder(prefix).slash("/suffix").withSelfRel()

    assertThat(link.toUri().toString()).isEqualTo("https://example.org/api/suffix")
    assertThat(link.rel).isEqualTo(SELF)
  }

  @Test
  fun `verify parameter expansion for parameter in prefix and path`() {
    val prefix = "https://example.org/api/{param1}"
    val link =
        CustomLinkBuilder(prefix)
            .slash("{param2}")
            .withParameters(mapOf("param1" to "expanded1", "param2" to "expanded2"))
            .withSelfRel()

    assertThat(link.toUri().toString()).isEqualTo("https://example.org/api/expanded1/expanded2")
    assertThat(link.rel).isEqualTo(SELF)
  }

  @Test
  fun `verify parameter expansion is applied when invoking toUri method`() {
    val prefix = "https://example.org/api/{param1}"
    val linkBuilder =
        CustomLinkBuilder(prefix)
            .slash("{param2}")
            .withParameters(mapOf("param1" to "expanded1", "param2" to "expanded2"))

    assertThat(linkBuilder.toUri().toString())
        .isEqualTo("https://example.org/api/expanded1/expanded2")
  }

  @Test
  fun `verify invoking withParameters multiple times throws exception`() {
    val thrown = assertThrows(IllegalArgumentException::class.java) {
      CustomLinkBuilder("https://example.org/api/{param1}")
          .withParameters(mapOf("param1" to "expanded1"))
          .withParameters(mapOf("param1" to "expanded1"))
          .withSelfRel()
    }
    assertEquals("This method must not be invoked multiple times on the same builder.", thrown.message)
  }

  @Test
  fun `verify invoking withQueryParameters multiple times throws exception`() {
    assertThrows(java.lang.IllegalArgumentException::class.java) {
      CustomLinkBuilder("https://example.org/api/{param1}")
          .withQueryParameters(mapOf("param1" to "expanded1"))
          .withQueryParameters(mapOf("param1" to "expanded1"))
          .withSelfRel()
    }
  }

  @Test
  fun `verify multiple query parameters are appended correctly`() {
    val prefix = "https://example.org/api"
    val link =
        CustomLinkBuilder(prefix)
            .withQueryParameters(
                mapOf("param1" to "value1", "param2" to "value2", "param3" to "value3"))
            .withSelfRel()

    assertThat(link.toUri().toString())
        .isEqualTo("https://example.org/api?param1=value1&param2=value2&param3=value3")
    assertThat(link.rel).isEqualTo(SELF)
  }

  @Test
  fun `verify one query parameter is appended correctly`() {
    val prefix = "https://example.org/api"
    val link =
        CustomLinkBuilder(prefix).withQueryParameters(mapOf("param1" to "value1")).withSelfRel()

    assertThat(link.toUri().toString()).isEqualTo("https://example.org/api?param1=value1")
    assertThat(link.rel).isEqualTo(SELF)
  }

  @Test
  fun `verify query parameters and parameters are handled correctly`() {
    val prefix = "https://example.org/api/{pathParam1}/{pathParam2}"
    val link =
        CustomLinkBuilder(prefix)
            .withParameters(mapOf("pathParam1" to "pathVal1", "pathParam2" to "pathVal2"))
            .withQueryParameters(mapOf("param1" to "value1"))
            .withSelfRel()

    assertThat(link.toUri().toString())
        .isEqualTo("https://example.org/api/pathVal1/pathVal2?param1=value1")
    assertThat(link.rel).isEqualTo(SELF)
  }

  @Test
  fun `verify pageable query parameters are expanded correctly`() {

    val prefix = "https://example.org/api"
    val link =
        CustomLinkBuilder(prefix)
            .withQueryParameters(
                mapOf("param1" to "value1", "param2" to Pageable.ofSize(3), "param3" to "value3"))
            .withSelfRel()

    assertThat(link.href)
        .isEqualTo("https://example.org/api?param1=value1&size=3&page=0&param3=value3")
    assertThat(link.rel).isEqualTo(SELF)
  }

  @Test
  fun `verify unpaged pageable query parameters are handled correctly`() {

    val prefix = "https://example.org/api"
    val link =
        CustomLinkBuilder(prefix)
            .withQueryParameters(
                mapOf(
                    "param1" to "value1", "param3" to "value3", "myPageable" to Pageable.unpaged()))
            .withSelfRel()

    assertThat(link.href).isEqualTo("https://example.org/api?param1=value1&param3=value3")
    assertThat(link.rel).isEqualTo(SELF)
  }

  @Test
  fun `verify pageable query parameters with sort are expanded correctly`() {
    val prefix = "https://example.org/api"
    val link =
        CustomLinkBuilder(prefix)
            .withQueryParameters(
                mapOf(
                    "param1" to "value1",
                    "param2" to PageRequest.of(3, 5, Sort.Direction.DESC, "sortProp1", "sortProp2"),
                    "param3" to "value3"))
            .withSelfRel()

    assertThat(link.href)
        .isEqualTo(
            "https://example.org/api?param1=value1&size=5&page=3&sort=sortProp1,sortProp2,desc&param3=value3")
    assertThat(link.rel).isEqualTo(SELF)
  }

  @Test
  fun `verify pageable query parameters with multiple sorts are expanded correctly`() {
    val prefix = "https://example.org/api"
    val link =
        CustomLinkBuilder(prefix)
            .withQueryParameters(
                mapOf(
                    "param1" to "value1",
                    "param2" to
                        PageRequest.of(
                            3,
                            5,
                            Sort.by(Sort.Order.asc("sortProp1"), Sort.Order.desc("sortProp2"))),
                    "param3" to "value3"))
            .withSelfRel()

    assertThat(link.href)
        .isEqualTo(
            "https://example.org/api?param1=value1&size=5&page=3&sort=sortProp1,asc&sort=sortProp2,desc&param3=value3")
    assertThat(link.rel).isEqualTo(SELF)
  }
}
