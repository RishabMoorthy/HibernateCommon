package org.common.db.repository;

import org.common.db.entity.ExecutionDetailsDTO;

import java.util.Optional;

public class ExecutionDetailsRepository extends AbstractRepository {

    public Optional<ExecutionDetailsDTO> findExecutionDetails(String vsName, String virtServer) {

        return executeReadOnly(session -> {
            Optional<Object[]> result = session.createQuery(
                    "SELECT l.host, m.executionMode " +
                            "FROM VsLiveUrl l, VsExecutionMode m, VsCatalog c " +
                            "WHERE l.vsid = m.vsid " +
                            "AND m.masterId = c.masterId " +
                            "AND c.vsName = :vsName " +
                            "AND m.virtServer = :virtServer " +
                            "AND l.isActive = 'Y'",
                    Object[].class)
                    .setParameter("vsName", vsName)
                    .setParameter("virtServer", virtServer)
                    .setMaxResults(1)
                    .uniqueResultOptional();

            if (result.isEmpty()) {
                return Optional.empty();
            }

            Object[] row = result.get();
            String host = (String) row[0];
            String executionMode = (String) row[1];

            return Optional.of(new ExecutionDetailsDTO(host, executionMode));
        });
    }
}
