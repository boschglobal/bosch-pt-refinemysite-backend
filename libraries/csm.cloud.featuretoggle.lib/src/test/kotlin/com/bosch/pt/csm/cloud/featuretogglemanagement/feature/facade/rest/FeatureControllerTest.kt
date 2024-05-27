package com.bosch.pt.csm.cloud.featuretogglemanagement.feature.facade.rest

import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.FeatureIdentifier
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureCreatedEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureDisabledEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.FeatureEnabledEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.events.UpstreamFeatureEvent
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.query.FeatureProjector
import com.bosch.pt.csm.cloud.testapp.TestApplication
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.security.Principal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

private const val ENDPOINT_PREFIX = "/bim"

@ActiveProfiles("local", "test", "kafka-feature-projector-listener-disabled")
@SpringBootTest(
    classes = [TestApplication::class, TestConfig::class],
    properties = ["custom.feature.endpoint.prefix=$ENDPOINT_PREFIX"])
@AutoConfigureMockMvc
internal class FeatureControllerTest {
  @Autowired lateinit var mockMvc: MockMvc
  @Autowired private lateinit var featureProjector: FeatureProjector
  @MockkBean lateinit var participantAuthorization: ParticipantAuthorization

  @BeforeEach
  fun setup() {
    TestSecurityContextHolder.setAuthentication(
        UsernamePasswordAuthenticationToken(
            Principal { "user1" }, "n/a", AuthorityUtils.createAuthorityList("ROLE_USER")))
  }

  @Test
  fun `GET returns 403 if user is not participant of project`() {
    every { participantAuthorization.isParticipantOf(any()) } returns false

    mockMvc
        .perform(
            get(
                "$ENDPOINT_PREFIX/projects/{projectIdentifier}/features",
                "e3f2179b-7051-4615-820d-490e9169005d"))
        .andExpect(status().isForbidden)
  }

  @Test
  fun `GET returns 400 for malformed project identifier`() {
    every { participantAuthorization.isParticipantOf(any()) } throws
        IllegalArgumentException("Invalid identifier format!")

    mockMvc
        .perform(
            get("$ENDPOINT_PREFIX/projects/{projectIdentifier}/features", "malformed-identifier"))
        .andExpect(status().isBadRequest)
  }

  @Test
  fun `GET only returns enabled features`() {
    every { participantAuthorization.isParticipantOf(any()) } returns true

    val enabledFeatureId = FeatureIdentifier("64544a2a-aa9d-488e-b841-225c7407820f")
    val disabledFeatureId = FeatureIdentifier("12344a2a-aa9d-488e-b841-225c7407820f")
    given(
        FeatureCreatedEvent(enabledFeatureId, "SOME_ENABLED_FEATURE"),
        FeatureEnabledEvent(enabledFeatureId),
        FeatureCreatedEvent(disabledFeatureId, "SOME_NOT_ENABLED_FEATURE"),
        FeatureDisabledEvent(disabledFeatureId),
    )

    mockMvc
        .perform(
            get(
                "$ENDPOINT_PREFIX/projects/{projectIdentifier}/features",
                "e3f2179b-7051-4615-820d-490e9169005d"))
        .andExpectAll(
            status().isOk,
            content()
                .json(
                    """
            {
            "items": ["SOME_ENABLED_FEATURE"]
            }
        """))
  }

  private fun given(vararg events: UpstreamFeatureEvent) {
    events.forEach { featureProjector.handle(it) }
  }
}

@TestConfiguration
class TestConfig {
  @Bean
  fun participantAuthorization() =
      object : ParticipantAuthorization {
        override fun isParticipantOf(projectIdentifier: String): Boolean {
          return true
        }
      }
}
