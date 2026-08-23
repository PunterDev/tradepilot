package com.tradepilot.observability.dto;

public class ErrorStatusBreakdownDto {
    private int status;
    private long count;

    public ErrorStatusBreakdownDto() {
    }

    public ErrorStatusBreakdownDto(int status, long count) {
        this.status = status;
        this.count = count;
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
