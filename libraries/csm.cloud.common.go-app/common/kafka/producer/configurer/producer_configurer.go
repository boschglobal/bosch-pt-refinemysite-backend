package configurer

import (
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/app"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/config/properties"
	"fmt"
	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
	"github.com/rs/zerolog/log"
)

/*
ConfigureKafkaProducer initializes the Kafka Producer which will run until the application is terminated.
Uses shutdown hook to get terminated on Interrupt and SIGTERM signals so that the producer is closed in an orderly
fashion. Fails fast (in panic) upon error.
*/
func ConfigureKafkaProducer(brokerProperties properties.BrokerProperties) *kafka.Producer {

	configMap := createProducerConfigMap(brokerProperties)
	producer, err := kafka.NewProducer(configMap)
	if err != nil {
		panic(app.NewFatalError(fmt.Sprintf("Failed to create Kafka Producer bootstrapping with %s",
			brokerProperties.Urls), err))
	}

	app.RegisterShutdownListener(func() {
		log.Info().Msg("Closing Kafka Producer as shutdown hook was called")
		producer.Close()
		log.Info().Msg("Kafka Producer closed")
	})

	return producer
}

func createProducerConfigMap(brokerProperties properties.BrokerProperties) *kafka.ConfigMap {
	configMap := &kafka.ConfigMap{"bootstrap.servers": brokerProperties.Urls}
	if brokerProperties.SaslMechanism != "" {
		_ = configMap.SetKey("sasl.mechanisms", brokerProperties.SaslMechanism)
	}
	if brokerProperties.SecurityProtocol != "" {
		_ = configMap.SetKey("security.protocol", brokerProperties.SecurityProtocol)
	}
	if brokerProperties.Api.Key != "" {
		_ = configMap.SetKey("sasl.username", brokerProperties.Api.Key)
	}
	if brokerProperties.Api.Secret != "" {
		_ = configMap.SetKey("sasl.password", brokerProperties.Api.Secret)
	}
	if brokerProperties.Address.Family != "" {
		_ = configMap.SetKey("broker.address.family", brokerProperties.Address.Family)
	}
	_ = configMap.SetKey("acks", "all")
	return configMap
}
