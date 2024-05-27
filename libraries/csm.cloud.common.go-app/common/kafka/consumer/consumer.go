package consumer

type Consumer interface {
	Consume(topics []string, callbackChannel chan<- any)
}
