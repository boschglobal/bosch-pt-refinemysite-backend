package schema_registry

import (
	"csm.cloud.storage.event.core/config/properties"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/schema-registry-client"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/retry"
	"github.com/riferrei/srclient"
	"os"
	"time"
)

type defaultSchemaRegistryClient struct {
	kafkaProperties       properties.KafkaProperties
	schemaRegistryService schema_registry_client.SchemaRegistryService
}

func NewDefaultSchemaRegistryClient(kafkaProperties properties.KafkaProperties) defaultSchemaRegistryClient {
	return defaultSchemaRegistryClient{
		kafkaProperties: kafkaProperties,
		schemaRegistryService: schema_registry_client.NewDefaultSchemaRegistryService(
			kafkaProperties.Schema.AutoRegisterSchemas,
			kafkaProperties.SchemaRegistry,
		),
	}
}

func (this *defaultSchemaRegistryClient) LoadSchemas() (*srclient.Schema, *srclient.Schema) {
	// Load key schema file
	keyFile, err := os.ReadFile(this.kafkaProperties.Schema.Key.SchemaFile)
	if err != nil {
		panic(err)
	}

	// Read value schema file
	valueFile, err := os.ReadFile(this.kafkaProperties.Schema.Upload.SchemaFile)
	if err != nil {
		panic(err)
	}

	// Load key schema
	var keySchema *srclient.Schema
	err = retry.SimpleRetry(
		func() error {
			keySchema, err = this.schemaRegistryService.LoadAvroSchema(this.kafkaProperties.Schema.Key.SchemaSubject, keyFile)
			return err
		}, 20, 1*time.Second, "loading key schema")
	if err != nil {
		panic(err)
	}

	// Load value schema
	var valueSchema *srclient.Schema
	err = retry.SimpleRetry(func() error {
		valueSchema, err = this.schemaRegistryService.LoadAvroSchema(this.kafkaProperties.Schema.Upload.SchemaSubject, valueFile)
		return err
	}, 20, 1*time.Second, "loading value schema")
	if err != nil {
		panic(err)
	}

	return keySchema, valueSchema
}
