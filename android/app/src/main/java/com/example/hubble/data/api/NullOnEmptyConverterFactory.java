package com.example.hubble.data.api;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Intercepts empty response bodies BEFORE GsonConverterFactory processes them.
 * Prevents EOFException when backend returns 200/204 with no body (e.g., DELETE endpoints).
 */
public class NullOnEmptyConverterFactory extends Converter.Factory {

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(
            Type type, Annotation[] annotations, Retrofit retrofit) {

        final Converter<ResponseBody, ?> delegate =
                retrofit.nextResponseBodyConverter(this, type, annotations);

        return body -> {
            // contentLength() == 0 covers Content-Length: 0 header
            if (body.contentLength() == 0) return null;
            // Peek the source to check actual bytes (handles chunked transfer with no content)
            try {
                Buffer buffer = new Buffer();
                body.source().peek().readAll(buffer);
                if (buffer.size() == 0) return null;
            } catch (IOException ignored) {}
            return delegate.convert(body);
        };
    }
}


