package de.softwareschmied.homeintegrator.power.impl

import de.softwareschmied.homedataintegration.HomePowerData
import de.softwareschmied.myhomecontrolinterface.MyHomeControlPowerData
import de.softwareschmied.solarwebinterface.{MeterData, PowerFlowSite}
import play.api.libs.json.{Format, Json}

/**
  * Created by Thomas Becker (thomas.becker00@gmail.com) on 05.01.18.
  */
trait JsonSupport {
  implicit val format4: Format[MeterData] = Json.format[MeterData]
  implicit val format2: Format[MyHomeControlPowerData] = Json.format[MyHomeControlPowerData]
  implicit val format3: Format[PowerFlowSite] = Json.format[PowerFlowSite]
  implicit val format: Format[HomePowerData] = Json.format[HomePowerData]
}
