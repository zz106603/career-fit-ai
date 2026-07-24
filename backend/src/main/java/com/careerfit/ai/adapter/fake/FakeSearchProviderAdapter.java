package com.careerfit.ai.adapter.fake;

import com.careerfit.ai.port.SearchProviderPort;
import com.careerfit.ai.port.model.SearchRequest;
import com.careerfit.ai.port.model.SearchResponse;
import com.careerfit.ai.port.model.SearchResult;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class FakeSearchProviderAdapter implements SearchProviderPort {

    private final FakeProviderBehavior behavior;

    public FakeSearchProviderAdapter() {
        this(FakeProviderBehavior.SUCCESS);
    }

    public FakeSearchProviderAdapter(FakeProviderBehavior behavior) {
        this.behavior = Objects.requireNonNull(behavior, "behavior는 null일 수 없습니다.");
    }

    @Override
    public SearchResponse search(SearchRequest request) {
        Objects.requireNonNull(request, "request는 null일 수 없습니다.");
        FakeProviderSupport.verifyBehavior(behavior);

        String identifier = FakeProviderSupport.identifier(request.query());
        SearchResult result = new SearchResult(
                "Fake search result " + identifier,
                URI.create("https://fake.provider/search/" + identifier),
                "Deterministic result for query " + identifier);
        return new SearchResponse(List.of(result));
    }
}
