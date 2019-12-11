package com.iiot.edgenode.entity;

public class EdgeNode {
	
	private String hostName;
	private String ip;
	private String runStatus;
	private String deviceBindStatus;
	private int appNum;
	
	
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getRunStatus() {
		return runStatus;
	}
	public void setRunStatus(String runStatus) {
		this.runStatus = runStatus;
	}
	public String getDeviceBindStatus() {
		return deviceBindStatus;
	}
	public void setDeviceBindStatus(String deviceBindStatus) {
		this.deviceBindStatus = deviceBindStatus;
	}
	public int getAppNum() {
		return appNum;
	}
	public void setAppNum(int appNum) {
		this.appNum = appNum;
	}

}
