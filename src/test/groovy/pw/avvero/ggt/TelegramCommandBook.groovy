package pw.avvero.ggt

class TelegramCommandBook {

    static def sendMessage(String patch) {
        def template = """{
          "message": {
            "message_id": 50713,
            "from": {
              "id": "#REQUIRED",
              "is_bot": false,
              "username": "#REQUIRED",
              "language_code": "ru"
            },
            "chat": {
              "id": "#REQUIRED",
              "title": "#REQUIRED",
              "type": "supergroup"
            },
            "date": 1660400417,
            "text": "#REQUIRED"
          }
        }""" as String
        return JsonMerger.merge(template, patch)
    }

}