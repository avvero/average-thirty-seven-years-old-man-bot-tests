package pw.avvero.ggt.skill

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate
import pw.avvero.ggt.SequenceGenerator
import pw.avvero.ggt.TelegramBook
import pw.avvero.ggt.WiredRequestCaptor
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.springframework.http.HttpStatus.OK

@SpringBootTest
@ActiveProfiles(profiles = "test")
class SkillTests extends Specification {

    @Autowired
    RestTemplate restTemplate
    @Shared
    WireMockServer telegramStub
    @Shared
    WireMockServer openAiStub

    def botHost = "http://localhost:8080"
    def user1 = [id: "10001", username: "user1"]
    def user2 = [id: "10002", username: "user2"]
    def chat1 = [id: "0", title: "Guild"]

    def setupSpec() {
        telegramStub = new WireMockServer(WireMockConfiguration.options()
                .maxRequestJournalEntries(100)
                .port(10080))
        telegramStub.start()
        openAiStub = new WireMockServer(WireMockConfiguration.options()
                .maxRequestJournalEntries(100)
                .port(10100))
        openAiStub.start()
    }

    @Unroll
    def "Bot answers #response on #message"() {
        setup:
        StubMapping sendMessageMapping = telegramStub.stubFor(post(urlPathMatching("/bottoken/sendMessage"))
                .willReturn(okJson("""{}""")))
        def sendMessageRequestCaptor = new WiredRequestCaptor(telegramStub, sendMessageMapping)
        when:
        def sequence = SequenceGenerator.getNext(["messageId"])
        def request = TelegramBook.sendMessage("""{
          "message": {
            "message_id": $sequence.messageId,
            "from": {
              "id": $user1.id,
              "username": "$user1.username"
            },
            "chat": {
              "id": "$chat1.id",
              "title": "$chat1.title"
            },
            "text": "$message"
          }
        }""")
        def postResponse = restTemplate.postForEntity("$botHost/main", request, Map.class)
        then:
        postResponse.statusCode == OK
        and:
        JSONAssert.assertEquals("""{
          "chat_id": "$chat1.id",
          "reply_to_message_id": "$sequence.messageId",
          "text": "$response"
        }""", sendMessageRequestCaptor.bodyString, false) // actual can contain more fields than expected
        cleanup:
        telegramStub.resetAll()
        openAiStub.resetAll()
        where:
        message | response
        "gg"    | "gg"
    }

    @Unroll
    def "Bot answers on poebot"() {
        setup:
        def botMessage = SequenceGenerator.getNext(["messageId"])
        StubMapping botMessageMapping = telegramStub.stubFor(post(urlPathMatching("/bottoken/sendMessage"))
                .willReturn(okJson("""{
            "ok": true,
            "result": {
                "message_id": $botMessage.messageId
            }
        }""")))
        def botMessageMappingCaptor = new WiredRequestCaptor(telegramStub, botMessageMapping)

        openAiStub.stubFor(post(urlPathMatching("/v1/chat/completions"))
                .willReturn(okJson("""
        {
          "choices": [
            {
              "message": {
                "role": "assistant",
                "content": "Привет"
              }
            }
          ]
        }""")))
        when:
        def userMessage1 = SequenceGenerator.getNext(["messageId"])
        def request = TelegramBook.sendMessage("""{
          "message": {
            "message_id": $userMessage1.messageId,
            "from": {
              "id": $user1.id,
              "username": "$user1.username"
            },
            "chat": {
              "id": "$chat1.id",
              "title": "$chat1.title"
            },
            "text": "поебот привет"
          }
        }""")
        def postResponse = restTemplate.postForEntity("$botHost/main", request, Map.class)
        then:
        postResponse.statusCode == OK
        and:
        JSONAssert.assertEquals("""{
          "chat_id": "$chat1.id",
          "reply_to_message_id": "$userMessage1.messageId",
          "text": "Привет"
        }""", botMessageMappingCaptor.bodyString, false) // actual can contain more fields than expected
        cleanup:
        telegramStub.resetAll()
        openAiStub.resetAll()
    }

