/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.user.query

import com.bosch.pt.csm.cloud.job.job.api.UserIdentifier
import com.bosch.pt.csm.cloud.job.user.query.security.JobServiceUserDetails
import com.bosch.pt.csm.cloud.job.user.query.security.JobServiceUserDetailsService
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Import
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("local", "test")
@DataMongoTest
@Import(UserProjectionIntegrationTestConfiguration::class)
class UserProjectionIntegrationTest {

  @Autowired lateinit var userProjector: UserProjector

  @Autowired lateinit var jobServiceUserDetailsService: JobServiceUserDetailsService

  val userIdentifier = UserIdentifier("UserIdentifier of Daniel Kramer")
  val externalUserIdentifier = ExternalUserIdentifier("ciamId of Daniel Kramer")

  @Test
  fun `throws for unknown User`() {
    assertThatExceptionOfType(UsernameNotFoundException::class.java).isThrownBy {
      jobServiceUserDetailsService.loadUserByUsername("ciamId of unknown User")
    }
  }

  @Test
  fun `provides UserDetails for known User`() {
    given(UserChangedEvent(userIdentifier, externalUserIdentifier, Locale.GERMAN))

    assertThat(jobServiceUserDetailsService.loadUserByUsername(externalUserIdentifier.value))
        .isEqualTo(JobServiceUserDetails(userIdentifier, Locale.GERMAN))
  }

  @Test
  fun `throws for deleted User`() {
    given(
        UserChangedEvent(userIdentifier, externalUserIdentifier, Locale.GERMAN),
        UserDeletedEvent(userIdentifier))

    assertThatExceptionOfType(UsernameNotFoundException::class.java).isThrownBy {
      jobServiceUserDetailsService.loadUserByUsername(externalUserIdentifier.value)
    }
  }

  private fun given(vararg events: UserEvent) {
    events.forEach { userProjector.handle(it) }
  }
}

@Import(UserProjector::class, JobServiceUserDetailsService::class)
private class UserProjectionIntegrationTestConfiguration
