package org.common.db.entity;

public class ExecutionDetailsDTO {

    private String host;
    private String executionMode;

    public ExecutionDetailsDTO(String host, String executionMode) {
        this.host = host;
        this.executionMode = executionMode;
    }

    public String getHost() { return host; }
    public String getExecutionMode() { return executionMode; }
}
