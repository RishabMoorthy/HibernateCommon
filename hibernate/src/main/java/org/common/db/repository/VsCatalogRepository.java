package org.common.db.repository;

import org.common.db.entity.VsCatalog;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class VsCatalogRepository extends AbstractRepository {

    public VsCatalogRepository(SessionFactory sessionFactory) { super(sessionFactory); }

    public Optional<VsCatalog> findByVsName(String vsName) {
        return executeReadOnly(session ->
                session.createQuery(
                        "FROM VsCatalog v WHERE v.vsName = :n",
                        VsCatalog.class)
                        .setParameter("n", vsName)
                        .uniqueResultOptional()
        );
    }

    public List<VsCatalog> findAll() {
        return executeReadOnly(session ->
                session.createQuery("FROM VsCatalog", VsCatalog.class).list()
        );
    }

    public VsCatalog save(VsCatalog entity) {
        return executeInSession(session -> (VsCatalog) session.merge(entity));
    }

    public Optional<VsCatalog> findById(Long id) {
        return executeReadOnly(session ->
                Optional.ofNullable(session.get(VsCatalog.class, id))
        );
    }
}
