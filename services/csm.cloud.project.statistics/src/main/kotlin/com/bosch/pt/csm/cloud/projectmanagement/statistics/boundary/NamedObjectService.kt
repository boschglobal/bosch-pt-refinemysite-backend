/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary

import com.bosch.pt.csm.cloud.companymanagement.company.messages.CompanyEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.AggregateType.COMPANY
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.AggregateType.PROJECTCRAFT
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.NamedObject
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.NamedObjectRepository
import com.bosch.pt.csm.cloud.projectmanagement.util.toUUID
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NamedObjectService(private val repository: NamedObjectRepository) {

  @Trace
  @Transactional
  fun saveCompanyName(event: CompanyEventAvro) =
      createOrUpdateNamedObject(
          COMPANY,
          event.getAggregate().getAggregateIdentifier().toUUID(),
          event.getAggregate().getName())

  @Trace
  @Transactional
  fun saveProjectCraftName(event: ProjectCraftEventG2Avro) =
      createOrUpdateNamedObject(
          PROJECTCRAFT,
          event.getAggregate().getAggregateIdentifier().toUUID(),
          event.getAggregate().getName())

  @Trace
  @Transactional(readOnly = true)
  fun findCompanyNames(companyIdentifiers: Collection<UUID>) =
      findNamedObjects(companyIdentifiers, COMPANY)

  @Trace
  @Transactional(readOnly = true)
  fun findProjectCraftNames(craftIdentifiers: Collection<UUID>) =
      findNamedObjects(craftIdentifiers, PROJECTCRAFT)

  @Trace
  @Transactional
  fun deleteAll(identifiers: Collection<ObjectIdentifier>) {
    if (!identifiers.isEmpty()) {
      repository.deleteAllByObjectIdentifierIn(identifiers)
    }
  }

  private fun findNamedObjects(identifiers: Collection<UUID>, type: String) =
      repository.findAllByObjectIdentifierIn(identifiers.map { ObjectIdentifier(type, it) })

  private fun createOrUpdateNamedObject(type: String, identifier: UUID, name: String) {
    val objectIdentifier = ObjectIdentifier(type, identifier)
    val namedObject =
        repository.findOneByObjectIdentifier(objectIdentifier)?.also { it.name = name }
            ?: NamedObject(type, identifier, name)
    repository.save(namedObject)
  }
}
