package consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.itmo.secs.services.NotificationService;
import org.itmo.secs.services.UserEventsConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventsConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UserEventsConsumer consumer;

    private final ObjectMapper realMapper = new ObjectMapper();

    @Test
    void consume_createdEvent() throws Exception {

        String json = """
        {
          "type":"CREATED",
          "user_id":5,
          "data":{
            "id":10,
            "name":"Alex",
            "role":"ADMIN"
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
          "user_id":5,
          "data":{
            "id":10,
            "name":"Alex",
            "role":"ADMIN"
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
          "user_id":5,
          "data":{
            "id":10,
            "role":{"old":"USER","new":"ADMIN"}
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
            {"type":"WHAT"}
        """;

        JsonNode node = realMapper.readTree(json);
        when(objectMapper.readTree(json)).thenReturn(node);

        consumer.consumeMessage(json);

        verify(notificationService, never()).save(any());
    }
}
