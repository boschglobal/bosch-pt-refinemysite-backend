/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.common.repository

import com.bosch.pt.csm.cloud.usermanagement.application.MySqlTest
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.transaction.annotation.Transactional

@SpringJUnitConfig(
    classes = [AbstractSmartsiteApplicationRepositoryTest.JpaRepositoryConfiguration::class])
@ActiveProfiles("test")
@DataJpaTest
@MySqlTest
// default transaction context will roll back in test class and prevent impacts on following tests
@Transactional
class AbstractSmartsiteApplicationRepositoryTest {

  /**
   * Configuration for kafka repositories. Use the test repository implementation, which supports
   * save without an eventType.
   */
  @TestConfiguration
  @EnableJpaRepositories(
      basePackages =
          [
              "com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.repository",
              "com.bosch.pt.csm.cloud.usermanagement.user.user.shared.repository"])
  class JpaRepositoryConfiguration
}
