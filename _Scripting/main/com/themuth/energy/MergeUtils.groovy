package com.themuth.energy

import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

class MergeUtils {
    MergeUtils(){

    }

    //void convertData(String iFilename, String iInfo) {
    void convertData(String iFolder, String iWhat) {
        def iFolderName = "../../_data/in/" + iFolder + "/data/"
        def iFolderInfo = "../../_data/in/Info/"
        if (iWhat.contains('Y')) {
            convertToJsonFile("year", iFolderName, iFolderInfo)
        }
        if (iWhat.contains('M')) {
            convertToJsonFile("month", iFolderName, iFolderInfo)
        }
        if (iWhat.contains('D')) {
            convertToJsonFile("day", iFolderName, iFolderInfo)
        }
        if (iWhat.contains('H')) {
            convertToJsonFile("hour", iFolderName, iFolderInfo)
        }
        if (iWhat.contains('m')) {
            convertToJsonFile("minute", iFolderName, iFolderInfo)
        }
    }

    void convertToJsonFile(String iType, String iFolderName, String iFolderInfo) {
        println("Converting: " + iFolderName + " " + iType)
        def iFilename = iFolderName + iType + ".array"
        def iTargetName = iFilename.replace("array","json").replace("/data/","/json/")
        def iInfo = iFolderInfo + iType + ".json"
        def definition = new JsonSlurper().parse(new File(iInfo))
        def aFields = definition.fields
        def file = new File(iFilename)
        def array = new JsonSlurper().parse(file)
        def map = []

        for (int i = 0; i < array.size(); i++){
            def oRow = new Object()
            for (int j = 0; j < aFields.size(); j++){
                def field = aFields[j].name
                oRow.metaClass[field] = array[i][j]
            }
            map.push(oRow)
        }
        def json = new JsonBuilder(map);
        new File(iTargetName).write(json.toPrettyString())
    }
}
