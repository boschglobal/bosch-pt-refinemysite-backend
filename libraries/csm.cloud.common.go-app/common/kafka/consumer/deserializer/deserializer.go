package deserializer

/*
Deserializer interface allows to deserialize any event according to a schema
*/
type Deserializer interface {

	/*
		Deserialize data.
	*/
	Deserialize([]byte) (any, error)
}
