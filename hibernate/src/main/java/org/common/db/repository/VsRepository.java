package org.common.db.repository;

import org.common.db.entity.VSDetails;

import java.util.List;
import java.util.Optional;

public class VsRepository extends AbstractRepository {

    public Optional<VSDetails> findByName(String vsName) {
        return executeReadOnly(session ->
                session.createQuery(
                        "FROM VSDetails v WHERE v.vsName = :n",
                        VSDetails.class)
                        .setParameter("n", vsName)
                        .uniqueResultOptional()
        );
    }

    public List<VSDetails> findAll() {
        return executeReadOnly(session ->
                session.createQuery("FROM VSDetails", VSDetails.class).list()
        );
    }

    public VSDetails save(VSDetails entity) {
        return executeInSession(session ->
                (VSDetails) session.merge(entity)
        );
    }

    public void deleteByName(String vsName) {
        executeInSession(session -> {
            session.createMutationQuery(
                            "DELETE FROM VSDetails WHERE vsName = :n")
                    .setParameter("n", vsName)
                    .executeUpdate();
            return null;
        });
    }

    public long findVSID(String vsName) {
        Long id = executeReadOnly(session ->
                session.createQuery(
                        "SELECT v.vsid FROM VSDetails v WHERE v.vsName = :n",
                        Long.class)
                        .setParameter("n", vsName)
                        .uniqueResult()
        );
        return id != null ? id : -1;
    }

    public VSDetails update(VSDetails entity) {
        return executeInSession(session -> (VSDetails) session.merge(entity));
    }

    public List<VSDetails> findByStatus(String status) {
        return executeReadOnly(session ->
                session.createQuery(
                        "FROM VSDetails v WHERE v.status = :s",
                        VSDetails.class)
                        .setParameter("s", status)
                        .getResultList()
        );
    }

    public List<VSDetails> findByNameIgnoreCase(String vsName) {
        return executeReadOnly(session ->
                session.createQuery(
                        "FROM VSDetails v WHERE LOWER(v.vsName) = LOWER(:n)",
                        VSDetails.class)
                        .setParameter("n", vsName)
                        .getResultList()
        );
    }
}
