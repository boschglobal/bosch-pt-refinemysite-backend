/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.common

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.test.tester.HttpGraphQlTester

abstract class AbstractGraphQlApiIntegrationTest : AbstractIntegrationTest() {

  @Autowired lateinit var graphQlTester: HttpGraphQlTester
}
