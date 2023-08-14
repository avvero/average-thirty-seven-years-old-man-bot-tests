package pw.avvero.ggt

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.client.RestTemplate
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.springframework.http.HttpStatus.OK

@SpringBootTest
class ProcessingTests extends Specification {

    @Shared
    RestTemplate restTemplate = new RestTemplate()
    @Shared
    WireMockServer telegramStub

    def botHost = "http://localhost:8080"
    def user1 = [id: "10001", username: "user1"]
    def chat1 = [id: "0", title: "Guild"]

    def setup() {
        def config = WireMockConfiguration.options()
                .maxRequestJournalEntries(100)
                .port(10080)
        telegramStub = new WireMockServer(config)
        telegramStub.start()
    }

    @Unroll
    def "Bot answers #response on #message"() {
        setup:
        StubMapping sendMessageMapping = telegramStub.stubFor(post(urlPathMatching("/bottoken/sendMessage"))
                .willReturn(okJson("""{}""")))
        def sendMessageRequestCaptor = new WiredRequestCaptor(telegramStub, sendMessageMapping)
        when:
        def request = """{
          "update_id": 1000,
          "message": {
            "message_id": 50713,
            "from": {
              "id": $user1.id,
              "is_bot": false,
              "username": "$user1.username",
              "language_code": "ru"
            },
            "chat": {
              "id": "$chat1.id",
              "title": "$chat1.title",
              "type": "supergroup"
            },
            "date": 1660400417,
            "text": "$message"
          }
        }""" as String
        def postResponse = restTemplate.postForEntity("$botHost/main", request, Map.class)
        then:
        postResponse.statusCode == OK
        and:
        JSONAssert.assertEquals("""{
          "chat_id": "$chat1.id",
          "reply_to_message_id": "50713",
          "text": "$response"
        }""", sendMessageRequestCaptor.bodyString, false) // actual can contain more fields than expected
        where:
        message | response
        "gg"    | "gg"
    }

}
