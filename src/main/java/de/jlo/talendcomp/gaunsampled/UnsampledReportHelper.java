package de.jlo.talendcomp.gaunsampled;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Clock;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.analytics.Analytics;
import com.google.api.services.analytics.AnalyticsScopes;
import com.google.api.services.analytics.model.UnsampledReport;
import com.google.api.services.analytics.model.UnsampledReports;

public class UnsampledReportHelper {

	private static final Map<String, UnsampledReportHelper> clientCache = new HashMap<String, UnsampledReportHelper>();
	private final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	private final JsonFactory JSON_FACTORY = new JacksonFactory();
	private File keyFile; // *.p12 key file is needed
	private String accountEmail;
	private String applicationName = null;
	private boolean useServiceAccount = true;
	private String credentialDataStoreDir = null;
	private String clientSecretFile = null;
	private int timeoutInSeconds = 120;
	private Analytics analyticsClient;
	private long timeMillisOffsetToPast = 10000;
	private List<UnsampledReport> listUnsampledReports;
	private long mainWaitInterval = 2000;
	private long innerLoopWaitInterval = 500;
	private int maxRows = 0;
	private int currentIndex = 0;
	private String profileId;
	private String webPropertyId;
	private String accountId;
	private String metrics;
	private String dimensions;
	private String filters;
	private String segment;
	private String startDate;
	private String endDate;
	private String reportTitle;
	public static final String DRIVE = "https://www.googleapis.com/auth/drive";
	public static final String DRIVE_FILE = "https://www.googleapis.com/auth/drive.file";
	
	public static void putIntoCache(String key, UnsampledReportHelper gam) {
		clientCache.put(key, gam);
	}
	
	public static UnsampledReportHelper getFromCache(String key) {
		return clientCache.get(key);
	}
	
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public void setKeyFile(String file) {
		keyFile = new File(file);
	}

	public void setAccountEmail(String email) {
		accountEmail = email;
	}

	public void setTimeoutInSeconds(int timeoutInSeconds) {
		this.timeoutInSeconds = timeoutInSeconds;
	}
	
	public void initializeAnalyticsClient() throws Exception {
		// Authorization.
		final Credential credential;
		if (useServiceAccount) {
			credential = authorizeWithServiceAccount();
		} else {
			credential = authorizeWithClientSecret();
		}
		// Set up and return Google Analytics API client.
		analyticsClient = new Analytics.Builder(
			HTTP_TRANSPORT, 
			JSON_FACTORY, 
			new HttpRequestInitializer() {
				@Override
				public void initialize(final HttpRequest httpRequest) throws IOException {
					credential.initialize(httpRequest);
					httpRequest.setConnectTimeout(timeoutInSeconds * 1000);
					httpRequest.setReadTimeout(timeoutInSeconds * 1000);
				}
			})
			.setApplicationName(applicationName)
			.build();
	}
	
	private Credential authorizeWithServiceAccount() throws Exception {
		if (keyFile == null) {
			throw new Exception("KeyFile not set!");
		}
		if (keyFile.canRead() == false) {
			throw new IOException("keyFile:" + keyFile.getAbsolutePath()
					+ " is not readable");
		}
		if (accountEmail == null || accountEmail.isEmpty()) {
			throw new Exception("account email cannot be null or empty");
		}
		// Authorization.
		return new GoogleCredential.Builder()
				.setTransport(HTTP_TRANSPORT)
				.setJsonFactory(JSON_FACTORY)
				.setServiceAccountId(accountEmail)
				.setServiceAccountScopes(Arrays.asList(AnalyticsScopes.ANALYTICS))
				.setServiceAccountPrivateKeyFromP12File(keyFile)
				.setClock(new Clock() {
					@Override
					public long currentTimeMillis() {
						// we must be sure, that we are always in the past from Googles point of view
						// otherwise we get an "invalid_grant" error
						return System.currentTimeMillis() - timeMillisOffsetToPast;
					}
				})
				.build();
	}
	
