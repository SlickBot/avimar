package eu.slickbot.avimar.data.adsb.readsb

import android.util.Log
import eu.slickbot.avimar.data.adsb.info.doc8643.Doc8643LookupResponse
import eu.slickbot.avimar.data.adsb.info.doc8643.Doc8643LookupService

class AdsbReportEnricher internal constructor(
  private val lookupAll: (Set<String>) -> Map<String, Doc8643LookupResponse>,
) {

  companion object {
    private const val TAG = "AdsbReportEnricher"
  }

  constructor(service: Doc8643LookupService) : this(lookupAll = service::lookupAll)

  fun enrich(snapshot: ReadSbSnapshot): ReadSbSnapshot {
    val typeCodes = snapshot.reports.mapNotNull { it.icaoTypeCode }.toSet()
    if (typeCodes.isEmpty()) {
      return snapshot
    }

    val lookups = try {
      lookupAll(typeCodes)
    } catch (e: Exception) {
      Log.w(TAG, "enrich failed: ${e.message ?: e.javaClass.simpleName}")
      return snapshot
    }
    if (lookups.isEmpty()) {
      return snapshot
    }

    val enrichedReports = snapshot.reports.map { report ->
      enrichReport(report, lookups)
    }
    return snapshot.copy(reports = enrichedReports)
  }

  private fun enrichReport(
    report: ReadSbReport,
    lookups: Map<String, Doc8643LookupResponse>,
  ): ReadSbReport {
    val typeCode = report.icaoTypeCode ?: return report
    val lookup = lookups[typeCode] ?: return report

    val descCode = report.icaoDescriptionCode
      ?: lookup.aircraft.classification?.trim()?.takeIf { it.isNotBlank() }
    val descText = report.icaoDescriptionCodeText
      ?: lookup.descriptionDecoded?.description
    val wtc = report.wakeTurbulenceCategory
      ?: lookup.aircraft.category?.trim()?.takeIf { it.isNotBlank() }

    if (descCode == report.icaoDescriptionCode
      && descText == report.icaoDescriptionCodeText
      && wtc == report.wakeTurbulenceCategory
    ) {
      return report
    }

    return report.copy(
      icaoDescriptionCode = descCode,
      icaoDescriptionCodeText = descText,
      wakeTurbulenceCategory = wtc,
    )
  }
}
