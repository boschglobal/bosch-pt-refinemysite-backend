package properties

type BrokerProperties struct {
	Address          AddressProperties
	Api              ApiCredentials
	Urls             string `validate:"required"`
	SaslMechanism    string
	SecurityProtocol string
}

type SchemaRegistryProperties struct {
	Api  ApiCredentials
	Urls string `validate:"required"`
}

type AddressProperties struct {
	Family string
}

type ApiCredentials struct {
	Key    string
	Secret string
}

type ConsumerProperties struct {
	GroupId     string `validate:"required"`
	ReadTimeout string `validate:"required"`
}
