package demo.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/event")
@RequiredArgsConstructor
public class EventController {

    private final List<Event> openEvents = Collections.synchronizedList(new ArrayList<>());
    private final List<Event> closeEvents = Collections.synchronizedList(new ArrayList<>());
    // autowired
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<EventSummary> getEvents(
            @RequestParam(value = "includeOpen", required = false, defaultValue = "true") boolean includeOpen,
            @RequestParam(value = "includeClose", required = false, defaultValue = "true") boolean includeClose) {

        List<Event> openEvents = Collections.emptyList(), closeEvents = Collections.emptyList();

        if (includeOpen) {
            openEvents = Collections.unmodifiableList(this.openEvents);
        }

        if (includeClose) {
            closeEvents = Collections.unmodifiableList(this.closeEvents);
        }

        return ResponseEntity.ok(EventSummary.builder().openEvents(openEvents).closeEvents(closeEvents).build());
    }

    @PostMapping
    public ResponseEntity<Void> handleOpenEvent(HttpServletRequest request) {
        try {
            final Event openEvent = convertToEvent(request);
            openEvents.add(openEvent);
            logger.info("## [Notification] POST /api/event.\n{}",
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(openEvent));
        } catch (Exception e) {
            logger.error("Exception occur while read request", e);
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> handleCloseEvent(HttpServletRequest request) {
        try {
            final Event closeEvent = convertToEvent(request);
            closeEvents.add(closeEvent);
            logger.info("## [Notification] DELETE /api/event.\n{}",
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(closeEvent));
        } catch (Exception e) {
            logger.error("Exception occur while read request", e);
        }
        return ResponseEntity.ok().build();
    }

    private Event convertToEvent(HttpServletRequest request) throws Exception {
        // extract headers
        final Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            final String key = headerNames.nextElement();
            headers.put(key, request.getHeader(key));
        }

        // bind body
        final Map<String, Object> body = objectMapper.readValue(request.getReader(),
                                                                new TypeReference<Map<String, Object>>() {});

        return Event.builder().headers(headers).body(body).build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    private static class EventSummary {
        private List<Event> openEvents;
        private List<Event> closeEvents;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    private static class Event {
        private Map<String, String> headers;
        private Map<String, Object> body;
    }
}
