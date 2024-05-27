/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.facade.rest

import com.bosch.pt.csm.cloud.common.api.toAggregateReference
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.csm.cloud.common.eventstore.EventStoreUtils
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.registerStaticContext
import com.bosch.pt.csm.cloud.common.util.HttpTestUtils.setFakeUrlWithApiVersion
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum
import com.bosch.pt.iot.smartsite.application.SmartSiteSpringBootTest
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.authorizeWithUser
import com.bosch.pt.iot.smartsite.common.event.ProjectServiceEventStreamContext
import com.bosch.pt.iot.smartsite.common.model.AbstractEntity
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextKafkaEvent
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectInvitationContextKafkaEvent
import com.bosch.pt.iot.smartsite.test.Repositories
import com.bosch.pt.iot.smartsite.test.TimeLineGeneratorImpl
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.util.getCreatedBy
import com.bosch.pt.iot.smartsite.util.getCreatedDate
import com.bosch.pt.iot.smartsite.util.getLastModifiedBy
import com.bosch.pt.iot.smartsite.util.getLastModifiedDate
import java.util.TimeZone
import java.util.UUID
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import org.apache.avro.specific.SpecificRecordBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.support.TransactionTemplate

@Suppress("UnnecessaryAbstractClass")
@SmartSiteSpringBootTest
abstract class AbstractIntegrationTestV2 {

  @Autowired protected lateinit var messageSource: MessageSource

  @Autowired
  protected lateinit var projectEventStoreUtils: EventStoreUtils<ProjectContextKafkaEvent>

  @Autowired
  protected lateinit var invitationEventStoreUtils:
      EventStoreUtils<ProjectInvitationContextKafkaEvent>

  @Autowired protected lateinit var repositories: Repositories

  @Autowired protected lateinit var transactionTemplate: TransactionTemplate

  @Autowired protected lateinit var eventStreamGenerator: EventStreamGenerator

  @Autowired protected lateinit var jdbcTemplateLockProvider: JdbcTemplateLockProvider

  protected lateinit var timeLineGenerator: TimeLineGeneratorImpl

  @BeforeEach
  protected fun initAbstractIntegrationTest() {
    timeLineGenerator = eventStreamGenerator.getContext().timeLineGenerator as TimeLineGeneratorImpl
    eventStreamGenerator.registerStaticContext()
    setFakeUrlWithApiVersion()

    // The online/restore listeners can be activated on demand in tests, so we have to reset them
    // before a new test is executed. The default listeners for tests are the restore listeners.
    // Therefore, it is reset to the default here.
    useRestoreListener()
  }

  @AfterEach
  protected fun cleanup() {
    eventStreamGenerator.reset()
    projectEventStoreUtils.reset()
    invitationEventStoreUtils.reset()
    SecurityContextHolder.clearContext()
    repositories.truncateDatabase()
    jdbcTemplateLockProvider.clearCache()
    LocaleContextHolder.resetLocaleContext()
  }

  protected fun setAuthentication(userReference: String) =
      setAuthentication(eventStreamGenerator.getIdentifier(userReference))

  protected fun setAuthentication(userIdentifier: UUID) =
      repositories.userRepository.findOneByIdentifier(userIdentifier)!!.also {
        authorizeWithUser(it, it.admin)
      }

  protected fun useOnlineListener() {
    (eventStreamGenerator.getContext() as ProjectServiceEventStreamContext).useOnlineListener()
  }

  protected fun useRestoreListener() {
    (eventStreamGenerator.getContext() as ProjectServiceEventStreamContext).useRestoreListener()
  }

