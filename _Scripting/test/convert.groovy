import com.themuth.energy.MergeUtils
//import java.text.SimpleDateFormat

def vFolder = 'BMB'
def mu = new MergeUtils()
mu.loadPrices('../../_data/in/Info/Prijzen/stroomprijzen.json')
//mu.convertData("$vFolder",'YMDH')
//mu.convertData("$vFolder",'H')

def dateFormat = "yyyy-MM-dd"
def dateFrom = Date.parse(dateFormat,'2023-11-01')

mu.splitHoursIntoMonthFile("../../_data/in/$vFolder/json/hour.json")
mu.buildFileWithDateRange("$vFolder", dateFrom)

