package com.ociproject.service.embedding;

import com.ociproject.model.*;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Hooks into Hibernate's POST_INSERT / POST_UPDATE / POST_DELETE events so we
 * can react to entity changes WITHOUT modifying any entity class or service
 * method. Each event is converted into a Spring application event that the
 * {@link EmbeddingEventDispatcher} consumes asynchronously after commit.
 *
 * Why Hibernate events and not @EntityListeners: JPA's @PostUpdate annotation
 * would require modifying every embeddable entity to add @EntityListeners and
 * would prevent Spring DI inside the listener. Hibernate's event registry is
 * Spring-friendly and zero-touch on the domain model.
 */
@Component
@RequiredArgsConstructor
public class HibernateChangeListener
        implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private static final Logger log = LoggerFactory.getLogger(HibernateChangeListener.class);

    private final EntityManagerFactory emf;
    private final ApplicationEventPublisher publisher;

    @PostConstruct
    void register() {
        SessionFactoryImplementor sf = emf.unwrap(SessionFactoryImplementor.class);
        EventListenerRegistry registry = sf.getServiceRegistry().getService(EventListenerRegistry.class);
        if (registry == null) {
            log.warn("Hibernate EventListenerRegistry not available — embedding change events disabled.");
            return;
        }
        registry.appendListeners(EventType.POST_COMMIT_INSERT, this);
        registry.appendListeners(EventType.POST_COMMIT_UPDATE, this);
        registry.appendListeners(EventType.POST_COMMIT_DELETE, this);
        // Some entities don't emit POST_COMMIT_* with default flush — also bind to
        // the non-transactional variants so we don't miss writes outside JTA.
        registry.appendListeners(EventType.POST_INSERT, this);
        registry.appendListeners(EventType.POST_UPDATE, this);
        registry.appendListeners(EventType.POST_DELETE, this);
        log.info("HibernateChangeListener registered for INSERT/UPDATE/DELETE.");
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        classify(event.getEntity(), false);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        // Soft-delete is a normal UPDATE that flips deleted=true. classify() inspects
        // the entity and emits a DELETE event in that case.
        classify(event.getEntity(), false);
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        // Physical delete (rare in this codebase — most entities use soft-delete).
        classify(event.getEntity(), true);
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    // ── dispatch ──

    private void classify(Object entity, boolean forcedDelete) {
        try {
            if (entity instanceof Project p) {
                publish(SourceType.PROJECT, p.getProjectId(), forcedDelete || Boolean.TRUE.equals(p.getDeleted()));
            } else if (entity instanceof Sprint s) {
                publish(SourceType.SPRINT, s.getSprintId(), forcedDelete || Boolean.TRUE.equals(s.getDeleted()));
            } else if (entity instanceof Task t) {
                publish(SourceType.TASK, t.getTaskId(), forcedDelete || Boolean.TRUE.equals(t.getDeleted()));
            } else if (entity instanceof User u) {
                publish(SourceType.USER, u.getUserId(), forcedDelete || Boolean.TRUE.equals(u.getDeleted()));
            } else if (entity instanceof Team team) {
                publish(SourceType.TEAM, team.getTeamId(), forcedDelete || Boolean.TRUE.equals(team.getDeleted()));
            } else if (entity instanceof KpiValue k) {
                publish(SourceType.KPI_VALUE, k.getKpiValueId(), forcedDelete);
            } else if (entity instanceof Deployment d) {
                publish(SourceType.DEPLOYMENT, d.getDeploymentId(), forcedDelete);
            } else if (entity instanceof Incident i) {
                publish(SourceType.INCIDENT, i.getIncidentId(), forcedDelete || Boolean.TRUE.equals(i.getDeleted()));
            }
        } catch (Exception e) {
            log.warn("Failed to dispatch embedding event for {}: {}",
                    entity != null ? entity.getClass().getSimpleName() : "null", e.getMessage());
        }
    }

    private void publish(SourceType type, Long id, boolean delete) {
        if (id == null) return;
        publisher.publishEvent(delete
                ? EmbeddingChangeEvent.delete(type, id)
                : EmbeddingChangeEvent.upsert(type, id));
    }
}
