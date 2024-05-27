/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.test

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.springframework.graphql.test.tester.GraphQlTester

fun <T> GraphQlTester.Response.getList(
    path: String,
    listType: Class<T>
): GraphQlTester.EntityList<T> = this.path(path).entityList(listType)

fun <T> GraphQlTester.EntityList<T>.single(): ObjectAssert<T> = assertThat(this.get().single())

fun GraphQlTester.Response.get(path: String): GraphQlTester.Entity<Any, *> =
    this.path(path).entity(Any::class.java)

fun GraphQlTester.Response.isNull(path: String): GraphQlTester.Path =
    this.path(path).pathDoesNotExist()

fun GraphQlTester.Response.isNotNull(path: String): GraphQlTester.Entity<Any, *> = this.get(path)
