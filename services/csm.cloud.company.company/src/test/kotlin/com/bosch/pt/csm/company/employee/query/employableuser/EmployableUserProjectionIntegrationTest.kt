/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.company.employee.query.employableuser

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitCompany
import com.bosch.pt.csm.cloud.companymanagement.company.event.submitEmployee
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeAggregateAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.EmployeeEventEnumAvro
import com.bosch.pt.csm.cloud.companymanagement.employee.messages.getIdentifier
import com.bosch.pt.csm.cloud.usermanagement.common.UsermanagementAggregateTypeEnum.USER
import com.bosch.pt.csm.cloud.usermanagement.common.messages.GenderEnumAvro.FEMALE
import com.bosch.pt.csm.cloud.usermanagement.common.messages.GenderEnumAvro.MALE
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitAnnouncementUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitTestAdminUserAndActivate
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUser
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitUserTombstones
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserEventEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getCreatedDate
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import com.bosch.pt.csm.common.AbstractListenerIntegrationTest
import com.bosch.pt.csm.company.company.asCompanyId
import com.bosch.pt.csm.company.employee.asEmployeeId
import com.bosch.pt.csm.user.testdata.submitUserCreatedUnregistered
import com.bosch.pt.csm.common.util.getIdentifier
import com.bosch.pt.csm.common.util.toUUID
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EmployableUserProjectionIntegrationTest : AbstractListenerIntegrationTest() {

  @BeforeEach
  fun submitSystemUser() {
    eventStreamGenerator.submitSystemUserAndActivate()
  }

  @Nested
  inner class `Verify user context events` {

    @Test
    fun `user created event for system user is not projected`() {
      verifyProjectionDoesNotExistsForUser(getIdentifier("system").asUserId())
    }

    @Test
    fun `user created event for testadmin user is not projected`() {
      eventStreamGenerator.submitTestAdminUserAndActivate()
      verifyProjectionDoesNotExistsForUser(getIdentifier("admin").asUserId())
    }

    @Test
    fun `user created event for announcement user is not projected`() {
      eventStreamGenerator.submitAnnouncementUserAndActivate()
      verifyProjectionDoesNotExistsForUser(getIdentifier("announcement").asUserId())
    }

    @Test
    fun `user created event creates projection with user data only`() {
      eventStreamGenerator.submitUser("user")
      verifyProjection(get("user")!!, null, null)
    }

    @Test
    fun `user created event updates projection with user data when received after employee event`() {
      val userAggregateIdentifier = randomUserAggregateIdentifier()
      eventStreamGenerator
          .submitCompany()
          .submitEmployee { it.user = userAggregateIdentifier }
          .submitUser("user") { it.aggregateIdentifier = userAggregateIdentifier }
      verifyProjection(get("user")!!, get("company")!!, get("employee")!!)
    }

    @Test
    fun `user updated event updates user data of effected projection`() {
      eventStreamGenerator.submitUserCreated().submitUserUpdated()
      verifyProjection(get("user")!!, null, null)
    }

    @Test
    fun `user registered event updated user data of effected projection`() {
      eventStreamGenerator.submitUserCreatedUnregistered().submitUserRegistered()
      verifyProjection(get("user")!!, null, null)
    }

    @Test
    fun `user deleted event (tombstone) updates projection successfully`() {
      eventStreamGenerator.submitUserCreated()
      verifyProjectionExistsForUser(get<UserAggregateAvro>("user")!!.getIdentifier().asUserId())
      eventStreamGenerator.submitUserTombstones("user")
      verifyProjectionDoesNotExistsForUser(getIdentifier("user").asUserId())
    }
  }

  @Nested
  inner class `Verify company context events` {

    @Test
    fun `employee created event updates effected projection with company and employee information`() {
      eventStreamGenerator.submitUser("user").submitCompany().submitEmployee()

      verifyProjection(get("user")!!, get("company")!!, get("employee")!!)
    }

    @Test
    fun `employee deleted event removes company and employee information from effected projection`() {
      eventStreamGenerator
          .submitUser("user")
          .submitCompany()
          .submitEmployee()
          .submitEmployee(eventType = EmployeeEventEnumAvro.DELETED)

      verifyProjection(get("user")!!, null, null)

      eventStreamGenerator.repeat(3)

      verifyProjection(get("user")!!, null, null)
    }

    @Test
    fun `employee deleted event removes projection if not user data exists`() {
      val userAggregateIdentifier = randomUserAggregateIdentifier()
      eventStreamGenerator
          .submitCompany()
          .submitEmployee { it.user = userAggregateIdentifier }
          .submitEmployee(eventType = EmployeeEventEnumAvro.DELETED)

      get<EmployeeAggregateAvro>("employee")!!.user.identifier.toUUID().asUserId().apply {
        verifyProjectionDoesNotExistsForUser(this)
      }

      eventStreamGenerator.repeat(2)

      get<EmployeeAggregateAvro>("employee")!!.user.identifier.toUUID().asUserId().apply {
        verifyProjectionDoesNotExistsForUser(this)
      }
    }

    @Test
    fun `employee deleted event does nothing if a second employee created event was received`() {
      // This scenario can happen when event from different partitions are received and a user
      // was removed from company A and added to Company B
      eventStreamGenerator
          .submitUser("user")
          .submitCompany(asReference = "CompanyA")
          .submitEmployee(asReference = "EmployeeA")
          .submitCompany(asReference = "CompanyB")
          .submitEmployee(asReference = "EmployeeB")
          .submitEmployee(asReference = "EmployeeA", eventType = EmployeeEventEnumAvro.DELETED)

      verifyProjection(get("user")!!, get("CompanyB")!!, get("EmployeeB")!!)

      eventStreamGenerator.repeat(2)

      verifyProjection(get("user")!!, get("CompanyB")!!, get("EmployeeB")!!)
    }

    @Test
    fun `employee created event does nothing if a newer event was already received`() {
      // This scenario can happen when event from different partitions are received and a user
      // was removed from company A and added to Company B
      eventStreamGenerator
          .submitUser("user")
          .submitCompany(asReference = "CompanyB")
          .submitEmployee(asReference = "EmployeeB")
          .submitCompany(asReference = "CompanyA", time = Instant.now().minus(3, DAYS))
          .submitEmployee(asReference = "EmployeeA", time = Instant.now().minus(2, DAYS))
          .submitEmployee(
              asReference = "EmployeeA",
              time = Instant.now().minus(1, DAYS),
              eventType = EmployeeEventEnumAvro.DELETED)

      verifyProjection(get("user")!!, get("CompanyB")!!, get("EmployeeB")!!)
    }

    @Test
    fun `company updated event with changed name updates all effected projections`() {

      val newCompanyName = "Changed Name of Company 1"

      eventStreamGenerator
          .submitCompany("company1")
          .submitUser("company1user1")
          .submitEmployee("company1employee1")
          .submitUser("company1user2")
          .submitEmployee("company1employee2")
          .submitCompany("company2")
          .submitUser("company2user1")
          .submitEmployee("company2employee1")
          .submitCompany("company1", eventType = CompanyEventEnumAvro.UPDATED) {
            it.name = newCompanyName
          }

      verifyCompanyNameUpdated(get("company1")!!)

      eventStreamGenerator.repeat(4)

      verifyCompanyNameUpdated(get("company1")!!)
    }

    @Test
    fun `company delete event remove company name from company name table`() {

      eventStreamGenerator.submitCompany()

      repositories.employableUserProjectionCompanyNameRepository
          .findOneById(getByReference("company").identifier.toUUID().asCompanyId())
          .apply { assertThat(this).isNotNull }

      eventStreamGenerator.submitCompany(eventType = CompanyEventEnumAvro.DELETED)

      repositories.employableUserProjectionCompanyNameRepository
          .findOneById(getByReference("company").identifier.toUUID().asCompanyId())
          .apply { assertThat(this).isNull() }
    }

    private fun verifyCompanyNameUpdated(companyAggregate: CompanyAggregateAvro) {
      repositories.employableUserProjectionRepository
          .findAllByCompanyIdentifier(companyAggregate.getIdentifier().asCompanyId())
          .forEach { assertThat(it.companyName).isEqualTo(companyAggregate.name) }
    }
  }

  private fun EventStreamGenerator.submitUserCreated() =
      this.submitUser("user") {
        it.firstName = "Max"
        it.lastName = "Mustermann"
        it.email = "max@mustermann.de"
        it.gender = MALE
        it.admin = false
        it.locked = false
      }

  private fun EventStreamGenerator.submitUserRegistered() =
      this.submitUser("user", eventType = UserEventEnumAvro.REGISTERED) {
        it.firstName = "Max"
        it.lastName = "Mustermann"
        it.registered = true
        it.gender = MALE
        it.admin = false
        it.locked = false
      }

  private fun EventStreamGenerator.submitUserUpdated() =
      this.submitUser("user", eventType = UserEventEnumAvro.UPDATED) {
        it.firstName = "Maja"
        it.lastName = "Musterfrau"
        it.email = "maja@musterfrau.de"
        it.gender = FEMALE
        it.admin = true
        it.locked = true
      }

  private fun verifyProjectionExistsForUser(identifier: UserId) {
    repositories.employableUserProjectionRepository.findById(identifier).apply {
      assertThat(this.isPresent).isTrue()
    }
  }

  private fun verifyProjectionDoesNotExistsForUser(identifier: UserId) {
    repositories.employableUserProjectionRepository.findById(identifier).apply {
      assertThat(this.isEmpty).isTrue()
    }
  }

  private fun verifyProjection(
      userAggregate: UserAggregateAvro,
      companyAggregate: CompanyAggregateAvro?,
      employeeAggregate: EmployeeAggregateAvro?
  ) {
    repositories.employableUserProjectionRepository
        .findById(userAggregate.getIdentifier().asUserId())
        .get()
        .apply {
          assertThat(this.id).isEqualTo(userAggregate.getIdentifier().asUserId())
          assertThat(this.firstName).isEqualTo(userAggregate.firstName)
          assertThat(this.lastName).isEqualTo(userAggregate.lastName)
          assertThat(this.userName)
              .isEqualTo("${userAggregate.firstName} ${userAggregate.lastName}")
          assertThat(this.email).isEqualTo(userAggregate.email)
          assertThat(this.admin).isEqualTo(userAggregate.admin)
          assertThat(this.locked).isEqualTo(userAggregate.locked)
          assertThat(this.gender?.name).isEqualTo(userAggregate.gender.name)
          assertThat(this.userCreatedDate?.toInstant()).isEqualTo(userAggregate.getCreatedDate())
          assertThat(this.companyIdentifier)
              .isEqualTo(companyAggregate?.getIdentifier()?.asCompanyId())
          assertThat(this.companyName).isEqualTo(companyAggregate?.name)
          assertThat(this.employeeIdentifier)
              .isEqualTo(employeeAggregate?.getIdentifier()?.asEmployeeId())
        }
  }

  private fun randomUserAggregateIdentifier() =
      AggregateIdentifierAvro.newBuilder()
          .setType(USER.name)
          .setIdentifier(randomUUID().toString())
          .setVersion(0L)
          .build()
}
