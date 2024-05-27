package configurer

import (
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/app"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/config/properties"
	"fmt"
	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
	"github.com/rs/zerolog/log"
)

func ConfigureKafkaConsumer(brokerProperties properties.BrokerProperties, consumerProperties properties.ConsumerProperties) *kafka.Consumer {

	configMap := createConsumerConfigMap(brokerProperties, consumerProperties)
	consumer, err := kafka.NewConsumer(configMap)
	if err != nil {
		panic(app.NewFatalError(fmt.Sprintf("Failed to create Kafka Consumer bootstrapping with %s",
			brokerProperties.Urls), err))
	}

	app.RegisterShutdownListener(func() {
		log.Info().Msg("Closing Kafka Consumer as shutdown hook was called")
		err = consumer.Close()
		if err != nil {
			log.Error().Msg("Failed to stop kafka consumer")
		}
		log.Info().Msg("Kafka Consumer closed")
	})

	return consumer
}

func createConsumerConfigMap(brokerProperties properties.BrokerProperties, consumerProperties properties.ConsumerProperties) *kafka.ConfigMap {
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
	if consumerProperties.GroupId != "" {
		_ = configMap.SetKey("group.id", consumerProperties.GroupId)
	}
	_ = configMap.SetKey("auto.offset.reset", "earliest")
	_ = configMap.SetKey("isolation.level", "read_committed")
	_ = configMap.SetKey("enable.auto.commit", "false")
	return configMap
}
