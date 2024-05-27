package producer

import (
	"csm.cloud.storage.event.core/domain"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/producer/serializer/avro"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/test"
	"errors"
	"github.com/riferrei/srclient"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestAvroSerializer_KeyEmptyValue(t *testing.T) {

	// Initialize key and value serializers
	keySerializer := avro.NewAvroSerializer(createKeySchemata())
	valueSerializer := avro.NewAvroSerializer(createValueSchemata())

	// Create domain event
	event := &domain.FileCreatedEvent{
		Identifier: "hui",
	}

	// Serialize key and value
	key, err := keySerializer.Serialize(domain.NewStringMessageKey(event.Identifier))
	assert.Nil(t, err)
	value, err := valueSerializer.Serialize(event)

	// Verify no error occurred
	assert.Nil(t, err)

	// Verify that serialized result is correct
	expectedKey := []uint8{0, 0, 0, 0, 3, 6, 104, 117, 105}
	expectedVal := []uint8{0, 0, 0, 0, 4, 6, 104, 117, 105, 0, 0, 0, 0}
	assert.Equal(t, expectedKey, key)
	assert.Equal(t, expectedVal, value)
}

func TestAvroSerializer_SetValues(t *testing.T) {

	// Initialize value serializer
	avroSerializer := avro.NewAvroSerializer(createValueSchemata())

	// Serialize a domain event with values
	value, err := avroSerializer.Serialize(&domain.FileCreatedEvent{
		Identifier:    "hui",
		Path:          "/path",
		FileName:      "file.txt",
		ContentLength: 100,
		ContentType:   "text/plain",
	})

	// Verify that the values were serialized correctly to avro
	assert.Nil(t, err)
	expectedVal := []uint8{0, 0, 0, 0, 4, 6, 104, 117, 105, 10, 47, 112, 97, 116, 104, 16, 102, 105, 108, 101, 46, 116,
		120, 116, 20, 116, 101, 120, 116, 47, 112, 108, 97, 105, 110, 200, 1}
	assert.Equal(t, expectedVal, value)
}

func TestAvroSerializer_FailingWrongType(t *testing.T) {

	// Initialize key serializer
	cut := avro.NewAvroSerializer(createKeySchemata())

	// Serialize domain event
	key, err := cut.Serialize(&domain.FileCreatedEvent{
		Identifier: "hui",
	})

	// Verify serialization failed
	assert.IsType(t, errors.New(""), err)
	assert.Nil(t, key)
}

func createValueSchemata() *srclient.Schema {

	// Instantiate schema registry client
	valueSchema := &srclient.Schema{}

	//Set schema via reflection
	test.SetFieldValueForTesting(valueSchema, "schema", "{\"type\":\"record\",\"name\":\"FileCreatedEventAvro\","+
		"\"namespace\":\"com.bosch.pt.csm.cloud.storage.event.messages\",\"fields\":[{\"name\":\"identifier\","+
		"\"type\":{\"type\":\"string\",\"CommonAvroSerializer.java.string\":\"String\"},\"doc\":\"Identifier of the storage event\"},"+
		"{\"name\":\"path\",\"type\":{\"type\":\"string\",\"CommonAvroSerializer.java.string\":\"String\"}},{\"name\":\"filename\","+
		"\"type\":{\"type\":\"string\",\"CommonAvroSerializer.java.string\":\"String\"}},{\"name\":\"contentType\","+
		"\"type\":{\"type\":\"string\",\"CommonAvroSerializer.java.string\":\"String\"}},{\"name\":\"contentLength\","+
		"\"type\":\"long\"}]}")
	test.SetFieldValueForTesting(valueSchema, "id", 4)
	test.SetFieldValueForTesting(valueSchema, "version", 1)

	return valueSchema
}

func createKeySchemata() *srclient.Schema {

	// Instantiate schema registry client
	keySchema := &srclient.Schema{}

	//Set schema via reflection
	test.SetFieldValueForTesting(keySchema, "schema", "{\"type\":\"record\",\"name\":\"StringMessageKeyAvro\","+
		"\"namespace\":\"com.bosch.pt.csm.cloud.common.messages\",\"fields\":[{\"name\":\"identifier\","+
		"\"type\":{\"type\":\"string\",\"CommonAvroSerializer.java.string\":\"String\"}}]}")
	test.SetFieldValueForTesting(keySchema, "id", 3)
	test.SetFieldValueForTesting(keySchema, "version", 1)

	return keySchema
}
