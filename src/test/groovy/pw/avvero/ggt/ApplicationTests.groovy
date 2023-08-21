package pw.avvero.ggt

import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest
class ApplicationTests extends Specification {

    def "Context loads"(){
        expect:
        org.slf4j.helpers.NOPLoggerFactory
        1 == 1
    }

}
