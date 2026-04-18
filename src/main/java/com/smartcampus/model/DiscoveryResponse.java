package com.smartcampus.model;

import java.util.HashMap;
import java.util.Map;

public class DiscoveryResponse {
    private String version;
    private String adminContact;
    private Map<String, String> links = new HashMap<>();

    public DiscoveryResponse() {
    }

    public DiscoveryResponse(String version, String adminContact) {
        this.version = version;
        this.adminContact = adminContact;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAdminContact() {
        return adminContact;
    }

    public void setAdminContact(String adminContact) {
        this.adminContact = adminContact;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public void setLinks(Map<String, String> links) {
        this.links = links;
    }

    public void addLink(String rel, String href) {
        this.links.put(rel, href);
    }
}
