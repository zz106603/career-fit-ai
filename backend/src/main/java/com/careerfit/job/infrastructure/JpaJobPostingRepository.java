package com.careerfit.job.infrastructure;

import com.careerfit.identity.UserId;
import com.careerfit.job.application.JobPostingRepository;
import com.careerfit.job.domain.JobPosting;
import com.careerfit.job.domain.JobPostingId;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaJobPostingRepository implements JobPostingRepository {

    private final SpringDataJobPostingRepository repository;

    public JpaJobPostingRepository(SpringDataJobPostingRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(JobPosting jobPosting) {
        repository.save(new JobPostingEntity(
                jobPosting.id().value(),
                jobPosting.userId().value(),
                jobPosting.originalText(),
                jobPosting.titleHint(),
                jobPosting.companyHint(),
                jobPosting.registeredAt(),
                jobPosting.deletedAt()));
    }

    @Override
    public Optional<JobPosting> findActive(UserId userId, JobPostingId jobPostingId) {
        return repository
                .findByIdAndUserIdAndDeletedAtIsNull(
                        jobPostingId.value(), userId.value())
                .map(this::toDomain);
    }

    @Override
    public boolean delete(UserId userId, JobPostingId jobPostingId, Instant deletedAt) {
        return repository.softDelete(jobPostingId.value(), userId.value(), deletedAt) == 1;
    }

    private JobPosting toDomain(JobPostingEntity entity) {
        return new JobPosting(
                new JobPostingId(entity.id()),
                new UserId(entity.userId()),
                entity.originalText(),
                entity.titleHint(),
                entity.companyHint(),
                entity.registeredAt(),
                entity.deletedAt());
    }
}
