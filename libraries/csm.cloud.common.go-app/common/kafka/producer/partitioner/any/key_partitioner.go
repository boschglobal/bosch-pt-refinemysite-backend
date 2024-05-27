package any

/**
* Contains an implementation of Murmur2 algorithm originally published as
* https://github.com/apache/kafka/blob/1.0.0/clients/src/main/java/org/apache/kafka/common/utils/Utils.java#L353
* licensed under Apache 2.0 license.
 */

import (
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/producer/partitioner"
	"github.com/confluentinc/confluent-kafka-go/v2/kafka"
)

/*
KeyPartitioner is a partitioner that uses the keyBytes attribute for determine the number of the partition.
*/
type KeyPartitioner struct {
	topicPartitions map[string]int32
	partitionFunc   func([]byte) int32
}

/*
NewMurmur2Partitioner creates a Partitioner that uses murmur2 algorithm and the number of partitions per topic
for partitioning
*/
func NewMurmur2Partitioner(topicPartitions map[string]int32) partitioner.Partitioner {
	return &KeyPartitioner{
		topicPartitions: topicPartitions,
		partitionFunc: func(bytes []byte) int32 {
			return ToPositive(Murmur2(bytes))
		},
	}
}

/*
NewAnyPartitioner creates a Partitioner that selects any (undetermined) partition
*/
func NewAnyPartitioner() partitioner.Partitioner {
	return &KeyPartitioner{
		topicPartitions: nil,
		partitionFunc: func(bytes []byte) int32 {
			return kafka.PartitionAny
		},
	}
}

/*
Partition implements Partitioner.Partition
*/
func (this *KeyPartitioner) Partition(topicName string, key any, keyBytes []byte, value any, valueBytes []byte) int32 {
	// If the partitioner doesn't have information about existing partition counts per topic
	// just return the partition as calculated by the partion function
	if this.topicPartitions == nil {
		return this.partitionFunc(keyBytes)
	}
	return this.partitionFunc(keyBytes) % this.topicPartitions[topicName]
}

/*
Murmur2 translation of the algorithm from kafka repo
https://github.com/apache/kafka/blob/1.0.0/clients/src/main/java/org/apache/kafka/common/utils/Utils.java#L353
*/
func Murmur2(data []byte) int32 {
	length := int32(len(data))
	seed := uint32(0x9747b28c)
	m := int32(0x5bd1e995)
	r := uint32(24)

	h := int32(seed ^ uint32(length))
	length4 := length / 4

	for i := int32(0); i < length4; i++ {
		i4 := i * 4
		k := int32(data[i4+0]&0xff) + (int32(data[i4+1]&0xff) << 8) + (int32(data[i4+2]&0xff) << 16) + (int32(data[i4+3]&0xff) << 24)
		k *= m
		k ^= int32(uint32(k) >> r)
		k *= m
		h *= m
		h ^= k
	}

	switch length % 4 {
	case 3:
		h ^= int32(data[(length & ^3)+2]&0xff) << 16
		fallthrough
	case 2:
		h ^= int32(data[(length & ^3)+1]&0xff) << 8
		fallthrough
	case 1:
		h ^= int32(data[length & ^3] & 0xff)
		h *= m
	}

	h ^= int32(uint32(h) >> 13)
	h *= m
	h ^= int32(uint32(h) >> 15)

	return h
}

func ToPositive(value int32) int32 {
	return value & 0x7fffffff
}
