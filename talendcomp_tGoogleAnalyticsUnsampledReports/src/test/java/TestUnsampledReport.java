import java.io.IOException;
import java.util.List;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.analytics.model.UnsampledReport;

import de.jlo.talendcomp.google.analytics.unsampled.ResultFileParser;
import de.jlo.talendcomp.google.analytics.unsampled.UnsampledReportHelper;

public class TestUnsampledReport {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		testInsertReport();
	}
	
	public static void testParseReportFile() {
		ResultFileParser p = new ResultFileParser();
		try {
//			p.initializeFile("/Volumes/Data/Talend/testdata/ga/drive/mobile/0B1aeMk_qSLEkY1NtV1BLUDJSS1k.txt");
			p.initializeFile("/Volumes/Data/projects/mobile/mnt/talend/DEVELOPMENT/google_analytics/downloads/0B58W_P4pAxsERnJ1N1gzdDNUeTA.csv");
			System.out.println("dimensionInfo:" + p.getDimensionsInfo());
			System.out.println("metricInfo:" + p.getMetricsInfo());
			System.out.println("profileInfo:" + p.getProfileIdInfo());
			System.out.println("startDateInfo:" + p.getStartDateInfo());
			System.out.println("endDate:" + p.getEndDateInfo());
			System.out.println("segmentInfo:" + p.getSegmentInfo());
			System.out.println("filterInfo:" + p.getFiltersInfo());
			while (p.hasNextPlainRecord()) {
				printout(p.getNextPlainRecord());
			}
			System.out.println("Done.");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void testParseReportFileNormalized() {
		ResultFileParser p = new ResultFileParser();
		try {
//			p.initializeFile("/Volumes/Data/Talend/testdata/ga/drive/mobile/Jans Test for unsampled_small.csv");
//			p.setDimensions("d1,d2");
//			p.setMetrics("m1,m2,m3");
			p.initializeFile("/Volumes/Data/Talend/projects/mobile/mnt/talend/DEVELOPMENT/google_analytics/downloads/0B58W_P4pAxsEc0ZBb1VJTDhkUHM.csv");
			p.setMetrics("ga:sessions");
			// next calls the necessary calls to
			// hasNextPlainRecord()
			while (true) {
				if (p.nextNormalizedRecord() == false) {
					break;
				}
				de.jlo.talendcomp.google.analytics.unsampled.DimensionValue dv = p.getCurrentDimensionValue();
				if (dv != null) {
					System.out.println(dv);
				}
				de.jlo.talendcomp.google.analytics.unsampled.MetricValue mv = p.getCurrentMetricValue();
				if (mv != null) {
					System.out.println(mv);
				}
				System.out.println("------------------");
			}
			System.out.println("Done.");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	private static void printout(List<String> record) {
		boolean firstLoop = true;
		for (String s : record) {
			if (firstLoop) {
				firstLoop = false;
			} else {
				System.out.print("|");
			}
			System.out.print(s);
		}
		System.out.println();
	}

	public static void testInsertReport() {

		UnsampledReportHelper gm = new UnsampledReportHelper();
		gm.setApplicationName("GATalendComp");
		gm.setDebug(true);
//		gm.setAccountEmail("503880615382@developer.gserviceaccount.com");
//		gm.setKeyFile("/Volumes/Data/Talend/testdata/ga/config/2bc309bb904201fcc6a443ff50a3d8aca9c0a12c-privatekey.p12");

		gm.setAccountEmail("422451649636@developer.gserviceaccount.com");
		gm.setKeyFile("/Volumes/Data/Talend/testdata/ga/config/af21f07c84b14af09c18837c5a385f8252cc9439-privatekey.p12");
		gm.setTimeOffsetMillisToPast(10000);
		gm.setTimeoutInSeconds(240);
		gm.setInnerLoopWaitInterval(1000);
		gm.reset();
		try {
			System.out.println("initialize client....");
			gm.initializeAnalyticsClient();
			gm.setAccountId("3584729");
			gm.setWebPropertyId("UA-3584729-1");
			gm.setProfileId("33360211");
			gm.setStartDate("2015-10-02");
			gm.setEndDate("2015-10-02");
			gm.setMetrics("ga:pageviews");
			gm.setDimensions("ga:browser");
//			gm.setSegment("gaid:-1");
//			gm.setMetrics("ga:pageviews");
//			gm.setFilters("ga:pagePath=~.*/contactformsend");
//			gm.setStartDate("2014-12-03");
//			gm.setEndDate("2014-12-03");
			gm.setReportTitle("Jans Test to delete 2");
//			System.out.println("Start report...");
			gm.startUnsampledReport();
//			System.out.println("Delete report...");
//			gm.deleteUnsampledReport("OYJiKPPwSjeIRAq3SfiWKA");
//			UnsampledReport report = gm.startUnsampledReport();
//			System.out.println("report-Id=" + report.getId());
			System.out.println("List reports....");
			gm.collectUnsampledReports();
			while (gm.next()) {
				if (gm.hasCurrentUnsampledReport()) {
					UnsampledReport r = gm.getCurrentUnsampledReport();
					if (r.getId().equals("OYJiKPPwSjeIRAq3SfiWKA")) {
						printout(r);
					}
				}
			}
			System.out.println("Done.");
			
		} catch (Exception e1) {
			e1.printStackTrace();
		}

	}
	
	private static void printout(UnsampledReport report) {
		System.out.println("report-Id=" + report.getId());
		System.out.println("status=" + report.getStatus());
		System.out.println("title=" + report.getTitle());
		System.out.println("created=" + report.getCreated());
		System.out.println("updated=" + report.getUpdated());
		System.out.println("metrics=" + report.getMetrics());
		System.out.println("dimensions=" + report.getDimensions());
		System.out.println("filters=" + report.getFilters());
		System.out.println("start_date=" + report.getStartDate());
		System.out.println("end_date=" + report.getEndDate());
		System.out.println("downloadType=" + report.getDownloadType());
		if (report.getDriveDownloadDetails() != null) {
			System.out.println("drive document-id=" + report.getDriveDownloadDetails().getDocumentId());
		}
		System.out.println();
	}
	
}
