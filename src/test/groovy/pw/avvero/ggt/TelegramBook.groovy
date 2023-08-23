package pw.avvero.ggt

class TelegramBook {

    static def sendMessage(String patch) {
        def template = """{
          "message": {
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
