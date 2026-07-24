package com.careerfit.ai.port;

import com.careerfit.ai.port.model.SearchRequest;
import com.careerfit.ai.port.model.SearchResponse;

/** Provider 제품과 무관하게 외부 검색을 요청하는 경계다. */
public interface SearchProviderPort {

    SearchResponse search(SearchRequest request);
}
