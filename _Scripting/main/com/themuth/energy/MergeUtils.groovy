package com.themuth.energy

import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import groovy.xml.MarkupBuilder
import groovy.json.JsonOutput
import net.sf.saxon.trans.SymbolicName

import java.sql.Array
import java.text.NumberFormat
import groovy.time.TimeCategory
import java.util.Locale

import java.text.DecimalFormat
import java.text.SimpleDateFormat

import jxl.*
import jxl.write.*


class MergeUtils {
    private Map prices = [:]

    MergeUtils() {

    }

    void loadPrices(String iFilename) {
        def inJson = new JsonSlurper().parse(new File(iFilename))
        inJson.each { price ->
            def key = price.datum
            def value = price.prijs_excl_btw
            prices.put(key, value)
        }
    }

    String getPrice(key) {
        def mappedValue = prices.get(key.toString())
        if (!mappedValue) {
            return null
        } else {
            return mappedValue
        }
    }

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
        def dateFormat = "yyyy-MM-dd hh:mm:ss"
        def iFilename = iFolderName + iType + ".array"
        def iTargetName = iFilename.replace("array", "json").replace("/data/", "/json/")
        def iInfo = iFolderInfo + iType + ".json"
        def definition = new JsonSlurper().parse(new File(iInfo))
        def aFields = definition.fields
        def file = new File(iFilename)
        def array = new JsonSlurper().parse(file)
        def map = []