	/**
	 * Authorizes the installed application to access user's protected YouTube
	 * data.
	 * 
	 * @param scopes
	 *            list of scopes needed to access general and analytic YouTube
	 *            info.
	 */
	private Credential authorizeWithClientSecret() throws Exception {
		if (clientSecretFile == null) {
			throw new IllegalStateException("client secret file is not set");
		}
		File secretFile = new File(clientSecretFile);
		if (secretFile.exists() == false) {
			throw new Exception("Client secret file:" + secretFile.getAbsolutePath() + " does not exists or is not readable.");
		}
		Reader reader = new FileReader(secretFile);
		// Load client secrets.
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, reader);
		try {
			reader.close();
		} catch (Throwable e) {}
		// Checks that the defaults have been replaced (Default =
		// "Enter X here").
		if (clientSecrets.getDetails().getClientId().startsWith("Enter")
				|| clientSecrets.getDetails().getClientSecret()
						.startsWith("Enter ")) {
			throw new Exception("The client secret file does not contains the credentials. At first you have to pass the web based authorization process!");
		}
		credentialDataStoreDir = secretFile.getParent() + "/" + clientSecrets.getDetails().getClientId() + "/";
		File credentialDataStoreDirFile = new File(credentialDataStoreDir);             
		if (credentialDataStoreDirFile.exists() == false && credentialDataStoreDirFile.mkdirs() == false) {
			throw new Exception("Credentedial data dir does not exists or cannot created:" + credentialDataStoreDir);
		}
		FileDataStoreFactory fdsf = new FileDataStoreFactory(credentialDataStoreDirFile);
		// Set up authorization code flow.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				HTTP_TRANSPORT, 
				JSON_FACTORY, 
				clientSecrets, 
				Arrays.asList(AnalyticsScopes.ANALYTICS, DRIVE, DRIVE_FILE))
			.setDataStoreFactory(fdsf)
			.setClock(new Clock() {
				@Override
				public long currentTimeMillis() {
					// we must be sure, that we are always in the past from Googles point of view
					// otherwise we get an "invalid_grant" error
					return System.currentTimeMillis() - timeMillisOffsetToPast;
				}
			})
			.build();
		// Authorize.
		return new AuthorizationCodeInstalledApp(
				flow,
				new LocalServerReceiver()).authorize(accountEmail);
	}

	public void reset() {
		listUnsampledReports = null;
		maxRows = 0;
		currentIndex = 0;
	}

	private void setMaxRows(int rows) {
		if (maxRows < rows) {
			maxRows = rows;
		}
	}
	
	public void setTimeOffsetMillisToPast(long timeMillisOffsetToPast) {
		this.timeMillisOffsetToPast = timeMillisOffsetToPast;
	}

	public boolean next() {
		if (listUnsampledReports == null) {
			throw new IllegalStateException("Call collectUnsampledReports before!");
		}
		return ++currentIndex <= maxRows;
	}
	
	public int getMaxRows() {
		return maxRows;
	}
	
	public int getCurrentIndex() {
		return currentIndex - 1;
	}
	
	public long getMainWaitInterval() {
		return mainWaitInterval;
	}

	public void setMainWaitInterval(long mainWaitInterval) {
		this.mainWaitInterval = mainWaitInterval;
	}

	public long getInnerLoopWaitInterval() {
		return innerLoopWaitInterval;
	}

	public void setInnerLoopWaitInterval(long innerLoopWaitInterval) {
		this.innerLoopWaitInterval = innerLoopWaitInterval;
	}
	
	public UnsampledReport startUnsampledReport() throws Exception {
		UnsampledReport reportRequest = new UnsampledReport();
		reportRequest.setMetrics(metrics);
		reportRequest.setDimensions(dimensions);
		reportRequest.setFilters(filters);
		reportRequest.setSegment(segment);
		reportRequest.setStartDate(startDate);
		reportRequest.setEndDate(endDate);
		reportRequest.setTitle(reportTitle);
		reportRequest.setDownloadType("GOOGLE_DRIVE");
		UnsampledReport reportResponse = analyticsClient
				.management()
				.unsampledReports()
				.insert(accountId, webPropertyId, profileId, reportRequest)
				.execute();
		return reportResponse;
	}
	
	public void collectUnsampledReports() throws Exception {
		listUnsampledReports = new ArrayList<UnsampledReport>();
		UnsampledReports reports = analyticsClient
				.management()
				.unsampledReports()
				.list(accountId, webPropertyId, profileId)
			    .execute();
		if (reports != null && reports.getItems() != null) {
			for (UnsampledReport report : reports.getItems()) {
				listUnsampledReports.add(report);
			}
			setMaxRows(listUnsampledReports.size());
		}
	}
	
	public boolean hasCurrentUnsampledReport() {
		if (listUnsampledReports != null) {
			return currentIndex <= listUnsampledReports.size();
		} else {
			return false;
		}
	}
	
	public UnsampledReport getCurrentUnsampledReport() {
		if (currentIndex == 0) {
			throw new IllegalStateException("Call next before!");
		}
		if (currentIndex <= listUnsampledReports.size()) {
			return listUnsampledReports.get(currentIndex - 1);
		} else {
			return null;
		}
	}

	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		if (profileId == null || profileId.trim().isEmpty()) {
			throw new IllegalArgumentException("profileId cannot be null or empty.");
		}
		this.profileId = profileId;
	}

	public void setProfileId(Long profileId) {
		if (profileId == null) {
			throw new IllegalArgumentException("profileId cannot be null.");
		}
		this.profileId = Long.toString(profileId);
	}

	public String getWebPropertyId() {
		return webPropertyId;
	}

	public void setWebPropertyId(String webPropertyId) {
		if (webPropertyId == null || webPropertyId.trim().isEmpty()) {
			throw new IllegalArgumentException("webPropertyId cannot be null or empty.");
		}
		this.webPropertyId = webPropertyId;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		if (accountId == null || accountId.trim().isEmpty()) {
			throw new IllegalArgumentException("accountId cannot be null or empty.");
		}
		this.accountId = accountId;
	}
	
	public void setAccountId(Long accountId) {
		if (accountId == null) {
			throw new IllegalArgumentException("accountId cannot be null.");
		}
		this.accountId = Long.toString(accountId);
	}

	public String getMetrics() {
		return metrics;
	}

	public void setMetrics(String metrics) {
		if (metrics == null || metrics.trim().isEmpty()) {
			throw new IllegalArgumentException("metrics cannot be null or empty.");
		}
		this.metrics = metrics;
	}

	public String getDimensions() {
		return dimensions;
	}

	public void setDimensions(String dimensions) {
		if (dimensions != null && dimensions.trim().isEmpty() == false) {
			this.dimensions = dimensions.trim();
		} else {
			this.dimensions = null;
		}
	}

	public String getFilters() {
		return filters;
	}

	public void setFilters(String filters) {
		if (filters != null && filters.trim().isEmpty() == false) {
			this.filters = filters.trim();
		} else {
			this.filters = null;
		}
	}

	public String getSegment() {
		return segment;
	}

	public void setSegment(String segment) {
		if (segment != null && segment.trim().isEmpty() == false) {
			this.segment = segment.trim();
		} else {
			this.segment = null;
		}
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		if (startDate == null || startDate.trim().isEmpty()) {
			throw new IllegalArgumentException("startDate cannot be null or empty.");
		}
		this.startDate = startDate.trim();
	}

	public void setStartDate(Date startDate) {
		if (startDate == null) {
			throw new IllegalArgumentException("startDate cannot be null.");
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		this.startDate = sdf.format(startDate);
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		if (endDate == null) {
			throw new IllegalArgumentException("endDate cannot be null.");
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		this.endDate = sdf.format(endDate);
	}

	public void setEndDate(String endDate) {
		if (endDate == null || endDate.trim().isEmpty()) {
			throw new IllegalArgumentException("endDate cannot be null or empty.");
		}
		this.endDate = endDate.trim();
	}

	public String getReportTitle() {
		return reportTitle;
	}

	public void setReportTitle(String reportTitle) {
		this.reportTitle = reportTitle;
	}

	public boolean isUseServiceAccount() {
		return useServiceAccount;
	}

	public void setUseServiceAccount(boolean useServiceAccount) {
		this.useServiceAccount = useServiceAccount;
	}

	public String getClientSecretFile() {
		return clientSecretFile;
	}

	public void setClientSecretFile(String clientSecretFile) {
		this.clientSecretFile = clientSecretFile;
	}

}
