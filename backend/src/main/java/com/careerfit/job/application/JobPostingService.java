package com.careerfit.job.application;

import com.careerfit.identity.CurrentUserProvider;
import com.careerfit.identity.UserId;
import com.careerfit.job.domain.JobPosting;
import com.careerfit.job.domain.JobPostingId;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 사용자가 붙여넣은 채용공고 원문을 소유권 범위 안에서 등록·조회한다. */
public class JobPostingService {

    private final JobPostingRepository repository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public JobPostingService(
            JobPostingRepository repository,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Transactional
    public JobPosting create(String companyHint, String titleHint, String originalText) {
        UserId userId = currentUserProvider.currentUserId();
        JobPosting jobPosting = new JobPosting(
                JobPostingId.newId(),
                userId,
                originalText,
                titleHint,
                companyHint,
                clock.instant(),
                null);
        repository.save(jobPosting);
        return jobPosting;
    }

    @Transactional(readOnly = true)
    public JobPosting find(JobPostingId jobPostingId) {
        return repository
                .findActive(currentUserProvider.currentUserId(), jobPostingId)
                .orElseThrow(JobPostingNotFoundException::new);
    }

    @Transactional
    public void delete(JobPostingId jobPostingId) {
        if (!repository.delete(
                currentUserProvider.currentUserId(), jobPostingId, clock.instant())) {
            throw new JobPostingNotFoundException();
        }
    }
}
