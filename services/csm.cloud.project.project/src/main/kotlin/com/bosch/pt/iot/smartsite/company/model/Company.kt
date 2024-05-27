/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.company.model

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverageGenerated
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.companymanagement.common.CompanymanagementAggregateTypeEnum.COMPANY
import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyAggregateAvro
import com.bosch.pt.iot.smartsite.common.model.AbstractReplicatedEntity
import com.bosch.pt.iot.smartsite.common.util.returnUnit
import com.bosch.pt.iot.smartsite.user.model.User
import jakarta.persistence.AssociationOverride
import jakarta.persistence.AssociationOverrides
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import java.util.UUID
import org.apache.commons.lang3.builder.ToStringBuilder

@Entity
@AssociationOverrides(
    AssociationOverride(
        name = "createdBy",
        joinColumns = [JoinColumn(nullable = false)],
        foreignKey = ForeignKey(name = "FK_Company_CreatedBy")),
    AssociationOverride(
        name = "lastModifiedBy",
        joinColumns = [JoinColumn(nullable = false)],
        foreignKey = ForeignKey(name = "FK_Company_LastModifiedBy")))
@Table(indexes = [Index(name = "UK_Company_Identifier", columnList = "identifier", unique = true)])
class Company : AbstractReplicatedEntity<Long> {

  @OneToMany(fetch = LAZY, mappedBy = "company", targetEntity = Employee::class)
  var employees: Set<Employee> = HashSet()

  @field:Size(max = MAX_NAME_LENGTH) @Column(length = MAX_NAME_LENGTH) var name: String? = null

  @Embedded
  @AttributeOverrides(
      AttributeOverride(name = "street", column = Column(name = "streetAddress_street")),
      AttributeOverride(name = "houseNumber", column = Column(name = "streetAddress_houseNumber")),
      AttributeOverride(name = "city", column = Column(name = "streetAddress_city")),
      AttributeOverride(name = "zipCode", column = Column(name = "streetAddress_zipCode")),
      AttributeOverride(name = "area", column = Column(name = "streetAddress_area")),
      AttributeOverride(name = "country", column = Column(name = "streetAddress_country")))
  var streetAddress: StreetAddress? = null

  @Embedded
  @AttributeOverrides(
      AttributeOverride(name = "postBox", column = Column(name = "postBoxAddress_postBox")),
      AttributeOverride(name = "city", column = Column(name = "postBoxAddress_city")),
      AttributeOverride(name = "zipCode", column = Column(name = "postBoxAddress_zipCode")),
      AttributeOverride(name = "area", column = Column(name = "postBoxAddress_area")),
      AttributeOverride(name = "country", column = Column(name = "postBoxAddress_country")))
  var postBoxAddress: PostBoxAddress? = null

  @Column(nullable = false) var deleted = false

  constructor()

  constructor(identifier: UUID? = null, version: Long? = null, name: String?) {
    this.identifier = identifier
    this.version = version
    this.name = name
    deleted = false
  }

  public override fun setId(id: Long?) = super.setId(id)

  @ExcludeFromCodeCoverageGenerated
  override fun toString(): String =
      ToStringBuilder(this).appendSuper(super.toString()).append("name", name).toString()

  override fun getDisplayName(): String? = name

  override fun getAggregateType(): String = COMPANY.value

  companion object {

    private const val serialVersionUID: Long = -3548800021806772122

    const val MAX_NAME_LENGTH = 100

    @JvmStatic
    fun fromAvroMessage(
        aggregate: CompanyAggregateAvro,
        createdBy: User?,
        lastModifiedBy: User?
    ): Company =
        Company(
                aggregate.getAggregateIdentifier().getIdentifier().toUUID(),
                aggregate.getAggregateIdentifier().getVersion(),
                aggregate.getName())
            .apply {
              postBoxAddress(this, aggregate)
              streetAddress(this, aggregate)
              setCreatedBy(createdBy)
              setLastModifiedBy(lastModifiedBy)
              setCreatedDate(
                  aggregate.getAuditingInformation().getCreatedDate().toLocalDateTimeByMillis())
              setLastModifiedDate(
                  aggregate
                      .getAuditingInformation()
                      .getLastModifiedDate()
                      .toLocalDateTimeByMillis())
            }

    private fun postBoxAddress(company: Company, aggregate: CompanyAggregateAvro) =
        aggregate
            .getPostBoxAddress()
            ?.let { postBoxAddressAvro ->
              val postBoxAddress = PostBoxAddress()
              postBoxAddress.area = postBoxAddressAvro.getArea()
              postBoxAddress.city = postBoxAddressAvro.getCity()
              postBoxAddress.country = postBoxAddressAvro.getCountry()
              postBoxAddress.postBox = postBoxAddressAvro.getPostBox()
              postBoxAddress.zipCode = postBoxAddressAvro.getZipCode()
              company.postBoxAddress = postBoxAddress
            }
            .returnUnit()

    private fun streetAddress(company: Company, aggregate: CompanyAggregateAvro) =
        aggregate
            .getStreetAddress()
            ?.let { streetAddressAvro ->
              val streetAddress = StreetAddress()
              streetAddress.area = streetAddressAvro.getArea()
              streetAddress.city = streetAddressAvro.getCity()
              streetAddress.country = streetAddressAvro.getCountry()
              streetAddress.houseNumber = streetAddressAvro.getHouseNumber()
              streetAddress.street = streetAddressAvro.getStreet()
              streetAddress.zipCode = streetAddressAvro.getZipCode()
              company.streetAddress = streetAddress
            }
            .returnUnit()
  }
}
