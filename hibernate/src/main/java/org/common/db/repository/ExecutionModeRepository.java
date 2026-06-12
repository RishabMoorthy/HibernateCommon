package org.common.db.repository;

import org.common.db.entity.VsExecutionMode;
import org.hibernate.SessionFactory;

import java.util.Optional;

public class ExecutionModeRepository extends AbstractRepository {

    public ExecutionModeRepository(SessionFactory sessionFactory) { super(sessionFactory); }

    public Optional<VsExecutionMode> findByMasterIdAndVirtServer(Long masterId, String virtServer) {
        return executeReadOnly(session ->
                session.createQuery(
                        "FROM VsExecutionMode m WHERE m.masterId = :mid AND m.virtServer = :vs",
                        VsExecutionMode.class)
                        .setParameter("mid", masterId)
                        .setParameter("vs", virtServer)
                        .uniqueResultOptional()
        );
    }

    public VsExecutionMode save(VsExecutionMode entity) {
        return executeInSession(session -> (VsExecutionMode) session.merge(entity));
    }
}
