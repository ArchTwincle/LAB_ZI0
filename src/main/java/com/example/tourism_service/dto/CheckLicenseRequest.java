package com.example.tourism_service.dto;

public class CheckLicenseRequest {

    private String licenseCode;
    private String macAddress;

    public CheckLicenseRequest() {
    }

    public String getLicenseCode() {
        return licenseCode;
    }

    public void setLicenseCode(String licenseCode) {
        this.licenseCode = licenseCode;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
}