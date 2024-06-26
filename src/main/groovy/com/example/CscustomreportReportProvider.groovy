package com.example

import com.morpheusdata.core.AbstractReportProvider
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.model.OptionType
import com.morpheusdata.model.ReportResult
import com.morpheusdata.model.ReportResultRow
import com.morpheusdata.response.ServiceResponse
import com.morpheusdata.views.HTMLResponse
import com.morpheusdata.views.ViewModel
import groovy.util.logging.Slf4j
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import java.sql.Connection
import io.reactivex.rxjava3.core.Observable
import java.text.DecimalFormat

class CscustomreportReportProvider extends AbstractReportProvider{
	protected MorpheusContext morpheusContext
	protected Plugin plugin

	CscustomreportReportProvider(Plugin plugin, MorpheusContext morpheusContext) {
		this.morpheusContext = morpheusContext
		this.plugin = plugin
	}
	/**
	 * Returns the Morpheus Context for interacting with data stored in the Main Morpheus Application
	 *
	 * @return an implementation of the MorpheusContext for running Future based rxJava queries
	 */
	
	@Override
	MorpheusContext getMorpheus() {
		return this.morpheusContext
	}

	/**
	 * Returns the instance of the Plugin class that this provider is loaded from
	 * @return Plugin class contains references to other providers
	 */
	@Override
	Plugin getPlugin() {
		return this.plugin
	}
	/**
	 * A unique shortcode used for referencing the provided provider. Make sure this is going to be unique as any data
	 * that is seeded or generated related to this provider will reference it by this code.
	 * @return short code string that should be unique across all other plugin implementations.
	 */
	@Override
	String getCode() {
		return "cscr-report"
	}

	/**
	 * Provides the provider name for reference when adding to the Morpheus Orchestrator
	 * NOTE: This may be useful to set as an i18n key for UI reference and localization support.
	 *
	 * @return either an English name of a Provider or an i18n based key that can be scanned for in a properties file.
	 */
	@Override
	String getName() {
		return "CS VM Financials"
	}

	@Override
	ServiceResponse validateOptions(Map opts) {
		return ServiceResponse.success()
	}

	// start date
    OptionType startDate = new OptionType(
        code: 'start-date',
        name: 'StartDate',
        fieldName: 'startMonth',
        fieldContext: 'config',
        fieldLabel: 'Start date',
        displayOrder: 0,
        inputType: OptionType.InputType.TEXT
    )

    // end date
    OptionType endDate = new OptionType(
        code: 'end-date',
        name: 'EndDate',
        fieldName: 'endMonth',
        fieldContext: 'config',
        fieldLabel: 'End date',
        displayOrder: 1,
        inputType: OptionType.InputType.TEXT
    )

	OptionType region = new OptionType (
		code: 'region',
		name: 'Region',
		fieldName: 'region',
		fieldLabel: 'Region',
		fieldContext: 'config',
		displayOrder: 0,
		inputType: OptionType.InputType.TEXT
		// inputType: OptionType.InputType.SELECT,
		// optionSource: 'regions'
	)
	/**
	 * The primary entrypoint for generating a report. This method can be a long running process that queries data in the database
	 * or from another external source and generates {@link ReportResultRow} objects that can be pushed into the database
	 *
	 * <p><strong>Example:</strong></p>
	 * <pre>{@code
	 * void process(ReportResult reportResult) {
	 *      morpheus.report.updateReportResultStatus(reportResult,ReportResult.Status.generating).blockingGet();
	 *      Long displayOrder = 0
	 *      List<GroovyRowResult> results = []
	 *      Connection dbConnection
	 *
	 *      try {
	 *          dbConnection = morpheus.report.getReadOnlyDatabaseConnection().blockingGet()
	 *          if(reportResult.configMap?.phrase) {
	 *              String phraseMatch = "${reportResult.configMap?.phrase}%"
	 *              results = new Sql(dbConnection).rows("SELECT id,name,status from instance WHERE name LIKE ${phraseMatch} order by name asc;")
	 *          } else {
	 *              results = new Sql(dbConnection).rows("SELECT id,name,status from instance order by name asc;")
	 *          }
	 *      } finally {
	 *          morpheus.report.releaseDatabaseConnection(dbConnection)
	 *      }
	 *      log.info("Results: ${results}")
	 *      Observable<GroovyRowResult> observable = Observable.fromIterable(results) as Observable<GroovyRowResult>
	 *      observable.map{ resultRow ->
	 *          log.info("Mapping resultRow ${resultRow}")
	 *          Map<String,Object> data = [name: resultRow.name, id: resultRow.id, status: resultRow.status]
	 *          ReportResultRow resultRowRecord = new ReportResultRow(section: ReportResultRow.SECTION_MAIN, displayOrder: displayOrder++, dataMap: data)
	 *          log.info("resultRowRecord: ${resultRowRecord.dump()}")
	 *          return resultRowRecord
	 *      }.buffer(50).doOnComplete {
	 *          morpheus.report.updateReportResultStatus(reportResult,ReportResult.Status.ready).blockingGet();
	 *      }.doOnError { Throwable t ->
	 *          morpheus.report.updateReportResultStatus(reportResult,ReportResult.Status.failed).blockingGet();
	 *      }.subscribe {resultRows ->
	 *          morpheus.report.appendResultRows(reportResult,resultRows).blockingGet()
	 *      }
	 *  }
	 *}</pre>
	 *
	 * @param reportResult the Report result the data is being attached to. Status of the run is updated here, also this object contains filter parameters
	 *                     that may have been applied based on the {@link ReportProvider#getOptionTypes()}
	 */
	

