package org.common.db.repository;

import org.common.db.entity.PortRange;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class PortRangeRepository extends AbstractRepository {

    public PortRangeRepository(SessionFactory sessionFactory) { super(sessionFactory); }

    public List<PortRange> findAll() {
        return executeReadOnly(session ->
                session.createQuery("FROM PortRange", PortRange.class).list()
        );
    }

    public boolean existsByAppName(String appName) {
        Long count = executeReadOnly(session ->
                session.createQuery(
                        "SELECT COUNT(p) FROM PortRange p WHERE p.appName = :n",
                        Long.class)
                        .setParameter("n", appName)
                        .uniqueResult()
        );
        return count != null && count > 0;
    }

    public Optional<PortRange> findTopByPortIdDesc() {
        return executeReadOnly(session ->
                session.createQuery(
                        "FROM PortRange ORDER BY portId DESC",
                        PortRange.class)
                        .setMaxResults(1)
                        .uniqueResultOptional()
        );
    }

    public Optional<PortRange> findById(Long id) {
        return executeReadOnly(session ->
                Optional.ofNullable(session.get(PortRange.class, id))
        );
    }

    public boolean existsById(Long id) {
        return executeReadOnly(session ->
                session.get(PortRange.class, id) != null
        );
    }

    public PortRange save(PortRange entity) {
        return executeInSession(session -> (PortRange) session.merge(entity));
    }

    public void deleteById(Long id) {
        executeInSession(session -> {
            PortRange p = session.get(PortRange.class, id);
            if (p != null) session.remove(p);
            return null;
        });
    }
}
