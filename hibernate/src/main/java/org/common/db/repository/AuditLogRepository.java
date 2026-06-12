package org.common.db.repository;

import org.common.db.entity.AuditLog;
import org.hibernate.SessionFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class AuditLogRepository extends AbstractRepository {

    public AuditLogRepository(SessionFactory sessionFactory) { super(sessionFactory); }

    public AuditLog save(AuditLog entity) {
        return executeInSession(session -> (AuditLog) session.merge(entity));
    }

    public List<AuditLog> findAllOrderByTimestampDesc() {
        return executeReadOnly(session ->
                session.createQuery(
                        "FROM AuditLog a ORDER BY a.timestamp DESC",
                        AuditLog.class)
                        .getResultList()
        );
    }

    public Optional<AuditLog> findFirstByActionTypeIn(Collection<String> actionTypes) {
        return executeReadOnly(session ->
                session.createQuery(
                        "FROM AuditLog a WHERE a.actionType IN :types ORDER BY a.timestamp DESC",
                        AuditLog.class)
                        .setParameter("types", actionTypes)
                        .setMaxResults(1)
                        .uniqueResultOptional()
        );
    }
}
