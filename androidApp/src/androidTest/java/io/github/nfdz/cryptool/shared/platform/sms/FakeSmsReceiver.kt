package io.github.nfdz.cryptool.shared.platform.sms

class FakeSmsReceiver : SmsReceiver {

    var receivePendingMessageCount = 0
    override fun receivePendingMessage() {
        receivePendingMessageCount++
    }

}