	@Override
	void process(ReportResult reportResult) {
		//TODO: Fill out a report process as described above. NOTE: Use DataServices where able.
		morpheus.report.updateReportResultStatus(reportResult,ReportResult.Status.generating).blockingAwait();
		Long displayOrder = 0
		List<GroovyRowResult> repResults = []
		def instanceCount = 0
		def cloudCount = 0
		def totalCores = 0
		def totalMemory = 0
		def totalStorage = 0
		Float totalCostSum = 0
		String start_Date = ""
		String end_Date = ""

		Connection dbConnection
		try {
			dbConnection = morpheus.report.getReadOnlyDatabaseConnection().blockingGet()

			// get startMonth and endMonth
		    String startDate = "''"
		    String endDate =  "''"
            if (reportResult.configMap?.startMonth){
				println('Start Date (from config map): ' + reportResult.configMap?.startMonth);
                startDate = "'${this.plugin.goodDate(reportResult.configMap?.startMonth, false)}-01'"
				println('Start Date (after goodDate): ' + startDate);
            }
            if (reportResult.configMap?.endMonth){
				println('End Date (from config map): ' + reportResult.configMap?.endMonth);
                endDate = "'${this.plugin.goodDate(reportResult.configMap?.endMonth, true)}-01'"
				println('End Date (after goodDate): ' + endDate);
            }

			def accountName = reportResult.getAccount().getName()
			//repResults = new Sql(dbConnection).rows("SELECT i.id, i.display_name AS name, i.max_cores, ROUND(i.max_memory / 1024 / 1024 / 1024) AS max_memory, ROUND(i.max_storage / 1024 / 1024 / 1024) AS max_storage, os.name AS os_name, cz.name AS zone_name, SUM(ai.actual_total_cost) AS actual_total_cost, mt_abc.value AS abc_value FROM instance AS i JOIN container AS c ON i.id = c.instance_id JOIN compute_server AS cs ON c.server_id = cs.id JOIN os_type AS os ON cs.server_os_id = os.id JOIN compute_zone AS cz ON cs.zone_id = cz.id JOIN account_invoice AS ai ON i.id = ai.instance_id JOIN instance_metadata_tag AS imt_abc ON i.id = imt_abc.instance_metadata_id JOIN metadata_tag AS mt_abc ON imt_abc.metadata_tag_id = mt_abc.id WHERE ai.ref_type = 'Instance' AND ai.account_name = '"+ accountName + "' AND ai.start_date >= "+ startDate +" AND ai.end_date < " + endDate + " AND ai.actual_total_cost > 0 AND mt_abc.name = 'Platform_Service_Tier' GROUP BY i.id, os.name, cz.name, mt_abc.value LIMIT 8000;")
			// repResults = new Sql(dbConnection).rows("SELECT i.id, i.display_name AS name, i.max_cores, ROUND(i.max_memory / 1024 / 1024 / 1024) AS max_memory, ROUND(i.max_storage / 1024 / 1024 / 1024) AS max_storage, os.name AS os_name, cz.name AS zone_name, SUM(ai.actual_total_cost) AS actual_total_cost, MAX(CASE WHEN mt.name = 'Platform_Service_Tier' THEN mt.value END) AS platform_Service_Tier, MAX(CASE WHEN mt.name = 'Support_Owner_Email' THEN mt.value END) AS support_Owner_Email FROM instance AS i JOIN container AS c ON i.id = c.instance_id JOIN compute_server AS cs ON c.server_id = cs.id JOIN os_type AS os ON cs.server_os_id = os.id JOIN compute_zone AS cz ON cs.zone_id = cz.id JOIN account_invoice AS ai ON i.id = ai.instance_id JOIN instance_metadata_tag AS imt ON i.id = imt.instance_metadata_id JOIN metadata_tag AS mt ON imt.metadata_tag_id = mt.id WHERE ai.ref_type = 'Instance' AND ai.account_name = '"+ accountName + "' AND ai.start_date >= "+ startDate +" AND ai.end_date < "+ endDate +" AND ai.actual_total_cost > 0 AND mt.name = 'Platform_Service_Tier' GROUP BY i.id, os.name, cz.name LIMIT 8000;")
			repResults = new Sql(dbConnection).rows("SELECT i.id, i.display_name AS name, i.max_cores, ROUND(i.max_memory / 1024 / 1024 / 1024) AS max_memory, ROUND(i.max_storage / 1024 / 1024 / 1024) AS max_storage, os.name AS os_name, cz.name AS zone_name, ai.total_cost AS actual_total_cost, MAX(CASE WHEN mt.name = 'Platform_Service_Tier' THEN mt.value END) AS platform_Service_Tier, MAX(CASE WHEN mt.name = 'Support_Owner_Email' THEN mt.value END) AS support_Owner_Email, MAX(CASE WHEN mt.name = 'Region' THEN mt.value END) AS region, MAX(CASE WHEN mt.name = 'Primary_Application' THEN mt.value END) AS primary_Application, MAX(CASE WHEN mt.name = 'ApCID' THEN mt.value END) AS aPCID, MAX(CASE WHEN mt.name = 'App_Abbreviation' THEN mt.value END) AS app_Abbreviation, MAX(CASE WHEN mt.name = 'GBU' THEN mt.value END) AS gbu, MAX(CASE WHEN mt.name = 'Division' THEN mt.value END) AS division, MAX(CASE WHEN mt.name = 'Development_Team' THEN mt.value END) AS development_Team, MAX(CASE WHEN mt.name = 'Site_Code' THEN mt.value END) AS site_Code, MAX(CASE WHEN mt.name = 'Type' THEN mt.value END) AS tYPE, MAX(CASE WHEN mt.name = 'Env' THEN mt.value END) AS eNV, MAX(CASE WHEN mt.name = 'Function' THEN mt.value END) AS ifUNCTION, MAX(CASE WHEN mt.name = 'DMZ' THEN mt.value END) AS dMZ, MAX(CASE WHEN mt.name = 'Regulation' THEN mt.value END) AS rEGULATION FROM instance AS i JOIN container AS c ON i.id = c.instance_id JOIN compute_server AS cs ON c.server_id = cs.id JOIN os_type AS os ON cs.server_os_id = os.id JOIN compute_zone AS cz ON cs.zone_id = cz.id JOIN (SELECT ai.instance_id, SUM(ai.actual_total_cost) AS total_cost FROM account_invoice AS ai WHERE ai.ref_type = 'Instance' AND ai.account_name = '"+ accountName + "' AND ai.start_date >= "+ startDate +" AND ai.end_date < "+ endDate +" AND ai.actual_total_cost > 0 GROUP BY ai.instance_id) AS ai ON i.id = ai.instance_id JOIN instance_metadata_tag AS imt ON i.id = imt.instance_metadata_id JOIN metadata_tag AS mt ON imt.metadata_tag_id = mt.id WHERE mt.name IN ('Platform_Service_Tier', 'Support_Owner_Email', 'Region', 'Primary_Application', 'ApCID', 'App_Abbreviation', 'GBU', 'Division', 'Development_Team', 'Site_Code', 'Type', 'Env', 'Function', 'DMZ', 'Regulation') GROUP BY i.id, os.name, cz.name LIMIT 8000;")
			} finally {
				morpheus.report.releaseDatabaseConnection(dbConnection)
		}

		Observable<GroovyRowResult> observable = Observable.fromIterable(repResults) as Observable<GroovyRowResult>
			observable.map { resultRow -> 
				def Map<String,Object> data = [:]
				data = [
					Server_Name: resultRow.name,
					Guest_OS: resultRow.os_name,
					cloud: resultRow.zone_name,
					cores: resultRow.max_cores,
					memory: resultRow.max_memory,
					storage: resultRow.max_storage,
					totalCost: resultRow.actual_total_cost.round(2),
					Platform_Service_Tier: resultRow.platform_Service_Tier,
					Support_Owner_Email: resultRow.support_Owner_Email,
					Region: resultRow.region,
					ApCID: resultRow.aPCID,
					App_Abbreviation: resultRow.app_Abbreviation,
					Division: resultRow.division,
					Site_Code: resultRow.site_Code,
					Env: resultRow.eNV,
					DMZ: resultRow.dMZ,
					Primary_Application: resultRow.primary_Application,
					GBU: resultRow.gbu,
					Development_Team: resultRow.development_Team,
					Type: resultRow.tYPE,
					Function: resultRow.ifUNCTION,
					Regulation: resultRow.rEGULATION
				]
				instanceCount ++
				totalCostSum += resultRow.actual_total_cost
				start_Date = "${this.plugin.goodDate(reportResult.configMap?.startMonth, false)}-01"
				end_Date = "${this.plugin.goodDate(reportResult.configMap?.endMonth, true)}-01"

			
			ReportResultRow resultRowRecord = new ReportResultRow(section: ReportResultRow.SECTION_MAIN, displayOrder: displayOrder++, dataMap: data)

			return resultRowRecord
			}.buffer(50).doOnComplete {
				morpheus.report.updateReportResultStatus(reportResult,ReportResult.Status.ready).blockingAwait();
			}.doOnError { Throwable t -> 
				morpheus.report.updateReportResultStatus(reportResult,ReportResult.Status.failed).blockingAwait();
			}.subscribe {resultRows -> 
				morpheus.report.appendResultRows(reportResult,resultRows).blockingGet()
			}

			// header 
			def df = new DecimalFormat("#0.00")
        	def total2Dec = df.format(totalCostSum.round(2))
			Map<String,Object> headerData = [
				instanceCount: instanceCount,
				totalCost: total2Dec,
				startDate: start_Date,
				endDate: end_Date
			]
			ReportResultRow resultRowRecord = new ReportResultRow(section: ReportResultRow.SECTION_HEADER, displayOrder: displayOrder++, dataMap: headerData)
			morpheus.report.appendResultRows(reportResult,[resultRowRecord]).blockingGet()
	}

