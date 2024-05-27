/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.common

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.registerStaticContext
import com.bosch.pt.csm.cloud.projectmanagement.application.config.EnableKafkaListeners
import com.bosch.pt.csm.cloud.projectmanagement.application.config.NewsServiceEventStreamContext
import com.bosch.pt.csm.cloud.projectmanagement.common.Repositories
import org.apache.avro.specific.SpecificRecordBase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder

@EnableKafkaListeners
open class AbstractEventStreamIntegrationTest {

  @Autowired lateinit var repositories: Repositories

  @Autowired lateinit var eventStreamGenerator: EventStreamGenerator

  @Autowired lateinit var eventStreamContext: NewsServiceEventStreamContext

  val context: MutableMap<String, SpecificRecordBase> by lazy { eventStreamContext.events }

  @BeforeEach
  fun init() {
    eventStreamGenerator.reset().registerStaticContext()
  }

  @AfterEach
  fun cleanup() {
    SecurityContextHolder.clearContext()
    repositories.truncateDatabase()
  }
}
