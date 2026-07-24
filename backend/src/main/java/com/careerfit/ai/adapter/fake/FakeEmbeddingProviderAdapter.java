package com.careerfit.ai.adapter.fake;

import com.careerfit.ai.port.EmbeddingProviderPort;
import com.careerfit.ai.port.model.EmbeddingRequest;
import com.careerfit.ai.port.model.EmbeddingResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class FakeEmbeddingProviderAdapter implements EmbeddingProviderPort {

    private static final int VECTOR_DIMENSION = 8;
    private static final String MODEL = "fake-embedding-v1";

    private final FakeProviderBehavior behavior;

    public FakeEmbeddingProviderAdapter() {
        this(FakeProviderBehavior.SUCCESS);
    }

    public FakeEmbeddingProviderAdapter(FakeProviderBehavior behavior) {
        this.behavior = Objects.requireNonNull(behavior, "behavior는 null일 수 없습니다.");
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        Objects.requireNonNull(request, "request는 null일 수 없습니다.");
        FakeProviderSupport.verifyBehavior(behavior);

        byte[] digest = FakeProviderSupport.digest(request.text());
        List<Double> vector = new ArrayList<>(VECTOR_DIMENSION);
        for (int index = 0; index < VECTOR_DIMENSION; index++) {
            vector.add(Byte.toUnsignedInt(digest[index]) / 255.0);
        }
        return new EmbeddingResponse(vector, MODEL);
    }
}
