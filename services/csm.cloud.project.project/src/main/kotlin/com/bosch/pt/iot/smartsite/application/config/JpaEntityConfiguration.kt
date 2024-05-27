/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.config

import javax.sql.DataSource
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean

@Configuration
@EnableCaching
class JpaEntityConfiguration(
    private val dataSource: DataSource,
    private val jpaProperties: JpaProperties,
) {

  @Bean
  fun jdbcTemplate(): JdbcTemplate = JdbcTemplate(dataSource)

  @Bean(name = ["entityManagerFactory"])
  fun customerEntityManagerFactory(
      builder: EntityManagerFactoryBuilder
  ): LocalContainerEntityManagerFactoryBean =
      builder
          .dataSource(dataSource)
          .packages(
              "com.bosch.pt.csm.cloud.common.businesstransaction.jpa",
              "com.bosch.pt.iot.smartsite.attachment.model",
              "com.bosch.pt.iot.smartsite.common.kafka.streamable",
              "com.bosch.pt.iot.smartsite.common.model.converter",
              "com.bosch.pt.iot.smartsite.company.model",
              "com.bosch.pt.iot.smartsite.craft.model",
              "com.bosch.pt.iot.smartsite.project.attachment.model",
              "com.bosch.pt.iot.smartsite.project.eventstore",
              "com.bosch.pt.iot.smartsite.project.projectcraft.shared.model",
              "com.bosch.pt.iot.smartsite.project.message.shared.model",
              "com.bosch.pt.iot.smartsite.project.messageattachment.model",
              "com.bosch.pt.iot.smartsite.project.milestone.shared.model",
              "com.bosch.pt.iot.smartsite.project.participant.shared.model",
              "com.bosch.pt.iot.smartsite.project.project.shared.model",
              "com.bosch.pt.iot.smartsite.project.projectpicture.model",
              "com.bosch.pt.iot.smartsite.project.projectstatistics.model",
              "com.bosch.pt.iot.smartsite.project.relation.model",
              "com.bosch.pt.iot.smartsite.project.task.shared.model",
              "com.bosch.pt.iot.smartsite.project.taskschedule.shared.model",
              "com.bosch.pt.iot.smartsite.project.daycard.shared.model",
              "com.bosch.pt.iot.smartsite.project.external.model",
              "com.bosch.pt.iot.smartsite.project.importer.model",
              "com.bosch.pt.iot.smartsite.project.rfv.model",
              "com.bosch.pt.iot.smartsite.project.taskaction.model",
              "com.bosch.pt.iot.smartsite.project.taskconstraint.model",
              "com.bosch.pt.iot.smartsite.project.taskstatistics.model",
              "com.bosch.pt.iot.smartsite.project.taskattachment.model",
              "com.bosch.pt.iot.smartsite.project.topic.shared.model",
              "com.bosch.pt.iot.smartsite.project.topicattachment.model",
              "com.bosch.pt.iot.smartsite.project.workarea.shared.model",
              "com.bosch.pt.iot.smartsite.project.workday.shared.model",
              "com.bosch.pt.iot.smartsite.user.authorization.model",
              "com.bosch.pt.iot.smartsite.user.model")
          .properties(jpaProperties.properties)
          .build()
}