    @Unroll
    def "Bot answers on digest"() {
        setup:
        def botMessage = SequenceGenerator.getNext(["messageId"])
        StubMapping botMessageMapping = telegramStub.stubFor(post(urlPathMatching("/bottoken/sendMessage"))
                .willReturn(okJson("""
        {
            "ok": true,
            "result": {
                "message_id": $botMessage.messageId
            }
        }""")))
        def botMessageMappingCaptor = new WiredRequestCaptor(telegramStub, botMessageMapping)

        StubMapping openAiMessageMapping = openAiStub.stubFor(post(urlPathMatching("/v1/chat/completions"))
                .willReturn(okJson("""
        {
          "choices": [
            {
              "message": {
                "role": "assistant",
                "content": "Вот краткое содержание переписки"
              }
            }
          ]
        }""")))
        def openAiMessageMappingCaptor = new WiredRequestCaptor(openAiStub, openAiMessageMapping)
        when:
        def userMessage1 = SequenceGenerator.getNext(["messageId"])
        def request = TelegramBook.sendMessage("""{
          "message": {
            "message_id": $userMessage1.messageId,
            "from": {
              "id": $user1.id,
              "username": "$user1.username"
            },
            "chat": {
              "id": "$chat1.id",
              "title": "$chat1.title"
            },
            "text": "раз"
          }
        }""")
        def postResponse = restTemplate.postForEntity("$botHost/main", request, Map.class)
        Thread.sleep(200)
        then:
        postResponse.statusCode == OK
        and:
        def userMessage2 = SequenceGenerator.getNext(["messageId"])
        def request2 = TelegramBook.sendMessage("""{
          "message": {
            "message_id": $userMessage2.messageId,
            "from": {
              "id": $user2.id,
              "username": "$user2.username"
            },
            "chat": {
              "id": "$chat1.id",
              "title": "$chat1.title"
            },
            "text": "два"
          }
        }""")
        def postResponse2 = restTemplate.postForEntity("$botHost/main", request2, Map.class)
        Thread.sleep(200)
        then:
        postResponse2.statusCode == OK
        and:
        def userMessage3 = SequenceGenerator.getNext(["messageId"])
        def request3 = TelegramBook.sendMessage("""{
          "message": {
            "message_id": $userMessage3.messageId,
            "from": {
              "id": $user1.id,
              "username": "$user1.username"
            },
            "chat": {
              "id": "$chat1.id",
              "title": "$chat1.title"
            },
            "text": "дайджест"
          }
        }""")
        def postResponse3 = restTemplate.postForEntity("$botHost/main", request3, Map.class)
        then:
        postResponse3.statusCode == OK
        and:
        Thread.sleep(1000)
        and:
        openAiMessageMappingCaptor.times == 1
        JSONAssert.assertEquals("""{
          "model": "gpt-3.5-turbo-16k",
          "messages": [{
            "role": "user", 
            "content": "Сделай на русском пересказ переписки. Пересказ должен быть информативным, количество слов 200-300. Сделай выводы по основным моментам обсуждений. Переписка представлена ниже:\\nuser1: раз\\nuser2: два\\nuser1: дайджест"
          }]
        }""", openAiMessageMappingCaptor.bodyString, false) // actual can contain more fields than expected
        and:
        botMessageMappingCaptor.times == 1
        JSONAssert.assertEquals("""{
          "chat_id": "$chat1.id",
          "reply_to_message_id": "$userMessage1.messageId",
          "text": "Вот краткое содержание переписки"
        }""", botMessageMappingCaptor.bodyString, false) // actual can contain more fields than expected
        cleanup:
        telegramStub.resetAll()
        openAiStub.resetAll()
    }

    @Unroll
    def "Conversation test"() {
        setup:
        StubMapping sendMessageMapping = telegramStub.stubFor(post(urlPathMatching("/bottoken/sendMessage"))
                .willReturn(okJson("""{}""")))
        def sendMessageRequestCaptor = new WiredRequestCaptor(telegramStub, sendMessageMapping)
        when:
        def sequence = SequenceGenerator.getNext(["messageId"])
        def request = TelegramBook.sendMessage("""{
          "message": {
            "message_id": $sequence.messageId,
            "from": {
              "id": $user1.id,
              "username": "$user1.username"
            },
            "chat": {
              "id": "$chat1.id",
              "title": "$chat1.title"
            },
            "text": "Message1"
          }
        }""")
        def postResponse = restTemplate.postForEntity("$botHost/main", request, Map.class)
        then:
        postResponse.statusCode == OK
        and:
        JSONAssert.assertEquals("""{
          "chat_id": "$chat1.id",
          "reply_to_message_id": "$sequence.messageId",
          "text": "$response"
        }""", sendMessageRequestCaptor.bodyString, false) // actual can contain more fields than expected
        where:
        message | response
        "gg"    | "gg"
    }
}
