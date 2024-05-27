package consumer

import (
	"context"
	"csm.cloud.image.scale/config/properties"
	"csm.cloud.image.scale/domain"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/app"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/consumer"
	consumerConfigurer "dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/consumer/configurer"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/consumer/deserializer/avro"
	"fmt"
	"reflect"
)

func Listen(properties properties.KafkaProperties,
	deserializers []avro.AvroTypeDeserializer, callback func(event Event) error) {
	deserializer := avro.NewAvroDeserializer(deserializers)

	kafkaConsumer := consumerConfigurer.ConfigureKafkaConsumer(properties.Broker, properties.Consumer)
	listener := consumer.NewSynchronousKafkaConsumer(kafkaConsumer, deserializer)
	err := listener.Consume(properties.Consumer, []string{properties.Topic.Uploaded.Name}, func(record kafka.Record) error {
		tracingContext := record.Ctx
		deserializedMessage := record.Message
		if reflect.TypeOf(deserializedMessage.Key) != reflect.TypeOf(domain.StringMessageKey{}) {
			panic(app.NewFatalError(fmt.Sprintf("Unsupported message key found: %s", reflect.TypeOf(deserializedMessage.Key).Name()), nil))
		}
		if reflect.TypeOf(deserializedMessage.Value) != reflect.TypeOf(domain.FileCreatedEvent{}) {
			panic(app.NewFatalError(fmt.Sprintf("Unsupported message value found: %s", reflect.TypeOf(deserializedMessage.Key).Name()), nil))
		}

		key := deserializedMessage.Key.(domain.StringMessageKey)
		event := deserializedMessage.Value.(domain.FileCreatedEvent)
		return callback(Event{
			Ctx:   tracingContext,
			Key:   key,
			Event: event,
		})
	})
	if err != nil {
		panic(app.NewFatalError("Failed to register kafka message listener", err))
	}
}

type Event struct {
	Ctx   context.Context
	Key   domain.StringMessageKey
	Event domain.FileCreatedEvent
}
