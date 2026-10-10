package com.turtledot.studio.search;

import com.turtledot.studio.album.Album;
import com.turtledot.studio.album.AlbumRepository;
import com.turtledot.studio.client.Client;
import com.turtledot.studio.client.ClientRepository;
import com.turtledot.studio.employee.Employee;
import com.turtledot.studio.employee.EmployeeClientAccess;
import com.turtledot.studio.employee.EmployeeClientAccessRepository;
import com.turtledot.studio.employee.EmployeeRepository;
import com.turtledot.studio.event.Event;
import com.turtledot.studio.event.EventRepository;
import com.turtledot.studio.search.dto.GlobalSearchResultDto;
import com.turtledot.studio.security.UserPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final ClientRepository clientRepository;
    private final EventRepository eventRepository;
    private final AlbumRepository albumRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeClientAccessRepository accessRepository;

    public SearchService(ClientRepository clientRepository,
                         EventRepository eventRepository,
                         AlbumRepository albumRepository,
                         EmployeeRepository employeeRepository,
                         EmployeeClientAccessRepository accessRepository) {
        this.clientRepository = clientRepository;
        this.eventRepository = eventRepository;
        this.albumRepository = albumRepository;
        this.employeeRepository = employeeRepository;
        this.accessRepository = accessRepository;
    }

    @Transactional(readOnly = true)
    public GlobalSearchResultDto globalSearch(UserPrincipal user, String query) {
        GlobalSearchResultDto result = new GlobalSearchResultDto();
        if (query == null || query.trim().length() < 2) {
            return result;
        }

        String q = query.trim();
        Long studioId = user.getStudioId();
        boolean isEmployee = "EMPLOYEE".equals(user.getRole().name());

        Set<Long> allowedClientIds = null;
        if (isEmployee) {
            Employee employee = employeeRepository.findByUserId(user.getId()).orElse(null);
            if (employee != null) {
                allowedClientIds = accessRepository.findByEmployeeId(employee.getId()).stream()
                        .map(EmployeeClientAccess::getClientId)
                        .collect(Collectors.toSet());
            } else {
                allowedClientIds = Collections.emptySet();
            }
        }

        // 1. Search Clients
        List<Client> matchedClients = clientRepository.searchClients(studioId, null, q, PageRequest.of(0, 8)).getContent();
        for (Client c : matchedClients) {
            if (allowedClientIds == null || allowedClientIds.contains(c.getId())) {
                result.getClients().add(new GlobalSearchResultDto.ClientItem(
                        c.getId(), c.getClientCode(), c.getName(), c.getEmail(), c.getPhone()
                ));
            }
        }

        // 2. Search Events
        List<Event> matchedEvents = eventRepository.searchEvents(studioId, null, null, q, PageRequest.of(0, 8)).getContent();
        Map<Long, String> clientNameMap = new HashMap<>();
        for (Event e : matchedEvents) {
            if (allowedClientIds == null || allowedClientIds.contains(e.getClientId())) {
                String cName = clientNameMap.computeIfAbsent(e.getClientId(), id ->
                        clientRepository.findById(id).map(Client::getName).orElse("Client"));
                result.getEvents().add(new GlobalSearchResultDto.EventItem(
                        e.getId(), e.getEventCode(), e.getEventName(), e.getEventType(), cName
                ));
            }
        }

        // 3. Search Albums
        List<Album> albums = albumRepository.findByStudioId(studioId);
        String lowerQ = q.toLowerCase();
        for (Album a : albums) {
            if (a.getName().toLowerCase().contains(lowerQ)) {
                if (allowedClientIds == null || allowedClientIds.contains(a.getClientId())) {
                    String cName = clientNameMap.computeIfAbsent(a.getClientId(), id ->
                            clientRepository.findById(id).map(Client::getName).orElse("Client"));
                    String evName = eventRepository.findById(a.getEventId()).map(Event::getEventName).orElse("Event");
                    result.getAlbums().add(new GlobalSearchResultDto.AlbumItem(
                            a.getId(), a.getName(), evName, cName
                    ));
                    if (result.getAlbums().size() >= 8) break;
                }
            }
        }

        return result;
    }
}
