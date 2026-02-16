package consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.itmo.secs.domain.model.entities.Notification;
import org.itmo.secs.application.services.DishEventsConsumer;
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
class DishEventsConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DishEventsConsumer consumer;

    @Test
    void shouldHandleCreatedEvent() throws Exception {
        String json = """
            {
              "type":"CREATED",
              "data":{
                "id":1,
                "name":"Soup"
              }
            }
        """;

        JsonNode node = new ObjectMapper().readTree(json);

        when(objectMapper.readTree(json)).thenReturn(node);
        when(notificationService.saveForSubscribers(any())).thenReturn(Mono.empty());

        consumer.consumeMessage(json);

        verify(notificationService).saveForSubscribers(any(Notification.class));
    }

    @Test
    void shouldHandleDeletedEvent() throws Exception {
        String json = """
            {
              "type":"DELETED",
              "data":{
                "id":"5"
              }
            }
        """;

        JsonNode node = new ObjectMapper().readTree(json);

        when(objectMapper.readTree(json)).thenReturn(node);
        when(notificationService.saveForSubscribers(any())).thenReturn(Mono.empty());

        consumer.consumeMessage(json);

        verify(notificationService).saveForSubscribers(any(Notification.class));
    }

    @Test
    void shouldHandleUpdatedEvent() throws Exception {
        String json = """
        {
          "type":"UPDATED",
          "data":{
            "id":"3",
            "name":{"old":"Tea","new":"Coffee"},
            "items":[
              {"id":10,"count":50}
            ]
          }
        }
        """;

        JsonNode node = new ObjectMapper().readTree(json);

        when(objectMapper.readTree(json)).thenReturn(node);
        when(notificationService.saveForSubscribers(any())).thenReturn(Mono.empty());

        consumer.consumeMessage(json);

        verify(notificationService).saveForSubscribers(any(Notification.class));
    }
}
