package avro

import (
	"encoding/binary"
	"errors"
	"fmt"
)

type AvroDeserializer struct {
	deserializers []AvroTypeDeserializer
}

func NewAvroDeserializer(deserializers []AvroTypeDeserializer) *AvroDeserializer {
	return &AvroDeserializer{
		deserializers: deserializers,
	}
}

func (this *AvroDeserializer) Deserialize(data []byte) (any, error) {
	// First bit is avro version
	// Second to fifth bit is schema id
	schemaId := int(binary.BigEndian.Uint32(data[1:5]))

	// Sixth bit onwards is the payload
	valueBytes := data[5:]

	// Find handling deserializer and deserialize object
	for _, deserializer := range this.deserializers {
		if deserializer.Handles(schemaId) {
			value, err := deserializer.Deserialize(valueBytes)
			if err != nil {
				return nil, err
			}
			return value, nil
		}
	}
	return nil, errors.New(fmt.Sprint("no serializer found to deserialize data with schema id: %i", schemaId))
}
