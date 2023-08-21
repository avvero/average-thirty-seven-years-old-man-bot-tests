package pw.avvero.ggt

import groovy.json.JsonOutput
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

class JsonMergerTests extends Specification {

    @Ignore // pretty print spoils all the stuff
    @Unroll
    def "Json patch"() {
        expect:
        JsonMerger.merge(origin, patch) == JsonOutput.prettyPrint(JsonOutput.toJson(result))
        where:
        origin                               | patch                                 || result
        """{}"""                             | """{}"""                              || """{}"""
        """{}"""                             | """{"company":"Apple"}"""             || """{"company":"Apple"}"""
        """{"person":{}"""                   | """{"company":"Apple"}"""             || """{"person":{},"company":"Apple"}"""
        """{"person":{"firstName":"Ivan"}""" | """{"person":{"lastName":"Ivanov"}""" || """{"person":{"firstName":"Ivan","lastName":"Ivanov"}}"""
    }

}
