var joinMessages = ['+']; // Случайные сообщения при входе в чат, напр: ['+', 'Добрый вечер!'])

// эта функция вызывается при успешном подключении бота к чату
function onConnected(chatConnection) {
	// отправляем одно рандомное сообщение из списка joinMessages
	var joinMessage = joinMessages[Math.floor(Math.random() * joinMessages.length)]
	chatConnection.sendMessage(joinMessage)
}
