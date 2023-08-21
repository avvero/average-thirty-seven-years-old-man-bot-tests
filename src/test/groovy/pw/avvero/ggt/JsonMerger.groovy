package pw.avvero.ggt

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class JsonMerger {

    static String merge(String originString, String patchString) {
        def origin = new JsonSlurper().parseText(originString) as Map
        def patch = new JsonSlurper().parseText(patchString) as Map
        def result = merge(origin, patch)
        return JsonOutput.prettyPrint(JsonOutput.toJson(result))
    }

    static Map merge(Map... maps) {
        Map result

        if (maps.length == 0) {
            result = [:]
        } else if (maps.length == 1) {
            result = maps[0]
        } else {
            result = [:]
            maps.each { map ->
                map.each { k, v ->
                    result[k] = result[k] instanceof Map ? merge(result[k], v) : v
                }
            }
        }
        result
    }

    static String mergeWithNull(String originString, String patchString) {
        def origin = new JsonSlurper().parseText(originString) as Map
        def patch = new JsonSlurper().parseText(patchString) as Map
        def result = mergeWithNull(origin, patch)
        return JsonOutput.prettyPrint(JsonOutput.toJson(result))
    }

    static Map mergeWithNull(Map... maps) {
        Map result

        if (maps.length == 0) {
            result = [:]
        } else if (maps.length == 1) {
            result = maps[0]
        } else {
            result = [:]
            maps.each { map ->
                map.each { k, v ->
                    if (v == null) {
                        result[k] = v
                    } else {
                        result[k] = result[k] instanceof Map ? mergeWithNull(result[k], v) : v
                    }
                }
            }
        }
        result
    }

}
