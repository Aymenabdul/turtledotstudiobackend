package com.turtledot.studio.employee.dto;

import java.util.List;

public class AssignClientsRequest {
    private List<Long> clientIds;

    public AssignClientsRequest() {}

    public AssignClientsRequest(List<Long> clientIds) {
        this.clientIds = clientIds;
    }

    public List<Long> getClientIds() { return clientIds; }
    public void setClientIds(List<Long> clientIds) { this.clientIds = clientIds; }
}
