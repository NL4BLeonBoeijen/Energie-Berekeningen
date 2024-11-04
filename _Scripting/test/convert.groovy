import com.sap.it.api.mapping.ValueMappingApi
import com.themuth.energy.MergeUtils
import org.apache.camel.json.simple.Jsonable

import java.io.File
import groovy.json.JsonSlurper

def mu = new MergeUtils()
//mu.convertData('TMC','YMDH')
mu.convertData('BMB','YMDH')

mu.buildFileWithDateRange('../../_data/in/BMB/json/hour.json')
