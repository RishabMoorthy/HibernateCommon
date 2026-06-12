package org.common.db.repository;

import org.common.db.entity.VsExecutionMode;

import java.util.List;

public class VsExecutionModeRepository extends AbstractRepository {

    public List<VsExecutionMode> findByMasterId(Long masterId) {
        return executeInSession(session ->
                session.createQuery(
                        "FROM VsExecutionMode m WHERE m.masterId = :masterId",
                        VsExecutionMode.class)
                        .setParameter("masterId", masterId)
                        .getResultList()
        );
    }
}
