package configurer

import (
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/config/properties"
	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestConfigurerKafkaConsumer(t *testing.T) {

	//prepare
	brokerProperties := properties.BrokerProperties{
		Api: properties.ApiCredentials{
			Secret: "apiSecret!",
			Key:    "apiKey",
		},
		Urls:             "my-broker-url",
		SaslMechanism:    "PLAIN",
		SecurityProtocol: "SASL_SSL",
	}
	consumerProperties := properties.ConsumerProperties{
		GroupId: "my-group",
	}

	// execute
	cut := ConfigureKafkaConsumer(brokerProperties, consumerProperties)

	// verify
	assert.IsType(t, &kafka.Consumer{}, cut)
}

func TestConfigureKafkaConsumer_PanicsWhenMisconfigured(t *testing.T) {
	// prepare
	brokerProperties := properties.BrokerProperties{
		Api: properties.ApiCredentials{
			Secret: "apiSecret!",
			Key:    "apiKey",
		},
		Urls:             "my-broker-url",
		SaslMechanism:    "BrokenMechanism",
		SecurityProtocol: "BrokenProtocol",
	}
	consumerProperties := properties.ConsumerProperties{
		GroupId: "my-group",
	}

	// execute and verify
	assert.Panics(t, func() {
		ConfigureKafkaConsumer(brokerProperties, consumerProperties)
	})
}

func TestCreateConsumerConfigMap(t *testing.T) {

	// prepare
	brokerProperties := properties.BrokerProperties{
		Api: properties.ApiCredentials{
			Secret: "apiSecret!",
			Key:    "apiKey",
		},
		Urls:             "my-broker-url",
		SaslMechanism:    "PLAIN",
		SecurityProtocol: "SASL_SSL",
	}
	consumerProperties := properties.ConsumerProperties{
		GroupId: "my-group",
	}

	// execute
	cut := createConsumerConfigMap(brokerProperties, consumerProperties)

	// verify
	assert.Equal(t, "PLAIN", getConfigValue(cut, "sasl.mechanisms"))
	assert.Equal(t, "SASL_SSL", getConfigValue(cut, "security.protocol"))
	assert.Equal(t, "apiKey", getConfigValue(cut, "sasl.username"))
	assert.Equal(t, "apiSecret!", getConfigValue(cut, "sasl.password"))
	assert.Equal(t, "my-broker-url", getConfigValue(cut, "bootstrap.servers"))
	assert.Equal(t, "my-group", getConfigValue(cut, "group.id"))
	assert.Equal(t, "earliest", getConfigValue(cut, "auto.offset.reset"))
	assert.Equal(t, "read_committed", getConfigValue(cut, "isolation.level"))
	assert.Equal(t, "false", getConfigValue(cut, "enable.auto.commit"))
}

func TestCreateConsumerConfigMap_NonSecuredBrokers(t *testing.T) {
	// prepare
	brokerProperties := properties.BrokerProperties{
		Urls: "my-broker-url",
	}
	consumerProperties := properties.ConsumerProperties{
		GroupId: "my-group",
	}

	// execute
	cut := createConsumerConfigMap(brokerProperties, consumerProperties)

	// verify
	assert.Equal(t, nil, getConfigValue(cut, "sasl.mechanisms"))
	assert.Equal(t, nil, getConfigValue(cut, "security.protocol"))
	assert.Equal(t, nil, getConfigValue(cut, "sasl.username"))
	assert.Equal(t, nil, getConfigValue(cut, "sasl.password"))
	assert.Equal(t, "my-broker-url", getConfigValue(cut, "bootstrap.servers"))
	assert.Equal(t, "my-group", getConfigValue(cut, "group.id"))
	assert.Equal(t, "earliest", getConfigValue(cut, "auto.offset.reset"))
	assert.Equal(t, "read_committed", getConfigValue(cut, "isolation.level"))
	assert.Equal(t, "false", getConfigValue(cut, "enable.auto.commit"))
}

func getConfigValue(configMap *kafka.ConfigMap, key string) kafka.ConfigValue {
	val, _ := configMap.Get(key, nil)
	return val
}
