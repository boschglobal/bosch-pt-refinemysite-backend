/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite

import com.bosch.pt.iot.smartsite.ProjectApplicationRepositoryTest.AbstractSnapshotEntityRepositoryConfiguration
import com.bosch.pt.iot.smartsite.ProjectApplicationRepositoryTest.DataEntityRepositoryConfiguration
import com.bosch.pt.iot.smartsite.ProjectApplicationRepositoryTest.KafkaStreamRepositoryConfiguration
import com.bosch.pt.iot.smartsite.application.jpa.TestAuditorAware
import com.bosch.pt.iot.smartsite.common.kafka.streamable.KafkaStreamableRepositoryImpl
import com.bosch.pt.iot.smartsite.common.repository.impl.ReplicatedEntityRepositoryImpl
import java.util.TimeZone
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.transaction.annotation.Transactional

@Suppress("UnnecessaryAbstractClass")
@AutoConfigureTestDatabase(replace = NONE)
@TestPropertySource(properties = ["custom.business-transaction.consumer.persistence=jpa"])
@SpringJUnitConfig(
    classes =
        [
            ProjectApplicationRepositoryTest.AuditingConfiguration::class,
            KafkaStreamRepositoryConfiguration::class,
            DataEntityRepositoryConfiguration::class,
            AbstractSnapshotEntityRepositoryConfiguration::class])
@ActiveProfiles("test")
@DataJpaTest
// default transaction context will roll back in test class and prevent impacts on following tests
@Transactional
abstract class ProjectApplicationRepositoryTest {

  /** Test configuration for adding an auditor for this test. */
  @TestConfiguration
  @EnableJpaAuditing(auditorAwareRef = "auditorProvider")
  internal class AuditingConfiguration {
    /**
     * Creates the auditor provider for this test.
     *
     * @return the auditor provider
     */
    @Bean
    fun auditorProvider(): TestAuditorAware {
      return TestAuditorAware()
    }
  }

  /**
   * Configuration for kafka repositories. Use the test repository implementation, which supports
   * save without an eventType with [KafkaStreamableRepositoryImpl] repository base class.
   */
  @TestConfiguration
  @EnableJpaRepositories(
      basePackages =
          [
              "com.bosch.pt.iot.smartsite.project.messageattachment.repository",
              "com.bosch.pt.iot.smartsite.project.projectpicture.repository",
              "com.bosch.pt.iot.smartsite.project.projectstatistics.repository",
              "com.bosch.pt.iot.smartsite.project.relation.repository",
              "com.bosch.pt.iot.smartsite.project.rfv.repository",
              "com.bosch.pt.iot.smartsite.project.taskattachment.repository",
              "com.bosch.pt.iot.smartsite.project.taskconstraint.repository",
              "com.bosch.pt.iot.smartsite.project.taskstatistics.repository",
              "com.bosch.pt.iot.smartsite.project.topicattachment.repository"],
      repositoryBaseClass = KafkaStreamableRepositoryImpl::class)
  class KafkaStreamRepositoryConfiguration

  /**
   * Configuration for data repositories. Uses the [ReplicatedEntityRepositoryImpl] repository base
   * class.
   */
  @TestConfiguration
  @EnableJpaRepositories(
      basePackages =
          [
              "com.bosch.pt.iot.smartsite.company.repository",
              "com.bosch.pt.iot.smartsite.craft.repository",
              "com.bosch.pt.iot.smartsite.user.repository"],
      repositoryBaseClass = ReplicatedEntityRepositoryImpl::class)
  class DataEntityRepositoryConfiguration

  /** Configuration for data repositories without a base class */
  @TestConfiguration
  @EnableJpaRepositories(
      "com.bosch.pt.iot.smartsite.project.daycard.shared.repository",
      "com.bosch.pt.iot.smartsite.project.message.shared.repository",
      "com.bosch.pt.iot.smartsite.project.milestone.shared.repository",
      "com.bosch.pt.iot.smartsite.project.project.shared.repository",
      "com.bosch.pt.iot.smartsite.project.participant.shared.repository",
      "com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository",
      "com.bosch.pt.iot.smartsite.project.task.shared.repository",
      "com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository",
      "com.bosch.pt.iot.smartsite.project.topic.shared.repository",
      "com.bosch.pt.iot.smartsite.project.workarea.shared.repository")
  class AbstractSnapshotEntityRepositoryConfiguration

  companion object {
    init {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
  }
}
