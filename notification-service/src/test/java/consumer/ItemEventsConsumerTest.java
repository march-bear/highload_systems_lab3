package consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.itmo.secs.application.services.ItemEventsConsumer;
import org.itmo.secs.application.services.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemEventsConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ItemEventsConsumer consumer;

    private final ObjectMapper realMapper = new ObjectMapper();

    @Test
    void consume_createdEvent() throws Exception {

        String json = """
        {
          "type":"CREATED",
          "data":{
            "id":1,
            "name":"Rice",
            "calories":100,
            "carbs":20,
            "protein":5,
            "fats":1
          }
        }
        """;

        JsonNode node = realMapper.readTree(json);

        when(objectMapper.readTree(json)).thenReturn(node);
        when(notificationService.saveForSubscribers(any())).thenReturn(Mono.empty());

        consumer.consumeMessage(json);

        verify(notificationService).saveForSubscribers(any());
    }

    @Test
    void consume_deletedEvent() throws Exception {

        String json = """
        {
          "type":"DELETED",
          "data":{
            "id":"2",
            "name":"Rice",
            "calories":100,
            "carbs":20,
            "protein":5,
            "fats":1
          }
        }
        """;

        JsonNode node = realMapper.readTree(json);

        when(objectMapper.readTree(json)).thenReturn(node);
        when(notificationService.saveForSubscribers(any())).thenReturn(Mono.empty());

        consumer.consumeMessage(json);

        verify(notificationService).saveForSubscribers(any());
    }

    @Test
    void consume_updatedEvent() throws Exception {

        String json = """
        {
          "type":"UPDATED",
          "data":{
            "id":"2",
            "name":{"old":"Rice","new":"Brown rice"},
            "calories":{"old":100,"new":110},
            "carbs":{"old":20,"new":25},
            "protein":{"old":5,"new":6},
            "fats":{"old":1,"new":2}
          }
        }
        """;

        JsonNode node = realMapper.readTree(json);

        when(objectMapper.readTree(json)).thenReturn(node);
        when(notificationService.saveForSubscribers(any())).thenReturn(Mono.empty());

        consumer.consumeMessage(json);

        verify(notificationService).saveForSubscribers(any());
    }

    @Test
    void consume_unknownType() throws Exception {
        String json = """
            {"type":"UNKNOWN"}
        """;

        JsonNode node = realMapper.readTree(json);
        when(objectMapper.readTree(json)).thenReturn(node);

        consumer.consumeMessage(json);

        verify(notificationService, never()).saveForSubscribers(any());
    }
}
