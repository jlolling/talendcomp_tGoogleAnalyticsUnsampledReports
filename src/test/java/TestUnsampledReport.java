import com.google.api.services.analytics.model.UnsampledReport;

import de.jlo.talendcomp.googleanalytics.UnsampledReportHelper;

public class TestUnsampledReport {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		testGAManagement();
	}

	public static void testGAManagement() {

		UnsampledReportHelper gm = new UnsampledReportHelper();
		gm.setApplicationName("GATalendComp");

//		gm.setAccountEmail("503880615382@developer.gserviceaccount.com");
//		gm.setKeyFile("/Volumes/Data/Talend/testdata/ga/config/2bc309bb904201fcc6a443ff50a3d8aca9c0a12c-privatekey.p12");

		gm.setAccountEmail("422451649636@developer.gserviceaccount.com");
		gm.setKeyFile("/Volumes/Data/Talend/testdata/ga/config/af21f07c84b14af09c18837c5a385f8252cc9439-privatekey.p12");
		gm.setTimeOffsetMillisToPast(10000);
		gm.setTimeoutInSeconds(240);
		gm.reset();
		try {
			System.out.println("initialize client....");
			gm.initializeAnalyticsClient();
			gm.setAccountId("3584729");
			gm.setWebPropertyId("UA-3584729-1");
			gm.setProfileId("33360211");
			gm.setStartDate("2014-10-01");
			gm.setEndDate("2014-10-01");
			gm.setMetrics("ga:pageviews,ga:bounces");
			gm.setDimensions("ga:browser");
			gm.setFilters("ga:bounces>=100");
//			gm.setSegment("gaid:-1");
//			gm.setMetrics("ga:pageviews");
//			gm.setFilters("ga:pagePath=~.*/contactformsend");
//			gm.setStartDate("2014-12-03");
//			gm.setEndDate("2014-12-03");
			gm.setReportTitle("Jans Test 3");
//			UnsampledReport report = gm.startUnsampledReport();
//			System.out.println("report-Id=" + report.getId());
			System.out.println("List reports....");
			gm.collectUnsampledReports();
			while (gm.next()) {
				if (gm.hasCurrentUnsampledReport()) {
					printOut(gm.getCurrentUnsampledReport());
				}
			}
			System.out.println("Done.");
		} catch (Exception e1) {
			e1.printStackTrace();
		}

	}
	
	private static void printOut(UnsampledReport report) {
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
