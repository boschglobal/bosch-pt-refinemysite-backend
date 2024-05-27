package schema_registry

import (
	"csm.cloud.image.scale/config/properties"
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

func (this *defaultSchemaRegistryClient) LoadSchemas() KnownSchemas {
	// Load key schema file
	messageKeyFile, err := os.ReadFile(this.kafkaProperties.Schema.Key.SchemaFile)
	if err != nil {
		panic(err)
	}

	// Load string key schema file
	stringMessageKeyFile, err := os.ReadFile(this.kafkaProperties.Schema.StringKey.SchemaFile)
	if err != nil {
		panic(err)
	}

	// Read value schema file
	fileCreatedEventFile, err := os.ReadFile(this.kafkaProperties.Schema.Uploaded.SchemaFile)
	if err != nil {
		panic(err)
	}

	// Read value schema file
	imageDeletedEventFile, err := os.ReadFile(this.kafkaProperties.Schema.Deleted.SchemaFile)
	if err != nil {
		panic(err)
	}

	// Read value schema file
	imageScaledEventFile, err := os.ReadFile(this.kafkaProperties.Schema.Scaled.SchemaFile)
	if err != nil {
		panic(err)
	}

	// Load key schema
	var messageKeySchema *srclient.Schema
	err = retry.SimpleRetry(
		func() error {
			messageKeySchema, err = this.schemaRegistryService.LoadAvroSchema(this.kafkaProperties.Schema.Key.SchemaSubject, messageKeyFile)
			return err
		}, 20, 1*time.Second, "loading message key schema")
	if err != nil {
		panic(err)
	}

	// Load string key schema
	var stringMessageKeySchema *srclient.Schema
	err = retry.SimpleRetry(
		func() error {
			stringMessageKeySchema, err = this.schemaRegistryService.LoadAvroSchema(this.kafkaProperties.Schema.StringKey.SchemaSubject, stringMessageKeyFile)
			return err
		}, 20, 1*time.Second, "loading string message key schema")
	if err != nil {
		panic(err)
	}

	// Load file created event schema
	var fileCreatedEventSchema *srclient.Schema
	err = retry.SimpleRetry(func() error {
		fileCreatedEventSchema, err = this.schemaRegistryService.LoadAvroSchema(this.kafkaProperties.Schema.Uploaded.SchemaSubject, fileCreatedEventFile)
		return err
	}, 20, 1*time.Second, "loading file created event schema")
	if err != nil {
		panic(err)
	}

	var imageDeletedEventSchema *srclient.Schema
	err = retry.SimpleRetry(func() error {
		imageDeletedEventSchema, err = this.schemaRegistryService.LoadAvroSchema(this.kafkaProperties.Schema.Deleted.SchemaSubject, imageDeletedEventFile)
		return err
	}, 20, 1*time.Second, "loading image deleted event schema")
	if err != nil {
		panic(err)
	}

	// Load image scaled created event schema
	var imageScaledEventSchema *srclient.Schema
	err = retry.SimpleRetry(func() error {
		imageScaledEventSchema, err = this.schemaRegistryService.LoadAvroSchema(this.kafkaProperties.Schema.Scaled.SchemaSubject, imageScaledEventFile)
		return err
	}, 20, 1*time.Second, "loading image scaled event schema")
	if err != nil {
		panic(err)
	}

	return KnownSchemas{
		FileCreatedEvent:  *fileCreatedEventSchema,
		ImageDeletedEvent: *imageDeletedEventSchema,
		ImageScaledEvent:  *imageScaledEventSchema,
		StringMessageKey:  *stringMessageKeySchema,
		MessageKey:        *messageKeySchema,
	}
}
