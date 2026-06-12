package org.common.db.repository;

import org.common.db.entity.VSDetails;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class VsDetailsRepository extends AbstractRepository {

    public VsDetailsRepository(SessionFactory sessionFactory) { super(sessionFactory); }

    public Optional<VSDetails> findByVsName(String vsName) {
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
        return executeInSession(session -> (VSDetails) session.merge(entity));
    }

    public Optional<VSDetails> findById(Long id) {
        return executeReadOnly(session ->
                Optional.ofNullable(session.get(VSDetails.class, id))
        );
    }
}
