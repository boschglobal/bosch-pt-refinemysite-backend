/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.translation.facade.rest

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.common.utils.toLanguage
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.model.TopicCriticalityEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.translation.facade.rest.resource.response.assembler.ProjectTranslationsResourceAssembler
import com.bosch.pt.csm.cloud.projectmanagement.translation.facade.rest.resource.response.TranslationListResource
import com.bosch.pt.csm.cloud.projectmanagement.translation.facade.rest.resource.response.TranslationResource
import com.bosch.pt.csm.cloud.projectmanagement.user.translation.facade.rest.resource.response.assembler.UserTranslationsResourceAssembler
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@RmsSpringBootTest
class TranslationRestApiIntegrationTest : AbstractRestApiIntegrationTest() {

  @Autowired
  private lateinit var projectTranslationsResourceAssembler: ProjectTranslationsResourceAssembler

  @Autowired
  private lateinit var userTranslationsResourceAssembler: UserTranslationsResourceAssembler

  @Value("\${locale.supported}") private lateinit var supportedLocales: List<Locale>

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query translations`() {
    val translations = query()

    assertThat(translations.translations).isNotEmpty
    assertThat(translations.translations)
        .isEqualTo(
            (projectTranslationsResourceAssembler.assemble() +
                    userTranslationsResourceAssembler.assemble())
                .sortedWith(compareBy(TranslationResource::key, TranslationResource::language)))
  }

  @Test
  fun `query and validate single translation`() {
    val translations = query()
    val translation =
        translations.translations.first { it.key == TopicCriticalityEnum.CRITICAL.key }

    assertThat(translation.key).isEqualTo("TOPIC_CRITICALITY_CRITICAL")
    assertThat(translation.language).isEqualTo("Deutsch")
    assertThat(translation.value).isEqualTo("Kritisch")
  }

  @Test
  fun `query and validate languages`() {
    val translations = query()

    assertThat(translations.translations.map { it.language }.distinct())
        .isEqualTo(supportedLocales.map { it.toLanguage() }.sorted())
  }

  private fun query() =
      super.query(
          latestTranslationApi("/translations"),
          false,
          TranslationListResource::class.java)
}
