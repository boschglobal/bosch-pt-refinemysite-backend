/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.config

import com.bosch.pt.iot.smartsite.dataimport.company.rest.CompanyRestClient
import com.bosch.pt.iot.smartsite.dataimport.employee.rest.EmployeeRestClient
import com.bosch.pt.iot.smartsite.dataimport.featuretoggle.rest.FeatureToggleRestClient
import com.bosch.pt.iot.smartsite.dataimport.project.rest.MilestoneRestClient
import com.bosch.pt.iot.smartsite.dataimport.project.rest.ProjectCraftRestClient
import com.bosch.pt.iot.smartsite.dataimport.project.rest.ProjectParticipantRestClient
import com.bosch.pt.iot.smartsite.dataimport.project.rest.ProjectRestClient
import com.bosch.pt.iot.smartsite.dataimport.project.rest.WorkAreaRestClient
import com.bosch.pt.iot.smartsite.dataimport.task.rest.DayCardRestClient
import com.bosch.pt.iot.smartsite.dataimport.task.rest.MessageRestClient
import com.bosch.pt.iot.smartsite.dataimport.task.rest.TaskRestClient
import com.bosch.pt.iot.smartsite.dataimport.task.rest.TopicRestClient
import com.bosch.pt.iot.smartsite.dataimport.user.rest.ConsentsRestClient
import com.bosch.pt.iot.smartsite.dataimport.user.rest.CraftRestClient
import com.bosch.pt.iot.smartsite.dataimport.user.rest.UserRestClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

@Configuration
class RestConfiguration(
    @Value("\${csm-cloud-company.url}") private val csmCloudCompanyUrl: String,
    @Value("\${csm-cloud-project.url}") private val csmCloudProjectUrl: String,
    @Value("\${csm-cloud-user.url}") private val csmCloudUserUrl: String,
    @Value("\${csm-cloud-feature-toggle.url}") private val csmCloudFeatureToggleUrl: String
) {

  @Bean
  fun companyRetrofit(@Qualifier("okHttpClient") okHttpClient: OkHttpClient): Retrofit =
      buildClient(okHttpClient, csmCloudCompanyUrl)

  @Bean
  fun projectRetrofit(@Qualifier("okHttpClient") okHttpClient: OkHttpClient): Retrofit =
      buildClient(okHttpClient, csmCloudProjectUrl)

  @Bean
  fun userRetrofit(@Qualifier("okHttpClient") okHttpClient: OkHttpClient): Retrofit =
      buildClient(okHttpClient, csmCloudUserUrl)

  @Bean
  fun featureToggleRetrofit(@Qualifier("okHttpClient") okHttpClient: OkHttpClient): Retrofit =
      buildClient(okHttpClient, csmCloudFeatureToggleUrl)

  @Bean
  fun idpRestTemplate(@Qualifier("idpOkHttpClient") okHttpClient: OkHttpClient): RestTemplate =
      RestTemplate().apply { requestFactory = OkHttp3ClientHttpRequestFactory(okHttpClient) }

  @Bean
  fun restTemplate(@Qualifier("okHttpClient") okHttpClient: OkHttpClient): RestTemplate =
      RestTemplate().apply { requestFactory = OkHttp3ClientHttpRequestFactory(okHttpClient) }

  @Bean
  fun companyRestClient(companyRetrofit: Retrofit): CompanyRestClient =
      companyRetrofit.create(CompanyRestClient::class.java)

  @Bean
  fun consentsRestClient(userRetrofit: Retrofit): ConsentsRestClient =
      userRetrofit.create(ConsentsRestClient::class.java)

  @Bean
  fun employeeRestClient(companyRetrofit: Retrofit): EmployeeRestClient =
      companyRetrofit.create(EmployeeRestClient::class.java)

  @Bean
  fun featureToggleRestClient(featureToggleRetrofit: Retrofit): FeatureToggleRestClient =
      featureToggleRetrofit.create(FeatureToggleRestClient::class.java)

  @Bean
  fun projectParticipantRestClient(projectRetrofit: Retrofit): ProjectParticipantRestClient =
      projectRetrofit.create(ProjectParticipantRestClient::class.java)

  @Bean
  fun projectRestClient(projectRetrofit: Retrofit): ProjectRestClient =
      projectRetrofit.create(ProjectRestClient::class.java)

  @Bean
  fun projectCraftRestClient(projectRetrofit: Retrofit): ProjectCraftRestClient =
      projectRetrofit.create(ProjectCraftRestClient::class.java)

  @Bean
  fun milestoneRestClient(projectRetrofit: Retrofit): MilestoneRestClient =
      projectRetrofit.create(MilestoneRestClient::class.java)

  @Bean
  fun workAreaRestClient(projectRetrofit: Retrofit): WorkAreaRestClient =
      projectRetrofit.create(WorkAreaRestClient::class.java)

  @Bean
  fun topicRestClient(projectRetrofit: Retrofit): TopicRestClient =
      projectRetrofit.create(TopicRestClient::class.java)

  @Bean
  fun messageRestClient(projectRetrofit: Retrofit): MessageRestClient =
      projectRetrofit.create(MessageRestClient::class.java)

  @Bean
  fun taskRestClient(projectRetrofit: Retrofit): TaskRestClient =
      projectRetrofit.create(TaskRestClient::class.java)

  @Bean
  fun dayCardRestClient(projectRetrofit: Retrofit): DayCardRestClient =
      projectRetrofit.create(DayCardRestClient::class.java)

  @Bean
  fun craftRestClient(userRetrofit: Retrofit): CraftRestClient =
      userRetrofit.create(CraftRestClient::class.java)

  @Bean
  fun userRestClient(userRetrofit: Retrofit): UserRestClient =
      userRetrofit.create(UserRestClient::class.java)

  private fun buildClient(client: OkHttpClient, baseUrl: String): Retrofit =
      Retrofit.Builder()
          .baseUrl(baseUrl)
          .addConverterFactory(
              JacksonConverterFactory.create(
                  ObjectMapper().apply {
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                  }))
          .client(client)
          .build()
}
