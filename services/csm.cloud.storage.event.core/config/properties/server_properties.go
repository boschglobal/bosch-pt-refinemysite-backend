package properties

type ServerProperties struct {
	Port int `validate:"required"`
}