  protected fun validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
      actualAggregate: SpecificRecordBase,
      compareWithEntity: AbstractEntity<Long, *>,
  ) =
      with(actualAggregate) {
        assertThat(getCreatedBy())
            .isEqualTo(compareWithEntity.createdBy.get().toAggregateIdentifier())
        assertThat(getLastModifiedBy())
            .isEqualTo(compareWithEntity.lastModifiedBy.get().toAggregateIdentifier())
        assertThat(getCreatedDate()).isEqualTo(compareWithEntity.createdDate.get())
        assertThat(getLastModifiedDate()).isEqualTo(compareWithEntity.lastModifiedDate.get())

        with(get("aggregateIdentifier") as AggregateIdentifierAvro) {
          assertThat(identifier).isEqualTo(compareWithEntity.identifier.toString())
          assertThat(version).isEqualTo(requireNotNull(compareWithEntity.version))
          assertThat(type).isEqualTo(compareWithEntity.getAggregateType())
        }
      }

  protected fun validateDeletedAggregateAuditInfoAndAggregateIdentifier(
      actualAggregate: SpecificRecordBase,
      compareWithEntity: AbstractEntity<Long, *>,
      expectedLastModifiedBy: User,
  ) =
      with(actualAggregate) {
        assertThat(getCreatedBy())
            .isEqualTo(compareWithEntity.createdBy.get().toAggregateIdentifier())
        assertThat(getLastModifiedBy()).isEqualTo(expectedLastModifiedBy.toAggregateIdentifier())
        assertThat(getCreatedDate()).isEqualTo(compareWithEntity.createdDate.get())
        assertThat(getLastModifiedDate()).isNotNull

        with(get("aggregateIdentifier") as AggregateIdentifierAvro) {
          assertThat(identifier).isEqualTo(compareWithEntity.identifier.toString())
          assertThat(version).isEqualTo(requireNotNull(compareWithEntity.version) + 1)
          assertThat(type).isEqualTo(compareWithEntity.getAggregateType())
        }
      }

  protected fun getUserIdentifierAvro(identifier: UUID): AggregateIdentifierAvro =
      getAggregateIdentifierAvro(identifier, UsermanagementAggregateTypeEnum.USER.value)

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

    fun validateCreatedAggregateAuditInfoAndAggregateIdentifier(
        actualAggregate: SpecificRecordBase,
        expectedAggregateType: ProjectmanagementAggregateTypeEnum,
        expectedCreatedBy: User
    ) =
        with(actualAggregate) {
          assertThat(actualAggregate.getCreatedBy())
              .isEqualTo(expectedCreatedBy.toAggregateIdentifier())
          assertThat(actualAggregate.getLastModifiedBy())
              .isEqualTo(expectedCreatedBy.toAggregateIdentifier())

          with(get("aggregateIdentifier") as AggregateIdentifierAvro) {
            assertThat(identifier).isNotNull
            assertThat(version).isEqualTo(0L)
            assertThat(type).isEqualTo(expectedAggregateType.value)
          }
        }

    fun validateUpdatedAggregateAuditInfoAndAggregateIdentifier(
        actualAggregate: SpecificRecordBase,
        compareWithEntity: AbstractSnapshotEntity<*, *>,
        expectedAggregateType: ProjectmanagementAggregateTypeEnum
    ) =
        with(actualAggregate) {
          assertThat(getCreatedBy())
              .isEqualTo(compareWithEntity.createdBy.get().toAggregateReference())
          assertThat(getLastModifiedBy())
              .isEqualTo(compareWithEntity.lastModifiedBy.get().toAggregateReference())
          assertThat(getCreatedDate()).isEqualTo(compareWithEntity.createdDate.get())
          assertThat(getLastModifiedDate()).isEqualTo(compareWithEntity.lastModifiedDate.get())

          with(get("aggregateIdentifier") as AggregateIdentifierAvro) {
            assertThat(identifier).isEqualTo(compareWithEntity.identifier.toString())
            assertThat(version).isEqualTo(requireNotNull(compareWithEntity.version))
            assertThat(type).isEqualTo(expectedAggregateType.value)
          }
        }

    fun validateDeletedAggregateAuditInfoAndAggregateIdentifier(
        actualAggregate: SpecificRecordBase,
        compareWithEntity: AbstractSnapshotEntity<*, *>,
        expectedAggregateType: ProjectmanagementAggregateTypeEnum,
        expectedLastModifiedBy: User
    ) =
        with(actualAggregate) {
          assertThat(getCreatedBy())
              .isEqualTo(compareWithEntity.createdBy.get().toAggregateReference())
          assertThat(getLastModifiedBy()).isEqualTo(expectedLastModifiedBy.toAggregateIdentifier())
          assertThat(getCreatedDate()).isEqualTo(compareWithEntity.createdDate.get())
          assertThat(getLastModifiedDate()).isNotNull

          with(get("aggregateIdentifier") as AggregateIdentifierAvro) {
            assertThat(identifier).isEqualTo(compareWithEntity.identifier.toString())
            assertThat(version).isEqualTo(requireNotNull(compareWithEntity.version) + 1)
            assertThat(type).isEqualTo(expectedAggregateType.value)
          }
        }
  }
}
