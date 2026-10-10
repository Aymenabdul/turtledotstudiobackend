package com.turtledot.studio.employee.dto;

import java.util.List;

public class AssignPermissionsRequest {
    private List<String> permissions;

    public AssignPermissionsRequest() {}

    public AssignPermissionsRequest(List<String> permissions) {
        this.permissions = permissions;
    }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
}