        for (int i = 0; i < array.size(); i++) {
            def oRow = new Object()
            for (int j = 0; j < aFields.size(); j++) {
                def field = aFields[j].name
                oRow.metaClass[field] = array[i][j]
            }
            map.push(oRow)

        }
        def json = new JsonBuilder(map);
        new File(iTargetName).write(json.toPrettyString())
    }

    void splitHoursIntoMonthFile(String iFilename) {

        def dateFormat = "yyyy-MM-dd hh:mm:ss"
        def dateMonth = "yyyy-MM"
        NumberFormat format = NumberFormat.getInstance(Locale.US)
        def map = []
        def csv = ""
        def csvCdelta = 0.0
        def csvPdelta = 0.0
        def csvDelta = 0.0
        def csvCosts = 0.0

        def inJson = new JsonSlurper().parse(new File(iFilename))
        Writer writer = new StringWriter()
        def builder = new MarkupBuilder(writer)
        def previousMonth = Date.parse(dateMonth, inJson[0].TIMESTAMP_lOCAL.toString())
        builder.root {
            inJson.each { hour ->
                def monthLine = Date.parse(dateMonth, hour.TIMESTAMP_lOCAL.toString())
                if (previousMonth.format("yyyy-MM") <=> monthLine.format("yyyy-MM")) {
                    if (map == []) {
                    } else {
                        def json = new JsonBuilder(map);
                        def iTargetFile = iFilename.replace("hour.json", "hours/hour") + "_" + previousMonth.format("yyyy-MM") + ".json"
                        new File(iTargetFile).write(json.toPrettyString())
                        map = []
                        def iTargetFileCsv = iFilename.replace("hour.json", "hour").replace("json", "csv") + "_" + previousMonth.format("yyyy-MM") + ".csv"
                        new File(iTargetFileCsv).write(csv)
                        csv = ""
                    }
                }
                def Price = getPrice(hour.TIMESTAMP_lOCAL)
                if (Price == null) {
                    hour.PRICE = new BigDecimal("0.0")
                } else {
                    hour.PRICE = new BigDecimal(Price)
                }

                def delta = new DecimalFormat("#.####").format(hour.CONSUMPTION_DELTA_KWH).replace(",", ".")
                hour.CONSUMPTION_DELTA_KWH = new BigDecimal(delta)
                delta = new DecimalFormat("#.####").format(hour.PRODUCTION_DELTA_KWH).replace(",", ".")
                hour.PRODUCTION_DELTA_KWH = new BigDecimal(delta)
                delta = new DecimalFormat("#.####").format(hour.CONSUMPTION_GAS_DELTA_M3).replace(",", ".")
                hour.CONSUMPTION_GAS_DELTA_M3 = new BigDecimal(delta)

                hour.DELTA_KWH = hour.CONSUMPTION_DELTA_KWH - hour.PRODUCTION_DELTA_KWH
                hour.COSTS = hour.DELTA_KWH * hour.PRICE

                uur {
                    'TimeStampLocal' hour.TIMESTAMP_lOCAL
                    'TARIFCODE' hour.TARIFCODE
                    'CONSUMPTION_KWH_LOW' hour.CONSUMPTION_KWH_LOW
                    'CONSUMPTION_KWH_HIGH' hour.CONSUMPTION_KWH_HIGH
                    'PRODUCTION_KWH_LOW' hour.PRODUCTION_KWH_LOW
                    'PRODUCTION_KWH_HIGH' hour.PRODUCTION_KWH_HIGH
                    'CONSUMPTION_DELTA_KWH' hour.CONSUMPTION_DELTA_KWH
                    'PRODUCTION_DELTA_KWH' hour.PRODUCTION_DELTA_KWH
                    'DELTA_KWH' hour.DELTA_KWH
                    'PRICE' hour.PRICE
                    'COSTS' hour.COSTS
                    if (hour.PRICE < 0) {
                        'PRICE_NEG' 1
                        hour.PRICE_NEG = 1
                    } else {
                        'PRICE_NEG' 0
                        hour.PRICE_NEG = 0
                    }
                    if (hour.PRICE < 0 && hour.DELTA_KWH < 0) {
                        'PAY_FOR_PRODUCTION' hour.COSTS
                        hour.PAY_FOR_PRODUCTION = hour.COSTS
                    } else {
                        'PAY_FOR_PRODUCTION' 0
                        hour.PAY_FOR_PRODUCTION = 0
                    }
                    if (hour.PRICE < 0 && hour.DELTA_KWH > 0) {
                        'RETURN_FOR_CONSUMPTION' hour.COSTS
                        hour.RETURN_FOR_CONSUMPTION = hour.COSTS
                    } else {
                        'RETURN_FOR_CONSUMPTION' 0
                        hour.RETURN_FOR_CONSUMPTION = 0
                    }
                    if (hour.PRICE > 0 && hour.DELTA_KWH < 0) {
                        'RETURN_FOR_PRODUCTION' hour.COSTS
                        hour.RETURN_FOR_PRODUCTION = hour.COSTS
                    } else {
                        'RETURN_FOR_PRODUCTION' 0
                        hour.RETURN_FOR_PRODUCTION = 0
                    }
                    if (hour.PRICE > 0 && hour.DELTA_KWH > 0) {
                        'PAY_FOR_CONSUMPTION' hour.COSTS
                        hour.PAY_FOR_CONSUMPTION = hour.COSTS
                    } else {
                        'PAY_FOR_CONSUMPTION' 0
                        hour.PAY_FOR_CONSUMPTION = 0
                    }
                    'TIMESTAMP_UTC' hour.TIMESTAMP_UTC
                    'CONSUMPTION_GAS_M3' hour.CONSUMPTION_GAS_M3
                    'CONSUMPTION_GAS_DELTA_M3' hour.CONSUMPTION_GAS_DELTA_M3
                }
                map.push(hour)
                def line = hour.TIMESTAMP_lOCAL + "," +
                        hour.TARIFCODE + "," +
                        hour.CONSUMPTION_KWH_LOW + "," +
                        hour.CONSUMPTION_KWH_HIGH + "," +
                        hour.PRODUCTION_KWH_LOW + "," +
                        hour.PRODUCTION_KWH_HIGH + "," +
                        hour.CONSUMPTION_DELTA_KWH + "," +
                        hour.PRODUCTION_DELTA_KWH + "," +
                        hour.DELTA_KWH + "," +
                        hour.PRICE + "," +
                        hour.COSTS
                if (hour.PRICE < 0) {
                    line = line + "," + 1
                } else {
                    line = line + ","
                }
                if (hour.PRICE < 0 && hour.DELTA_KWH < 0) {
                    line = line + "," + hour.COSTS
                } else {
                    line = line + ","
                }
                if (hour.PRICE < 0 && hour.DELTA_KWH > 0) {
                    line = line + "," + hour.COSTS
                } else {
                    line = line + ","
                }
                if (hour.PRICE > 0 && hour.DELTA_KWH < 0) {
                    line = line + "," + hour.COSTS
                } else {
                    line = line + ","
                }
                if (hour.PRICE > 0 && hour.DELTA_KWH > 0) {
                    line = line + "," + hour.COSTS
                } else {
                    line = line + ","
                }
                line = line + "," +
                        hour.TIMESTAMP_UTC + "," +
                        hour.CONSUMPTION_GAS_M3 + "," +
                        hour.CONSUMPTION_GAS_DELTA_M3

                csv = line + "\n" + csv

                csvCdelta = csvCdelta + hour.CONSUMPTION_DELTA_KWH
                csvPdelta = csvPdelta + hour.PRODUCTION_DELTA_KWH
                csvDelta = csvDelta + hour.DELTA_KWH
                csvCosts = csvCosts + hour.COSTS

                previousMonth = monthLine
            }
        }
        if (map == []) {
        } else {
            def json = new JsonBuilder(map);
            def iTargetFile = iFilename.replace(".json", "") + "_" + previousMonth.format("yyyy-MM") + ".json"
            new File(iTargetFile).write(json.toPrettyString())

            def iTargetFileCsv = iFilename.replace("hour.json", "hour").replace("json", "csv") + "_" + previousMonth.format("yyyy-MM") + ".csv"
            new File(iTargetFileCsv).write(csv)
        }

        def iTargetFileXml = iFilename.replace("hour.json", "hour").replace("json", "xml") + ".xml"
        new File(iTargetFileXml).write(writer.toString())

    }

    void buildFileWithDateRange(String iFolder, Date iDateFrom) {

        def date = iDateFrom
        def month = ""
        def csv = ""
        def map = []
        def newFile = "../../_data/out/$iFolder/" + date.format("yyyy-MM")

        for (def i = 0; i < 12; i++) {
            def CalcDate = use(TimeCategory) { date + i.month }
            month = CalcDate.format("yyyy-MM")
            println(month)
            def vFilename = "../../_data/in/$iFolder/csv/hour_$month" + ".csv"
            def vJsonFilename = "../../_data/in/$iFolder/json/hours/hour_$month" + ".json"
            try {
                File file = new File(vFilename)
                csv = csv + "\n" + file.text
                def oJson = new JsonSlurper().parse(new File(vJsonFilename))
                map.push(oJson)
            } catch (oError) {
                println("No file $vFilename" + oError.toString())
            }
        }
        newFile = "$newFile-$month" + ".csv"
        csv = csv.split('\n').drop(1).join('\n')
        csv = csv.replace("\n\n", "\n")
        csv = "TimeStampLocal,TARIFCODE,CONSUMPTION_KWH_LOW,CONSUMPTION_KWH_HIGH," +
                "PRODUCTION_KWH_LOW,PRODUCTION_KWH_HIGH,CONSUMPTION_DELTA_KWH,PRODUCTION_DELTA_KWH," +
                "DELTA_KWH,PRICE,COSTS," +
                "PRICE_NEG,PAY_FOR_PRODUCTION,RETURN_FOR_CONSUMPTION,RETURN_FOR_PRODUCTION,PAY_FOR_CONSUMPTION," +
                "TIMESTAMP_UTC,CONSUMPTION_GAS_M3,CONSUMPTION_GAS_DELTA_M3\n" + csv

        def iTargetFileCsv = newFile
        new File(iTargetFileCsv).write(csv)
        def aName = newFile.split("/")
        def vName = aName[aName.length - 1].split(".csv")[0]
        mapToXsl(iFolder, map, vName)
    }

    void mapToXsl(String iFolder, iMap, String iName) {
        createExcelFile(iFolder, iMap, iName)
    }

    def createExcelFile(String iFolder, iMap, String iName) {
        def workbookFilename = "../../_data/out/$iFolder/$iName" + ".xls"

        new FileOutputStream(new File(workbookFilename)).withStream { stream ->
            def workbook = Workbook.createWorkbook(stream)
            def sheet = workbook.createSheet('Sheet1', 0)
            def nLine = 0
            iMap[0][0].each { header ->
                def aInfo = getColumn(header)
                sheet.addCell(new Label(aInfo[0], nLine, aInfo[2]))
            }
            iMap.each { iArray ->
                iArray.each { row ->
                    row.each { cell ->
                        if (nLine != 0) {
                            def aInfo = getColumn(cell)
                            switch (aInfo[1]) {
                                case "L":
                                    sheet.addCell(new Label(aInfo[0], nLine, cell.value.toString()))
                                    break;
                                case "N":
                                    if (cell.value != 0) {
                                        sheet.addCell(new Number(aInfo[0], nLine, cell.value))
                                    }
                                    break;
                            }
                        }
                    }
                    nLine++
                }
            }

            Formula f
            //Totals
            def nTotalLine = nLine + 2
            sheet.addCell(new Label(6, nTotalLine, "Total"))
            def nToLine = nLine
            sheet.addCell(new Formula(8, nTotalLine, "SUM(I2:I$nToLine)"))
//            sheet.addCell(new Formula(9, nTotalLine, "SUM(J2:J$nToLine)"))
            sheet.addCell(new Formula(10, nTotalLine, "SUM(K2:K$nToLine)", new WritableCellFormat(new jxl.write.NumberFormat("#.00"))))
            sheet.addCell(new Formula(11, nTotalLine, "SUM(L2:L$nToLine)"))
            sheet.addCell(new Formula(12, nTotalLine, "SUM(M2:M$nToLine)", new WritableCellFormat(new jxl.write.NumberFormat("#.00"))))
            sheet.addCell(new Formula(13, nTotalLine, "SUM(N2:N$nToLine)", new WritableCellFormat(new jxl.write.NumberFormat("#.00"))))
            sheet.addCell(new Formula(14, nTotalLine, "SUM(O2:O$nToLine)", new WritableCellFormat(new jxl.write.NumberFormat("#.00"))))
            sheet.addCell(new Formula(15, nTotalLine, "SUM(P2:P$nToLine)", new WritableCellFormat(new jxl.write.NumberFormat("#.00"))))
            //Counts
            def nCountLine = nTotalLine + 1
            sheet.addCell(new Label(6, nCountLine, "Count"))
            nToLine = nLine
            sheet.addCell(new Formula(8, nCountLine, "COUNT(I2:I$nToLine)"))
//            sheet.addCell(new Formula(9, nCountLine, "COUNT(J2:J$nToLine)"))
            sheet.addCell(new Formula(10, nCountLine, "COUNT(K2:K$nToLine)"))
            sheet.addCell(new Formula(11, nCountLine, "COUNT(L2:L$nToLine)"))
            sheet.addCell(new Formula(12, nCountLine, "COUNT(M2:M$nToLine)"))
            sheet.addCell(new Formula(13, nCountLine, "COUNT(N2:N$nToLine)"))
            sheet.addCell(new Formula(14, nCountLine, "COUNT(O2:O$nToLine)"))
            sheet.addCell(new Formula(15, nCountLine, "COUNT(P2:P$nToLine)"))

            workbook.write()
            workbook.close()
        }
    }

    Object getColumn(iObject) {
        switch (iObject.key.toString()) {
            case "TIMESTAMP_lOCAL": return [0, "L", "TimeStamp"]
            case "TARIFCODE": return [1, "L", "TarifCode"]
            case "CONSUMPTION_KWH_LOW": return [2, "N", "ConsumptionLow"]
            case "CONSUMPTION_KWH_HIGH": return [3, "N", "ConsumptionHigh"]
            case "PRODUCTION_KWH_LOW": return [4, "N", "ProductionLow"]
            case "PRODUCTION_KWH_HIGH": return [5, "N", "ProductionHigh"]
            case "CONSUMPTION_DELTA_KWH": return [6, "N", "ConsumptionDelta"]
            case "PRODUCTION_DELTA_KWH": return [7, "N", "ProductionDelta"]
            case "DELTA_KWH": return [8, "N", "Delta"]
            case "PRICE": return [9, "N", "Price"]
            case "COSTS": return [10, "N", "Costs"]
            case "PRICE_NEG": return [11, "N", "PriceNeg"]
            case "PAY_FOR_PRODUCTION": return [13, "N", "ProductionPay"]
            case "RETURN_FOR_CONSUMPTION": return [14, "N", "ConsumptionReturn"]
            case "RETURN_FOR_PRODUCTION": return [15, "N", "ProductionReturn"]
            case "PAY_FOR_CONSUMPTION": return [12, "N", "ConsumptionPay"]
            case "TIMESTAMP_UTC": return [16, "L", "UTC"]
            case "CONSUMPTION_GAS_M3": return [17, "N", "Gas M3"]
            case "CONSUMPTION_GAS_DELTA_M3": return [18, "N", "Gas M3 Delta"]
        }
    }
}
