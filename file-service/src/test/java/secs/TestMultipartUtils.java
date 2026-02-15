package secs;

import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;

public class TestMultipartUtils {

    public static MultiValueMap<String, HttpEntity<?>> file(String partName, String filename, String content) {

        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        builder.part(partName, content.getBytes())
                .filename(filename)
                .contentType(MediaType.TEXT_PLAIN);

        return builder.build();
    }
}
