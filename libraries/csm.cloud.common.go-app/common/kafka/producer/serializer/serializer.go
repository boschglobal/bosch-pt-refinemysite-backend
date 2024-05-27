package serializer

/*
Serializer interface allows to serialize any event according to a schema
*/
type Serializer interface {

	/*
		Serialize data.
	*/
	Serialize(data any) ([]byte, error)
}
