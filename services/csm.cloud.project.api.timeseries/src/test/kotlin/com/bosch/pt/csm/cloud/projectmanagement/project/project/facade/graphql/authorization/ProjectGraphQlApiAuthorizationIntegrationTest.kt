/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.graphql.authorization

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractGraphQlApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.graphql.execution.ErrorType.FORBIDDEN

@RmsSpringBootTest
class ProjectGraphQlApiAuthorizationIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  val query =
      """
      query {
        projects {
          id
        }
      }
      """
          .trimIndent()

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitProject().submitCsmParticipant()
  }

  @Test
  fun `verify that an authorized user is allowed to read projects`() {
    setAuthentication("csm-user")
    val response = graphQlTester.document(query).execute()
    response.get("projects[0].id").isEqualTo(getIdentifier("project").toString())
  }

  @Test
  fun `verify that an unauthorized user is not allowed to read projects`() {
    eventStreamGenerator.submitUser("user2")
    setAuthentication("user2")
    graphQlTester
        .document(query)
        .execute()
        .errors()
        .expect { it.errorType == FORBIDDEN }
        .expect { it.message == "Forbidden" }
        .expect { it.path == "projects" }
  }
}
