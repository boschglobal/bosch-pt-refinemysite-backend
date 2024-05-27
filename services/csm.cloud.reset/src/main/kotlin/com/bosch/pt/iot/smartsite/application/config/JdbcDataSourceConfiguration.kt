/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.config

import javax.sql.DataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate

@Profile("local", "kubernetes")
@Configuration
class JdbcDataSourceConfiguration {

  @Primary
  @Bean
  @ConfigurationProperties("smartsite.datasource.user-service")
  fun userDataSourceProperties(): DataSourceProperties = DataSourceProperties()

  @Bean
  @ConfigurationProperties("smartsite.datasource.user-restore")
  @Profile("user-restore-db")
  fun userRestoreDataSourceProperties(): DataSourceProperties = DataSourceProperties()

  @Bean
  @ConfigurationProperties("smartsite.datasource.company-service")
  fun companyDataSourceProperties(): DataSourceProperties = DataSourceProperties()

  @Bean
  @ConfigurationProperties("smartsite.datasource.company-restore")
  @Profile("company-restore-db")
  fun companyRestoreDataSourceProperties(): DataSourceProperties = DataSourceProperties()

  @Bean
  @ConfigurationProperties("smartsite.datasource.project-service")
  fun projectDataSourceProperties(): DataSourceProperties = DataSourceProperties()

  @Bean
  @ConfigurationProperties("smartsite.datasource.project-restore")
  @Profile("project-restore-db")
  fun projectRestoreDataSourceProperties(): DataSourceProperties = DataSourceProperties()

  @Bean
  @ConfigurationProperties("smartsite.datasource.news-service")
  fun newsDataSourceProperties(): DataSourceProperties = DataSourceProperties()

  @Bean
  @ConfigurationProperties("smartsite.datasource.statistics-service")
  fun statisticsDataSourceProperties(): DataSourceProperties = DataSourceProperties()

  @Bean
  @ConfigurationProperties("smartsite.datasource.bam")
  @Profile("reset-bam")
  fun bamDataSourceProperties(): DataSourceProperties = DataSourceProperties()

  @Bean
  @ConfigurationProperties("smartsite.datasource.featuretoggle-service")
  fun featuretoggleDataSourceProperties(): DataSourceProperties = DataSourceProperties()

  @Primary
  @Bean("userDataSource")
  @ConfigurationProperties("smartsite.datasource.user-service")
  fun userDataSource(): DataSource =
      userDataSourceProperties().initializeDataSourceBuilder().build()

  @Bean("userRestoreDataSource")
  @ConfigurationProperties("smartsite.datasource.user-restore")
  @Profile("user-restore-db")
  fun userRestoreDataSource(): DataSource =
      userRestoreDataSourceProperties().initializeDataSourceBuilder().build()

  @Bean("companyDataSource")
  @ConfigurationProperties("smartsite.datasource.company-service")
  fun companyDataSource(): DataSource =
      companyDataSourceProperties().initializeDataSourceBuilder().build()

  @Bean("companyRestoreDataSource")
  @ConfigurationProperties("smartsite.datasource.company-restore")
  @Profile("company-restore-db")
  fun companyRestoreDataSource(): DataSource =
      companyRestoreDataSourceProperties().initializeDataSourceBuilder().build()

  @Bean("projectDataSource")
  @ConfigurationProperties("smartsite.datasource.project-service")
  fun projectDataSource(): DataSource =
      projectDataSourceProperties().initializeDataSourceBuilder().build()

  @Bean("projectRestoreDataSource")
  @ConfigurationProperties("smartsite.datasource.project-restore")
  @Profile("project-restore-db")
  fun projectRestoreDataSource(): DataSource =
      projectRestoreDataSourceProperties().initializeDataSourceBuilder().build()

  @Bean("newsDataSource")
  @ConfigurationProperties("smartsite.datasource.news-service")
  fun newsDataSource(): DataSource =
      newsDataSourceProperties().initializeDataSourceBuilder().build()

  @Bean("statisticsDataSource")
  @ConfigurationProperties("smartsite.datasource.statistics-service")
  fun statisticsDataSource(): DataSource =
      statisticsDataSourceProperties().initializeDataSourceBuilder().build()

  @Bean("bamDataSource")
  @ConfigurationProperties("smartsite.datasource.bam")
  @Profile("reset-bam")
  fun bamDataSource(): DataSource = bamDataSourceProperties().initializeDataSourceBuilder().build()

  @Bean("featuretoggleDataSource")
  @ConfigurationProperties("smartsite.datasource.featuretoggle")
  fun featuretoggleDataSource(): DataSource =
      featuretoggleDataSourceProperties().initializeDataSourceBuilder().build()

  @Bean("userJdbcTemplate")
  fun userJdbcTemplate(@Qualifier("userDataSource") userDataSource: DataSource): JdbcTemplate =
      JdbcTemplate(userDataSource)

  @Bean("userRestoreJdbcTemplate")
  @Profile("user-restore-db")
  fun userRestoreJdbcTemplate(
      @Qualifier("userRestoreDataSource") userRestoreDataSource: DataSource
  ): JdbcTemplate = JdbcTemplate(userRestoreDataSource)

  @Bean("companyJdbcTemplate")
  fun companyJdbcTemplate(
      @Qualifier("companyDataSource") companyDataSource: DataSource
  ): JdbcTemplate = JdbcTemplate(companyDataSource)

  @Bean("companyRestoreJdbcTemplate")
  @Profile("company-restore-db")
  fun companyRestoreJdbcTemplate(
      @Qualifier("companyRestoreDataSource") companyRestoreDataSource: DataSource
  ): JdbcTemplate = JdbcTemplate(companyRestoreDataSource)

  @Bean("projectJdbcTemplate")
  fun projectJdbcTemplate(
      @Qualifier("projectDataSource") projectDataSource: DataSource
  ): JdbcTemplate = JdbcTemplate(projectDataSource)

  @Bean("projectRestoreJdbcTemplate")
  @Profile("project-restore-db")
  fun projectRestoreJdbcTemplate(
      @Qualifier("projectRestoreDataSource") projectRestoreDataSource: DataSource
  ): JdbcTemplate = JdbcTemplate(projectRestoreDataSource)

  @Bean("newsJdbcTemplate")
  fun newsJdbcTemplate(@Qualifier("newsDataSource") newsDataSource: DataSource): JdbcTemplate =
      JdbcTemplate(newsDataSource)

  @Bean("statisticsJdbcTemplate")
  fun statisticsJdbcTemplate(
      @Qualifier("statisticsDataSource") statisticsDataSource: DataSource
  ): JdbcTemplate = JdbcTemplate(statisticsDataSource)

  @Bean("bamJdbcTemplate")
  @Profile("reset-bam")
  fun bamJdbcTemplate(@Qualifier("bamDataSource") bamDataSource: DataSource): JdbcTemplate =
      JdbcTemplate(bamDataSource)

  @Bean("featuretoggleJdbcTemplate")
  fun featuretoggleJdbcTemplate(
      @Qualifier("featuretoggleDataSource") featuretoggleDataSource: DataSource
  ): JdbcTemplate = JdbcTemplate(featuretoggleDataSource)
}
