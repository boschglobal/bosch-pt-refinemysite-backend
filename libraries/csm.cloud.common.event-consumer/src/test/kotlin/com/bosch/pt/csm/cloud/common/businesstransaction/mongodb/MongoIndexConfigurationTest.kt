/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.businesstransaction.mongodb

import com.bosch.pt.csm.cloud.application.MongoDbTest
import com.bosch.pt.csm.cloud.application.TestApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(properties = ["custom.business-transaction.consumer.persistence=mongodb"])
@ContextConfiguration(classes = [TestApplication::class])
@MongoDbTest
internal class MongoIndexConfigurationTest {

  @Autowired private lateinit var mongoTemplate: MongoTemplate

  @Test
  fun `mongodb indexes are created on application startup`() {
    val indexes =
        mongoTemplate.indexOps(EventsOfBusinessTransactionDocument.COLLECTION_NAME).indexInfo

    assertThat(indexes.size).isEqualTo(2)
  }
}
