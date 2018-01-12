package de.softwareschmied.homeintegrator.impl

import de.softwareschmied.homedataintegration.HomeData
import de.softwareschmied.myhomecontrolinterface.MyHomeControlData
import de.softwareschmied.solarwebinterface.{MeterData, PowerFlowSite}
import play.api.libs.json.{Format, Json}

/**
  * Created by Thomas Becker (thomas.becker00@gmail.com) on 05.01.18.
  */
trait JsonSupport  {
  implicit val format4: Format[MeterData] = Json.format[MeterData]
  implicit val format2: Format[MyHomeControlData] = Json.format[MyHomeControlData]
  implicit val format3: Format[PowerFlowSite] = Json.format[PowerFlowSite]
  implicit val format: Format[HomeData] = Json.format[HomeData]
}
