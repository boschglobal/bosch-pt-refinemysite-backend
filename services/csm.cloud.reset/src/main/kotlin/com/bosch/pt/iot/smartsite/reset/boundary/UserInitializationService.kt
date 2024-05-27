/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.reset.boundary

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.messages.AuditingInformationAvro
import com.bosch.pt.csm.cloud.common.messages.IsoCountryCodeEnumAvro
import com.bosch.pt.csm.cloud.common.messages.MessageKeyAvro
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.common.messages.GenderEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro.REGISTERED
import com.bosch.pt.iot.smartsite.application.config.properties.CustomKafkaProperties
import com.bosch.pt.iot.smartsite.application.config.properties.CustomKafkaProperties.Companion.USER_BINDING
import com.bosch.pt.iot.smartsite.application.config.properties.SystemUserProperties
import com.bosch.pt.iot.smartsite.application.config.properties.UserProperties
import com.bosch.pt.iot.smartsite.application.config.properties.UserServiceProperties
import io.confluent.kafka.serializers.KafkaAvroSerializer
import java.nio.charset.StandardCharsets
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.utils.Utils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.stereotype.Service
import org.springframework.util.Assert

@Service
class UserInitializationService(
    private val userServiceProperties: UserServiceProperties,
    private val systemUserProperties: SystemUserProperties,
    @Qualifier("userJdbcTemplate") private val jdbcOperationsUsers: JdbcOperations,
    private val customKafkaProperties: CustomKafkaProperties,
    kafkaProperties: KafkaProperties
) {

  // Workaround since configuration as a bean is currently not working. See:
  // https://github.com/confluentinc/schema-registry/issues/553
  private val kafkaAvroSerializer: KafkaAvroSerializer =
      KafkaAvroSerializer(null, kafkaProperties.properties)

  fun initialize() {
    LOGGER.info("Create system users...")
    val topic = customKafkaProperties.getTopicForChannel(USER_BINDING)
    Assert.notNull(topic, "Topic with key 'user' not found in topic mapping yaml configuration")
    systemUserProperties.users.forEach { userProperties: UserProperties ->
      // Insert user in task service
      val insertStatement = userServiceProperties.insertStatement
      val updatedInsertStatement =
          String.format(
              insertStatement,
              userProperties.id,
              userProperties.identifier,
              if (userProperties.admin) 1 else 0,
              userProperties.email,
              userProperties.firstName,
              userProperties.lastName,
              userProperties.userId,
              userProperties.createdBy,
              userProperties.lastModifiedBy,
              userProperties.locale.toString(),
              userProperties.country.name)
      jdbcOperationsUsers.update(updatedInsertStatement)

      // Insert announcement permission
      if (userProperties.announcement) {
        jdbcOperationsUsers.update(
          "insert into announcement_permission (user_id) values (${userProperties.id});")
      }

      // Send message via kafka-connector
      val key = getAvroMessageKey(userProperties.identifier)
      val value = getAvroMessage(userProperties)
      val serializedKey = kafkaAvroSerializer.serialize(topic, key)
      val serializedValue = kafkaAvroSerializer.serialize(topic, value)
      val partition = partitionOf(key)
      jdbcOperationsUsers.update(
          userServiceProperties.insertMessageStatement, serializedKey, serializedValue, partition)
    }
  }

  private fun partitionOf(keyAvro: MessageKeyAvro): Int {
    return (Utils.toPositive(
        Utils.murmur2(keyAvro.getRootContextIdentifier().toByteArray(StandardCharsets.UTF_8))) %
        customKafkaProperties.bindings[USER_BINDING]!!.configuration.partitions)
  }

  private fun getAvroMessageKey(identifier: String?): MessageKeyAvro {
    return MessageKeyAvro.newBuilder()
        .setRootContextIdentifier(identifier)
        .setAggregateIdentifier(toAggregateIdentifier(identifier, USER.value))
        .build()
  }

  private fun getAvroMessage(userProperties: UserProperties): SpecificRecord {
    val userAggregateAvro =
        UserAggregateAvro.newBuilder()
            .setAggregateIdentifier(toAggregateIdentifier(userProperties.identifier, USER.value))
            .setAuditingInformation(toAuditingInformationAvro(userProperties.identifier))
            .setEmail(userProperties.email)
            .setPhoneNumbers(emptyList())
            .setCrafts(emptyList())
            .setFirstName(userProperties.firstName)
            .setGender(GenderEnumAvro.FEMALE)
            .setLastName(userProperties.lastName)
            .setRegistered(true)
            .setAdmin(userProperties.admin)
            .setUserId(userProperties.userId)
            .setLocale(userProperties.locale.toString())
            .setCountry(IsoCountryCodeEnumAvro.valueOf(userProperties.country.name))
    return UserEventAvro.newBuilder()
        .setName(REGISTERED)
        .setAggregateBuilder(userAggregateAvro)
        .build()
  }

  private fun toAuditingInformationAvro(identifier: String?): AuditingInformationAvro {
    return AuditingInformationAvro.newBuilder()
        .setCreatedBy(toAggregateIdentifier(identifier, USER.value))
        .setCreatedDate(0L)
        .setLastModifiedBy(toAggregateIdentifier(identifier, USER.value))
        .setLastModifiedDate(0L)
        .build()
  }

  private fun toAggregateIdentifier(identifier: String?, type: String): AggregateIdentifierAvro {
    return AggregateIdentifierAvro.newBuilder()
        .setIdentifier(identifier)
        .setVersion(0L)
        .setType(type)
        .build()
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(UserInitializationService::class.java)
  }
}
