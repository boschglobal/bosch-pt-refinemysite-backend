package main

import (
	"csm.cloud.image.scale/config"
	"csm.cloud.image.scale/domain"
	"csm.cloud.image.scale/facade/rest"
	"csm.cloud.image.scale/image"
	"csm.cloud.image.scale/kafka/admin"
	"csm.cloud.image.scale/kafka/consumer"
	"csm.cloud.image.scale/kafka/producer"
	"csm.cloud.image.scale/kafka/schema_registry"
	"csm.cloud.image.scale/storage"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/app"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/datadog"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/http/interceptor/request_host_rewrite"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/consumer/deserializer/avro"
	producerConfigurer "dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/producer/configurer"
	"fmt"
	"github.com/davidbyttow/govips/v2/vips"
	"github.com/rs/zerolog/log"
	"strings"
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

	// Initialize vips image processing and scaling library
	// requires libvips and C compiler as per https://github.com/davidbyttow/govips
	vips.LoggingSettings(nil, vips.LogLevelWarning)
	vips.Startup(nil)
	defer vips.Shutdown()

	quarantineBlobStorageClient := storage.NewBlobStorageClient(configuration.Storage.Quarantine)
	projectBlobStorageClient := storage.NewBlobStorageClient(configuration.Storage.Project)
	userBlobStorageClient := storage.NewBlobStorageClient(configuration.Storage.User)

	// Create topics if needed (on localhost)
	admin.CreateTopicsIfNeeded(configuration)

	// Initialize schema registry client and load schemas
	schemaRegistryClient := schema_registry.NewDefaultSchemaRegistryClient(configuration.Kafka)
	schemas := schemaRegistryClient.LoadSchemas()

	// Initialize kafka producers
	kafkaProducer := producerConfigurer.ConfigureKafkaProducer(configuration.Kafka.Broker)
	imageDeletedEventProducer := producer.NewAvroEventKafkaProducer[domain.MessageKey, domain.ImageDeletedEvent](
		&schemas.MessageKey,
		&schemas.ImageDeletedEvent,
		kafkaProducer,
		configuration.Kafka.Topic.Scaled.Name,
	)
	imageScaledEventProducer := producer.NewAvroEventKafkaProducer[domain.MessageKey, domain.ImageScaledEvent](
		&schemas.MessageKey,
		&schemas.ImageScaledEvent,
		kafkaProducer,
		configuration.Kafka.Topic.Scaled.Name,
	)

	imageEventProcessor := image.NewImageScalingProcessor(quarantineBlobStorageClient, projectBlobStorageClient, userBlobStorageClient, &imageDeletedEventProducer, &imageScaledEventProducer)

	stringMessageKeyDeserializer := avro.NewAvroTypeDeserializer[domain.StringMessageKey](&schemas.StringMessageKey)
	fileCreatedEventDeserializer := avro.NewAvroTypeDeserializer[domain.FileCreatedEvent](&schemas.FileCreatedEvent)

	// Configure kafka consumer and listen asynchronously
	consumer.Listen(configuration.Kafka,
		[]avro.AvroTypeDeserializer{stringMessageKeyDeserializer, fileCreatedEventDeserializer},
		func(record consumer.Event) error {
			tracingContext := record.Ctx
			event := record.Event

			if !strings.HasPrefix(event.Path, "/images") {
				log.Info().Msg(fmt.Sprintf("Ignore file uploaded to other container: %s/%s", event.Path, event.FileName))
				return nil
			}

			// Scale image
			err := imageEventProcessor.ScaleImageWithRetry(tracingContext, event)
			return err
		})

	// Initialize and run the blocking web-server
	webServerRunner := rest.NewWebServerRunner(configuration.Server)
	webServerRunner.Run()
}
