package partitioner

/*
Partitioner is an interface used to determine the correct partition number to produce to
*/
type Partitioner interface {
	Partition(topicName string, key any, keyBytes []byte, value any, valueBytes []byte) int32
}
