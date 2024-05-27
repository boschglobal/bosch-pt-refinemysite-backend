package avro

import (
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/producer/serializer/avro"
	"errors"
	"fmt"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"testing"
)

func TestNewAvroDeserializer(t *testing.T) {

	// prepare
	valueSchema := createTestSchema()
	typeDeserializer := NewAvroTypeDeserializer[FileCreatedEvent](valueSchema)
	cut := NewAvroDeserializer([]AvroTypeDeserializer{typeDeserializer})

	serializer := avro.NewAvroSerializer(valueSchema)
	binary, err := serializer.Serialize(&FileCreatedEvent{
		Identifier: "hui",
	})
	assert.Nil(t, err)

	// execute
	value, err := cut.Deserialize(binary)

	// validate
	assert.Nil(t, err)
	assert.Equal(t, "hui", value.(FileCreatedEvent).Identifier)
}

func TestNewAvroDeserializer_NoHandlingTypeDeserializer(t *testing.T) {

	// prepare
	cut := NewAvroDeserializer([]AvroTypeDeserializer{})

	valueSchema := createTestSchema()
	serializer := avro.NewAvroSerializer(valueSchema)
	binary, err := serializer.Serialize(&FileCreatedEvent{
		Identifier: "hui",
	})
	assert.Nil(t, err)

	// execute
	value, err := cut.Deserialize(binary)

	// validate
	assert.Nil(t, value)
	assert.Equal(t, fmt.Sprint("no serializer found to deserialize data with schema id: %i", valueSchema.ID()), err.Error())
}

type AvroTypeDeserializerMock struct {
	mock.Mock
}

func (a *AvroTypeDeserializerMock) Handles(schemaId int) bool {
	args := a.Called(schemaId)
	return args.Get(0).(bool)
}

func (a *AvroTypeDeserializerMock) Deserialize(data []byte) (any, error) {
	args := a.Called(data)
	return args.Get(0), args.Error(1)
}

func TestNewAvroDeserializer_DeserializationError(t *testing.T) {

	// prepare
	deserializerMock := &AvroTypeDeserializerMock{}
	deserializerMock.On("Handles", mock.Anything).Return(true)
	deserializerMock.On("Deserialize", mock.Anything).Return(nil, errors.New("couldn't deserialize event"))

	cut := NewAvroDeserializer([]AvroTypeDeserializer{deserializerMock})

	valueSchema := createTestSchema()
	serializer := avro.NewAvroSerializer(valueSchema)
	binary, err := serializer.Serialize(&FileCreatedEvent{
		Identifier: "hui",
	})
	assert.Nil(t, err)

	// execute
	value, err := cut.Deserialize(binary)

	// validate
	assert.Nil(t, value)
	assert.Equal(t, "couldn't deserialize event", err.Error())
}
