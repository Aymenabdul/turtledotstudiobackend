package com.turtledot.studio.search.dto;

import java.util.ArrayList;
import java.util.List;

public class GlobalSearchResultDto {

    private List<ClientItem> clients = new ArrayList<>();
    private List<EventItem> events = new ArrayList<>();
    private List<AlbumItem> albums = new ArrayList<>();

    public GlobalSearchResultDto() {}

    public static class ClientItem {
        private Long id;
        private String clientCode;
        private String name;
        private String email;
        private String phone;

        public ClientItem(Long id, String clientCode, String name, String email, String phone) {
            this.id = id;
            this.clientCode = clientCode;
            this.name = name;
            this.email = email;
            this.phone = phone;
        }

        public Long getId() { return id; }
        public String getClientCode() { return clientCode; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
    }

    public static class EventItem {
        private Long id;
        private String eventCode;
        private String name;
        private String eventType;
        private String clientName;

        public EventItem(Long id, String eventCode, String name, String eventType, String clientName) {
            this.id = id;
            this.eventCode = eventCode;
            this.name = name;
            this.eventType = eventType;
            this.clientName = clientName;
        }

        public Long getId() { return id; }
        public String getEventCode() { return eventCode; }
        public String getName() { return name; }
        public String getEventType() { return eventType; }
        public String getClientName() { return clientName; }
    }

    public static class AlbumItem {
        private Long id;
        private String name;
        private String eventName;
        private String clientName;

        public AlbumItem(Long id, String name, String eventName, String clientName) {
            this.id = id;
            this.name = name;
            this.eventName = eventName;
            this.clientName = clientName;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getEventName() { return eventName; }
        public String getClientName() { return clientName; }
    }

    // Getters and Setters
    public List<ClientItem> getClients() { return clients; }
    public void setClients(List<ClientItem> clients) { this.clients = clients; }

    public List<EventItem> getEvents() { return events; }
    public void setEvents(List<EventItem> events) { this.events = events; }

    public List<AlbumItem> getAlbums() { return albums; }
    public void setAlbums(List<AlbumItem> albums) { this.albums = albums; }
}
