package ru.tecon.parser.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportData implements Serializable {

	private String fileName;
	private String address, counterType, counterNumber, reportType;
	private LocalDate startDate, endDate;
	private List<ParameterData> param = new ArrayList<>(),
			paramIntegr = new ArrayList<>();
	
	public ReportData() {	
	}
	
	public void setAddress(String pAddress) {
		address = pAddress;
	}
	
	public void setCounterType(String pCounterType) {
		counterType = pCounterType;
	}
	
	public void setCounterNumber(String pCounterNumber) {
		counterNumber = pCounterNumber;
	}
	
	public void setParam(List<ParameterData> pParam) {
		param = pParam;
	}
	
	public void setParamIntegr(List<ParameterData> pParamIntegr) {
		paramIntegr = pParamIntegr;
	}
	
	public void setStartDate(LocalDate pStartDate) {
		startDate = pStartDate;
	}
	
	public void setEndDate(LocalDate pEndDate) {
		endDate = pEndDate;
	}
	
	public void setReportType(String reportType) {
		this.reportType = reportType;
	}
	
	public String getAddress() {
		return address;
	}
	
	public String getCounterType() {
		return counterType;
	}
	
	public String getCounterNumber() {
		return counterNumber;
	}
	
	public List<ParameterData> getParam() {
		return param;
	}
	
	public List<ParameterData> getParamIntegr() {
		return paramIntegr;
	}
	
	public String getReportType() {
		return reportType;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public boolean checkData() {
		paramIntegr.forEach(ParameterData::sort);
		return address != null
				&& param.size() > 1
				&& param.get(0).getData().size() != 0
				&& checkData(param)
				&& checkData(paramIntegr)
				&& checkDataNull(param)
				&& checkDataNull(paramIntegr);
	}
	
	private boolean checkData(List<ParameterData> data) {
		if (data.size() <= 1 || data.get(0).getData().size() == 0) {
			return true;
		}
		int size = data.get(0).getData().size();
		for (int i = 0; i < data.size(); i++) {
			if (data.get(i).getData().size() != size) {
				return false;
			}
			for (String obj: data.get(i).getData()) {
				if (i == 0) {
					if (!obj.matches("\\d{2}[.]\\d{2}[.]\\d{4}")) {
						return false;
					}
				} else {
					if (!obj.equals("")) {
						try {
							new BigDecimal(obj);
						} catch (NumberFormatException e) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}
	
	private boolean checkDataNull(List<ParameterData> data) {
		if (data.size() <= 1 || data.get(0).getData().size() == 0) {
			return true;
		}
		boolean flag;
		for (int i = 0; i < data.get(0).getData().size(); i++) {
			flag = false;
			for (int j = 1; j < data.size(); j++) {
				if (!data.get(j).getData().get(i).equals("")) {
					flag = true;
					break;
				}
			}
			if (!flag) {
				return false;
			}
		}
		return true;
	}
	
	public void print() {
		System.out.println();
		System.out.println("start");
		System.out.println("fileName: " + fileName);
		System.out.println("reportType: " + reportType);
		System.out.println("address: " + address);
		System.out.println("counterType: " + counterType);
		System.out.println("counterNumber: " + counterNumber);
		System.out.println("startDate: " + startDate);
		System.out.println("endDate: " + endDate);
		System.out.println("param:");
		param.forEach(System.out::println);
		System.out.println("paramIntegr:");
		paramIntegr.forEach(System.out::println);
		System.out.println("end");
	}
}