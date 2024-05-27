package main

import (
	"csm.cloud.storage.event.core/config"
	"csm.cloud.storage.event.core/facade/rest"
	"csm.cloud.storage.event.core/kafka/admin"
	"csm.cloud.storage.event.core/kafka/producer"
	"csm.cloud.storage.event.core/kafka/schema-registry"
	"csm.cloud.storage.event.core/storage/messages/get"
	"csm.cloud.storage.event.core/storage/queue"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/app"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/datadog"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/http/interceptor/request_host_rewrite"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/producer/configurer"
	"github.com/rs/zerolog/log"
)

func main() {

	// Initialize global panic handling
	defer app.HandlePanic()

	// Log starting message
	log.Info().Msg("Starting application")

	// Initialize configuration and register HostRewriter in HttpClient
	configuration := config.NewConfiguration()
	request_host_rewrite.RegisterRequestHostRewriter(configuration.HttpClient)

	// Initialize datadog tracer
	datadog.ApplyDefaultTracingConfiguration()

	// Initialize schema registry client and load schemas
	schemaRegistryClient := schema_registry.NewDefaultSchemaRegistryClient(configuration.Kafka)
	keySchema, valueSchema := schemaRegistryClient.LoadSchemas()

	// Create topics if needed (on localhost)
	admin.CreateTopicsIfNeeded(configuration)

	// Initialize kafka producer
	kafkaProducer := configurer.ConfigureKafkaProducer(configuration.Kafka.Broker)
	uploadEventProducer := producer.NewDefaultFileCreatedEventKafkaProducer(
		keySchema,
		valueSchema,
		kafkaProducer,
		configuration.Kafka.Topic.Upload.Name,
	)

	// Initialize azure storage queue listener
	queueConfiguration := queue.NewDefaultConfiguration(configuration.Storage)
	blobInfoService := get.NewDefaultGetBlobInfoService(configuration.Storage)
	storageQueueListener := queue.NewListener(
		&uploadEventProducer,
		blobInfoService,
		queueConfiguration,
	)

	// Initialize storage queue listener
	go app.Run(func() {
		storageQueueListener.Listen()
	})

	// Initialize and run the blocking web-server
	webServerRunner := rest.NewWebServerRunner(configuration.Server)
	webServerRunner.Run()
}