	/**
	 * A short description of the report for the user to better understand its purpose.
	 * @return the description string
	 */
	@Override
	String getDescription() {
		return "VM report."
	}

	/**
	 * Gets the category string for the report. Reports can be organized by category when viewing.
	 * @return the category string (i.e. inventory)
	 */
	@Override
	String getCategory() {
		return "Financial"
	}

	/**
	 * Only the owner of the report result can view the results.
	 * @return whether this report type can be read by the owning user only or not
	 */
	@Override
	Boolean getOwnerOnly() {
		return false
	}

	/**
	 * Some reports can only be run by the master tenant for security reasons. This informs Morpheus that the report type
	 * is a master tenant only report.
	 * @return whether or not this report is for the master tenant only.
	 */
	@Override
	Boolean getMasterOnly() {
		return false
	}

	/**
	 * Detects whether or not this report is scopable to all cloud types or not
	 * TODO: Implement this for custom reports (NOT YET USABLE)
	 * @return whether or not the report is supported by all cloud types. This allows for cloud type specific reports
	 */
	@Override
	Boolean getSupportsAllZoneTypes() {
		return true
	}

	@Override
	List<OptionType> getOptionTypes() {
		[region,startDate, endDate]
	}

	/**
	 * Presents the HTML Rendered output of a report. This can use different {@link Renderer} implementations.
	 * The preferred is to use server side handlebars rendering with {@link com.morpheusdata.views.HandlebarsRenderer}
	 * <p><strong>Example Render:</strong></p>
	 * <pre>{@code
	 *    ViewModel model = new ViewModel()
	 * 	  model.object = reportRowsBySection
	 * 	  getRenderer().renderTemplate("hbs/instanceReport", model)
	 *}</pre>
	 * @param reportResult the results of a report
	 * @param reportRowsBySection the individual row results by section (i.e. header, vs. data)
	 * @return result of rendering an template
	 */
	@Override
	HTMLResponse renderTemplate(ReportResult reportResult, Map<String, List<ReportResultRow>> reportRowsBySection) {
		ViewModel<Map<String, List<ReportResultRow>>> model = new ViewModel<>()
		def HashMap<String, String> reportPayload = new HashMap<String, String>();

		// nonce
		def nonce = morpheus.getWebRequest().getNonceToken()
		reportPayload.put("none", nonce)

		// add  report data
		reportPayload.put("report", reportRowsBySection)
		model.object = reportPayload
		getRenderer().renderTemplate("hbs/cscustomreportReport", model)
	}
}
