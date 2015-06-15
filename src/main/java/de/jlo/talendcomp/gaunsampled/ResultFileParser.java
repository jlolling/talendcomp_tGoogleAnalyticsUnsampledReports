/**
 * Copyright 2015 Jan Lolling jan.lolling@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.jlo.talendcomp.gaunsampled;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResultFileParser {
	
	private List<String> requestedDimensionNames = new ArrayList<String>();
	private List<String> requestedMetricNames = new ArrayList<String>();
	private BufferedReader br = null;
	private boolean validFile = false;
	private String headerLine = null;
	private List<String> headers = null;
	private String currentLine = null;
	private int currentPlainRowIndex = 0;
	private int maxCountNormalizedValues = 0;
	private int currentNormalizedValueIndex = 0;
	private List<DimensionValue> currentResultRowDimensionValues;
	private Date currentDate;
	private static final String DATE_DIME = "ga:date";
	private boolean excludeDate = false;
	private List<MetricValue> currentResultRowMetricValues;
	private int countDimensions = 0;
	private static final String DIMENSIONS_KEY = "dimensions:";
	private static final String METRICS_KEY = "metrics:";
	private static final String FILTERS_KEY = "filters:";
	private static final String SEGMENT_KEY = "segment:";
	private static final String PROFILE_KEY = "profileId:";
	private static final String START_DATE_KEY = "start-date:";
	private static final String END_DATE_KEY = "end-date:";
	private String profileIdInfo = null;
	private String dimensionsInfo = null;
	private String metricsInfo = null;
	private String filtersInfo = null;
	private String segmentInfo = null;
	private String startDateInfo = null;
	private String endDateInfo = null;
	private boolean setDimensionsAndMetricsFromHeader = true;
	private boolean checkMissingFields = true;
	private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");

	public void initializeFile(String filePath) throws Exception {
		if (filePath == null || filePath.trim().isEmpty()) {
			throw new IllegalArgumentException("filePath cannot be null or empty.");
		}
		File f = new File(filePath.trim());
		if (f.exists() == false) {
			throw new IllegalStateException("File " + f.getAbsolutePath() + " cannot be read!");
		}
		br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
		processHeaderRows();
		if (setDimensionsAndMetricsFromHeader) {
			setMetrics(metricsInfo);
			setDimensions(dimensionsInfo);
		}
		currentPlainRowIndex = 0;
		currentNormalizedValueIndex = 0;
		maxCountNormalizedValues = 0;
	}

	public void close() {
		if (br != null) {
			try {
				br.close();
			} catch (IOException e) {}
		}
	}

	public void setMetrics(String metrics) {
		if (metrics == null || metrics.trim().isEmpty()) {
			throw new IllegalArgumentException("metrics cannot be null or empty");
		}
		requestedMetricNames = new ArrayList<String>();
		String[] metricArray = metrics.split("[,;]");
		for (String metric : metricArray) {
			requestedMetricNames.add(metric);
		}
	}
	
	public void setDimensions(String dimensions) {
		if (dimensions == null || dimensions.trim().isEmpty()) {
			throw new IllegalArgumentException("dimensions cannot be null or empty");
		}
		requestedDimensionNames = new ArrayList<String>();
		String[] dimensionArray = dimensions.split("[,;]");
		for (String dimension : dimensionArray) {
			requestedDimensionNames.add(dimension.trim());
		}
		countDimensions = requestedDimensionNames.size();
	}
		
	private String extractInfoValue(String line, String key) {
		int pos = line.indexOf(key);
		if (pos > 0) {
			String valueStr = line.substring(pos + key.length());
			return valueStr.trim().replace("\"", "");
		} else {
			return null;
		}
	}
	
	private void extractInfo(String line) {
		if (line != null) {
			if (profileIdInfo == null) {
				profileIdInfo = extractInfoValue(line, PROFILE_KEY);
			}
			if (dimensionsInfo == null) {
				dimensionsInfo = extractInfoValue(line, DIMENSIONS_KEY);
			}
			if (metricsInfo == null) {
				metricsInfo = extractInfoValue(line, METRICS_KEY);
			}
			if (filtersInfo == null) {
				filtersInfo = extractInfoValue(line, FILTERS_KEY);
			}
			if (segmentInfo == null) {
				segmentInfo = extractInfoValue(line, SEGMENT_KEY);
			}
			if (startDateInfo == null) {
				startDateInfo = extractInfoValue(line, START_DATE_KEY);
			}
			if (endDateInfo == null) {
				endDateInfo = extractInfoValue(line, END_DATE_KEY);
			}
		}
	}
	
	private void processHeaderRows() throws Exception {
		dimensionsInfo = null;
		metricsInfo = null;
		filtersInfo = null;
		segmentInfo = null;
		startDateInfo = null;
		endDateInfo = null;
		profileIdInfo = null;
		String line = br.readLine();
		if (line != null) {
			extractInfo(line);
			// skip summary lines
			while (line != null && (line.startsWith("#") || line.startsWith("\"#"))) {
				line = br.readLine();
				extractInfo(line);
			}
			headerLine = line;
			validFile = (line != null); // skip the column header
			if (headerLine != null) {
				setupHeaderPositions();
			}
		} else {
			validFile = false;
		}
	}
	
	private void setupHeaderPositions() throws Exception {
		// header is simple comma separated
		int pos = 0;
		headers = new ArrayList<String>();
        data = getChars(headerLine);
        lastPosDel = 0;
        lastDelimiterIndex = 0;
		while (true) {
			String column = extractDataAtDelimiter(pos);
			if (column == null || column.isEmpty()) {
				break;
			} else {
				headers.add(column.toLowerCase());
			}
			pos++;
		}
	}
	
	public boolean hasNextPlainRecord() throws IOException {
		if (validFile) {
			currentLine = br.readLine();
			return currentLine != null && currentLine.trim().isEmpty() == false;
		} else {
			close();
			return false;
		}
	}
	
	public List<String> getNextPlainRecord() throws Exception {
		if (currentLine == null) {
			throw new IllegalStateException("call hasNextPlainRecord before and check return true!");
		}
		List<String> record = new ArrayList<String>();
        data = getChars(currentLine);
        lastPosDel = 0;
        lastDelimiterIndex = 0;
        // first read the dimensions
        for (String dim : requestedDimensionNames) {
        	int pos = headers.indexOf(dim.toLowerCase());
        	if (pos == -1) {
        		throw new Exception("Dimension " + dim + " not found in header line!");
        	}
        	String value = extractDataAtDelimiter(pos);
        	if (checkMissingFields && (value == null || value.trim().isEmpty())) {
        		throw new Exception("Missing value for dimension: " + dim + " in data line: " + currentPlainRowIndex);
        	}
			record.add(value);
        }
        for (String metric : requestedMetricNames) {
        	int pos = headers.indexOf(metric.toLowerCase());
        	if (pos == -1) {
        		throw new Exception("Metric " + metric + " not found in header line!");
        	}
        	String value = extractDataAtDelimiter(pos);
        	if (checkMissingFields && (value == null || value.trim().isEmpty())) {
        		throw new Exception("Missing value for metric: " + metric + " in data line: " + currentPlainRowIndex);
        	}
			record.add(value);
        }
		currentPlainRowIndex++;
		return record;
	}

	private void setMaxCountNormalizedValues(int count) {
		if (count > maxCountNormalizedValues) {
			maxCountNormalizedValues = count;
		}
	}

	public DimensionValue getCurrentDimensionValue() {
		if (currentNormalizedValueIndex == 0) {
			throw new IllegalStateException("Call nextNormalizedRecord() at first!");
		}
		if (currentNormalizedValueIndex <= currentResultRowDimensionValues.size()) {
			return currentResultRowDimensionValues.get(currentNormalizedValueIndex - 1);
		} else {
			return null;
		}
	}
	
	public Date getCurrentDate() {
		return currentDate;
	}
	
	public MetricValue getCurrentMetricValue() {
		if (currentNormalizedValueIndex == 0) {
			throw new IllegalStateException("Call nextNormalizedRecord() at first!");
		}
		if (currentNormalizedValueIndex <= currentResultRowMetricValues.size()) {
			return currentResultRowMetricValues.get(currentNormalizedValueIndex - 1);
		} else {
			return null;
		}
	}

	public boolean nextNormalizedRecord() throws Exception {
		if (maxCountNormalizedValues == 0) {
			// at start we do not have any records
			if (hasNextPlainRecord()) {
				buildNormalizedRecords(getNextPlainRecord());
			}
		}
		if (maxCountNormalizedValues > 0) {
			if (currentNormalizedValueIndex < maxCountNormalizedValues) {
				currentNormalizedValueIndex++;
				return true;
			} else if (currentNormalizedValueIndex == maxCountNormalizedValues) {
				// the end of the normalized rows reached, fetch the next data row
				if (hasNextPlainRecord()) {
					if (buildNormalizedRecords(getNextPlainRecord())) {
						currentNormalizedValueIndex++;
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean buildNormalizedRecords(List<String> oneRow) {
		maxCountNormalizedValues = 0;
		currentNormalizedValueIndex = 0;
		buildDimensionValues(oneRow);
		buildMetricValues(oneRow);
		return maxCountNormalizedValues > 0;
	}

	private List<DimensionValue> buildDimensionValues(List<String> oneRow) {
		int index = 0;
		currentDate = null;
		final List<DimensionValue> oneRowDimensionValues = new ArrayList<DimensionValue>();
		for (; index < requestedDimensionNames.size(); index++) {
			DimensionValue dm = new DimensionValue();
			dm.name = requestedDimensionNames.get(index);
			dm.value = oneRow.get(index);
			dm.rowNum = currentPlainRowIndex;
        	if (excludeDate && DATE_DIME.equalsIgnoreCase(dm.name.trim().toLowerCase())) {
        		try {
        			if (dm.value != null) {
    					currentDate = dateFormatter.parse(dm.value);
        			}
				} catch (ParseException e) {
					throw new RuntimeException("ga:date value=" + dm.value + " cannot be parsed as Date.", e);
				}
        	} else {
    			oneRowDimensionValues.add(dm);
        	}
		}
		currentResultRowDimensionValues = oneRowDimensionValues;
		setMaxCountNormalizedValues(currentResultRowDimensionValues.size());
		return oneRowDimensionValues;
	}

	private List<MetricValue> buildMetricValues(List<String> oneRow) {
		int index = 0;
		final List<MetricValue> oneRowMetricValues = new ArrayList<MetricValue>();
		for (; index < requestedMetricNames.size(); index++) {
			MetricValue mv = new MetricValue();
			mv.name = requestedMetricNames.get(index);
			mv.rowNum = currentPlainRowIndex;
			String valueStr = oneRow.get(index + countDimensions);
			try {
				mv.value = Util.convertToDouble(valueStr, Locale.ENGLISH.toString());
				oneRowMetricValues.add(mv);
			} catch (Exception e) {
				throw new IllegalStateException("Failed to build a double value for the metric:" + mv.name + " and value String:" + valueStr);
			}
		}
		currentResultRowMetricValues = oneRowMetricValues;
		setMaxCountNormalizedValues(currentResultRowMetricValues.size());
		return oneRowMetricValues;
	}

    private static final char[] getChars(String s) {
        if (s == null) {
            return new char[0];
        } else {
            return s.toCharArray();
        }
    }

    private char[] data;
	private int lastPosDel = 0;
	private int lastDelimiterIndex = 0;
    private char[] delimiterChars = {','};
    private char[] enclosureChars = {'"'};
    private boolean allowEnclosureInText = true;
	
	private String extractDataAtDelimiter(int fieldNum) throws Exception {
        String value = null;
        if (fieldNum < lastDelimiterIndex) {
        	throw new Exception("Current field index " + fieldNum + " is lower then last field index:" + lastDelimiterIndex);
        }
        int countDelimiters = lastDelimiterIndex;
        boolean inField = false;
        boolean atEnclosureStart = false;
        boolean atEnclosureStop = false;
        boolean atDelimiter = false;
        boolean useEnclosure = enclosureChars.length > 0;
        boolean fieldStartsWithEnclosure = false;
        boolean continueField = false;
        int currPos = lastPosDel;
        StringBuilder sb = new StringBuilder();
        while (currPos < data.length && countDelimiters <= fieldNum) {
            if (atEnclosureStart) {
                atEnclosureStart = false;
                fieldStartsWithEnclosure = true;
                currPos = currPos + enclosureChars.length;
                atEnclosureStop = startsWith(data, enclosureChars, currPos);
                if (atEnclosureStop == false) {
                    // don't check delimiter here because these chars are part of value
                    inField = true;
                }
            } else if (atEnclosureStop) {
                atEnclosureStop = false;
                currPos = currPos + enclosureChars.length;
                atDelimiter = startsWith(data, delimiterChars, currPos);
                if (atDelimiter == false && currPos < data.length) {
                	if (allowEnclosureInText) {
                		inField = true;
                		continueField = true;
                		sb.append(enclosureChars);
                	} else {
                        throw new Exception("Delimiter after enclosure stop missing at position:" + currPos + " in field number:" + fieldNum);
                	}
                }
            } else if (atDelimiter) {
                countDelimiters++;
                fieldStartsWithEnclosure = false;
                currPos = currPos + delimiterChars.length;
                atDelimiter = startsWith(data, delimiterChars, currPos);
                if (atDelimiter == false) {
                    if (useEnclosure && currPos < data.length) {
                        atEnclosureStart = startsWith(data, enclosureChars, currPos);
                        if (atEnclosureStart == false) {
                            inField = true;
                        }
                    } else {
                        inField = true;
                    }
                }
            } else if (inField) {
                if (continueField == false && countDelimiters == fieldNum) {
                    sb.setLength(0);
                }
                continueField = false;
                while (currPos < data.length) {
                    if (fieldStartsWithEnclosure) {
                        atEnclosureStop = startsWith(data, enclosureChars, currPos);
                        if (atEnclosureStop) {
                            break;
                        }
                    } else {
                        atDelimiter = startsWith(data, delimiterChars, currPos);
                        if (atDelimiter || atEnclosureStart) {
                            break;
                        }
                    }
                    if (countDelimiters == fieldNum) {
                        sb.append(data[currPos]);
                    }
                    currPos++;
                }
                inField = false;
                if (countDelimiters == fieldNum) {
                    value = sb.toString();
                }
            } else {
                if (useEnclosure) {
                    atEnclosureStart = startsWith(data, enclosureChars, currPos);
                }
                atDelimiter = startsWith(data, delimiterChars, currPos);
                if (atEnclosureStart == false && atDelimiter == false) {
                    inField = true;
                }
            }
        }
        lastPosDel = currPos;
        lastDelimiterIndex = fieldNum + 1;
        return value;
    }

    private static final boolean startsWith(char[] data, char[] search, int startPos) {
        if (search.length == 0 || data.length == 0) {
            return false;
        }
        if (startPos < 0 || startPos > (data.length - search.length)) {
            return false;
        }
        int searchPos = 0;
        int count = search.length;
        int dataPos = startPos;
        while (--count >= 0) {
            if (data[dataPos++] != search[searchPos++]) {
                return false;
            }
        }
        return true;
    }

	public String getProfileIdInfo() {
		return profileIdInfo;
	}

	public String getDimensionsInfo() {
		return dimensionsInfo;
	}

	public String getMetricsInfo() {
		return metricsInfo;
	}

	public String getFiltersInfo() {
		return filtersInfo;
	}

	public String getSegmentInfo() {
		return segmentInfo;
	}

	public String getStartDateInfo() {
		return startDateInfo;
	}

	public String getEndDateInfo() {
		return endDateInfo;
	}

	public boolean isSetDimensionsAndMetricsFromHeader() {
		return setDimensionsAndMetricsFromHeader;
	}

	public void setSetDimensionsAndMetricsFromHeader(
			boolean setDimensionsAndMetricsFromHeader) {
		this.setDimensionsAndMetricsFromHeader = setDimensionsAndMetricsFromHeader;
	}

	public boolean isExcludeDate() {
		return excludeDate;
	}

	public void setExcludeDate(boolean excludeDate) {
		this.excludeDate = excludeDate;
	}

}
