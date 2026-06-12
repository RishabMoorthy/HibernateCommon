package org.common.db.repository;

import org.common.db.entity.DailyMetrics;

import java.util.Optional;

public class DailyMetricsRepository extends AbstractRepository {

    public Optional<DailyMetrics> findByKey(String vsName,
                                            java.util.Date date,
                                            String virtServer) {

        return executeReadOnly(session ->
                session.createQuery(
                        "FROM DailyMetrics d WHERE d.vsName = :v " +
                                "AND d.transDate = :d AND d.virtServerName = :s",
                        DailyMetrics.class)
                        .setParameter("v", vsName)
                        .setParameter("d", date)
                        .setParameter("s", virtServer)
                        .uniqueResultOptional()
        );
    }

    public void save(DailyMetrics entity) {
        executeInSession(session -> {
            session.persist(entity);
            return null;
        });
    }

    public DailyMetrics update(DailyMetrics entity) {
        return executeInSession(session -> (DailyMetrics) session.merge(entity));
    }
}
