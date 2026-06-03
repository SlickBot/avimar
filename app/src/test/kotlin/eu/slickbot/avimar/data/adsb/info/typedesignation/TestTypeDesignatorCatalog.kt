package eu.slickbot.avimar.data.adsb.info.typedesignation

import kotlinx.serialization.json.Json
import java.io.File

fun testTypeDesignatorCatalog(json: Json): TypeDesignatorCatalog {
  return TypeDesignatorCatalog(json) {
    File("src/main/res/raw/type_designators.json").inputStream()
  }
}
