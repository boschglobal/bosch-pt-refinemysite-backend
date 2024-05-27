/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.facade.listener

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.csm.cloud.common.eventstore.EventStoreUtils
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreStrategyTest
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.registerStaticContext
import com.bosch.pt.csm.cloud.common.util.HttpTestUtils.setFakeUrlWithApiVersion
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.authorizeWithStandardUser
import com.bosch.pt.iot.smartsite.common.model.VersionedEntity
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextKafkaEvent
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectInvitationContextKafkaEvent
import com.bosch.pt.iot.smartsite.test.Repositories
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.util.getCreatedBy
import com.bosch.pt.iot.smartsite.util.getCreatedDate
import com.bosch.pt.iot.smartsite.util.getLastModifiedBy
import com.bosch.pt.iot.smartsite.util.getLastModifiedDate
import java.time.LocalDateTime
import java.util.TimeZone
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.data.domain.Auditable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.support.TransactionTemplate

@Suppress("UnnecessaryAbstractClass")
@EnableAllKafkaListeners
@RestoreStrategyTest
abstract class AbstractRestoreIntegrationTestV2 {

  @Autowired protected lateinit var messageSource: MessageSource

  @Autowired
  protected lateinit var projectEventStoreUtils: EventStoreUtils<ProjectContextKafkaEvent>

  @Autowired
  protected lateinit var invitationEventStoreUtils:
      EventStoreUtils<ProjectInvitationContextKafkaEvent>

  @Autowired protected lateinit var repositories: Repositories

  @Autowired protected lateinit var transactionTemplate: TransactionTemplate

  @Autowired protected lateinit var eventStreamGenerator: EventStreamGenerator

  @BeforeEach
  protected fun initAbstractIntegrationTest() {
    eventStreamGenerator.registerStaticContext()
    setFakeUrlWithApiVersion()
  }

  @AfterEach
  protected fun cleanup() {
    eventStreamGenerator.reset()
    projectEventStoreUtils.reset()
    invitationEventStoreUtils.reset()
    SecurityContextHolder.clearContext()
    repositories.truncateDatabase()
  }

  protected fun setAuthentication(userIdentifier: UUID) =
      authorizeWithStandardUser(repositories.userRepository.findOneByIdentifier(userIdentifier))

  protected fun getUserIdentifierAvro(identifier: UUID): AggregateIdentifierAvro =
      getAggregateIdentifierAvro(identifier, USER.value)

  protected fun <U> validateAuditableAndVersionedEntityAttributes(
      actualEntity: U,
      compareWithAggregate: SpecificRecordBase
  ) where U : Auditable<User, *, LocalDateTime>, U : VersionedEntity =
      with(actualEntity) {
        assertThat(createdBy.get().identifier)
            .isEqualTo(compareWithAggregate.getCreatedBy().identifier.toUUID())
        assertThat(lastModifiedBy.get().identifier)
            .isEqualTo(compareWithAggregate.getLastModifiedBy().identifier.toUUID())
        assertThat(createdDate.get()).isEqualTo(compareWithAggregate.getCreatedDate())
        assertThat(lastModifiedDate.get()).isEqualTo(compareWithAggregate.getLastModifiedDate())

        (compareWithAggregate.get("aggregateIdentifier") as AggregateIdentifierAvro).let {
          assertThat(requireNotNull(identifier)).isEqualTo(it.identifier.toUUID())
          assertThat(requireNotNull(version)).isEqualTo(it.version)
        }
      }

  private fun getAggregateIdentifierAvro(identifier: UUID, type: String): AggregateIdentifierAvro =
      AggregateIdentifierAvro.newBuilder()
          .setType(type)
          .setVersion(0)
          .setIdentifier(identifier.toString())
          .build()

  companion object {
    init {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    fun validateAuditingInformationAndIdentifierAndVersion(
        actualEntity: AbstractSnapshotEntity<Long, *>,
        compareWithAggregate: SpecificRecordBase
    ) {
      (compareWithAggregate.get("aggregateIdentifier") as AggregateIdentifierAvro).let {
        assertThat(actualEntity.identifier.toUuid()).isEqualTo(it.identifier.toUUID())
        assertThat(actualEntity.version).isEqualTo(it.version)
      }
      assertThat(actualEntity.createdBy.get())
          .isEqualTo(compareWithAggregate.getCreatedBy().identifier.toUUID().asUserId())
      assertThat(actualEntity.lastModifiedBy.get())
          .isEqualTo(compareWithAggregate.getLastModifiedBy().identifier.toUUID().asUserId())
      assertThat(actualEntity.createdDate.get()).isEqualTo(compareWithAggregate.getCreatedDate())
      assertThat(actualEntity.lastModifiedDate.get())
          .isEqualTo(compareWithAggregate.getLastModifiedDate())
    }
  }
}
