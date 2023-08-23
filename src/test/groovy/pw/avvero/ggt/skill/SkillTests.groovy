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
