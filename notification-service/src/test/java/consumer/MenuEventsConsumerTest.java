package consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.itmo.secs.services.MenuEventsConsumer;
import org.itmo.secs.services.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class MenuEventsConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MenuEventsConsumer consumer;

    private final ObjectMapper realMapper = new ObjectMapper();

    @Test
    void consume_createdEvent() throws Exception {

        String json = """
        {
          "type":"CREATED",
          "data":{
            "id":1,
            "date":"2025-01-01",
            "meal":"Breakfast",
            "user_id":7
          }
        }
        """;

        JsonNode node = realMapper.readTree(json);

        when(objectMapper.readTree(json)).thenReturn(node);
        when(notificationService.save(any())).thenReturn(Mono.empty());

        consumer.consumeMessage(json);

        verify(notificationService).save(any());
    }

    @Test
    void consume_deletedEvent() throws Exception {

        String json = """
        {
          "type":"DELETED",
          "data":{
            "id":1,
            "date":"2025-01-01",
            "meal":"Breakfast",
            "user_id":7
          }
        }
        """;

        JsonNode node = realMapper.readTree(json);

        when(objectMapper.readTree(json)).thenReturn(node);
        when(notificationService.save(any())).thenReturn(Mono.empty());

        consumer.consumeMessage(json);

        verify(notificationService).save(any());
    }

    @Test
    void consume_updatedEvent() throws Exception {

        String json = """
        {
          "type":"UPDATED",
          "data":{
            "id":1,
            "user_id":7,
            "date":{"old":"2025-01-01","new":"2025-01-02"},
            "meal":{"old":"Breakfast","new":"Dinner"},
            "deleted_dish":2,
            "inserted_dish":3
          }
        }
        """;

        JsonNode node = realMapper.readTree(json);

        when(objectMapper.readTree(json)).thenReturn(node);
        when(notificationService.save(any())).thenReturn(Mono.empty());

        consumer.consumeMessage(json);

        verify(notificationService).save(any());
    }

    @Test
    void consume_unknownType() throws Exception {
        String json = """
            {"type":"XXX"}
        """;

        JsonNode node = realMapper.readTree(json);
        when(objectMapper.readTree(json)).thenReturn(node);

        consumer.consumeMessage(json);

        verify(notificationService, never()).save(any());
    }
}

