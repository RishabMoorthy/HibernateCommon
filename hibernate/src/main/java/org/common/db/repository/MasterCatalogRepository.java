package org.common.db.repository;

import org.common.db.entity.MasterCatalog;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class MasterCatalogRepository extends AbstractRepository {

    public MasterCatalogRepository() { super(); }

    public MasterCatalogRepository(SessionFactory sessionFactory) { super(sessionFactory); }

    // Equivalent of getMSID(connection, port)
    public Optional<MasterCatalog> findAnyByPort(int port) {
        return executeReadOnly(session ->
                session.createQuery(
                        "FROM MasterCatalog v WHERE v.port = :port",
                        MasterCatalog.class)
                        .setParameter("port", (long) port)
                        .setMaxResults(1)
                        .uniqueResultOptional()
        );
    }

    // Equivalent of getMSIDbyServiceName(connection, port, serviceName)
    public Optional<MasterCatalog> findByPortAndServiceName(int port, String serviceName) {
        return executeReadOnly(session ->
                session.createQuery(
                        "FROM MasterCatalog v WHERE v.port = :port AND v.vsName = :name",
                        MasterCatalog.class)
                        .setParameter("port", (long) port)
                        .setParameter("name", serviceName)
                        .uniqueResultOptional()
        );
    }

    // Equivalent of getMSIDbyStatus(connection, port, "ACTIVE")
    public Optional<MasterCatalog> findByPortAndStatus(int port, String status) {
        return executeReadOnly(session ->
                session.createQuery(
                        "FROM MasterCatalog v WHERE v.port = :port AND v.status = :status",
                        MasterCatalog.class)
                        .setParameter("port", (long) port)
                        .setParameter("status", status)
                        .setMaxResults(1)
                        .uniqueResultOptional()
        );
    }

    // Optional: get ALL by port (useful for debugging / UI)
    public List<MasterCatalog> findAllByPort(int port) {
        return executeReadOnly(session ->
                session.createQuery(
                        "FROM MasterCatalog v WHERE v.port = :port",
                        MasterCatalog.class)
                        .setParameter("port", (long) port)
                        .getResultList()
        );
    }

    // Generic DB operations
    public void save(MasterCatalog entity) {
        executeInSession(session -> {
            session.persist(entity);
            return null;
        });
    }

    public Long findMaxMasterId() {
        Long maxId = executeReadOnly(session ->
                session.createQuery(
                        "SELECT MAX(c.masterId) FROM MasterCatalog c",
                        Long.class)
                        .uniqueResult()
        );
        return maxId != null ? maxId : 0L;
    }

    public List<MasterCatalog> findByServiceName(String vsName) {
        return executeInSession(session ->
                session.createQuery(
                        "FROM MasterCatalog v WHERE LOWER(v.vsName) = LOWER(:name)",
                        MasterCatalog.class)
                        .setParameter("name", vsName)
                        .getResultList()
        );
    }

    public MasterCatalog update(MasterCatalog entity) {
        return executeInSession(session -> (MasterCatalog) session.merge(entity));
    }

    public Optional<Long> findByPortAndName(int port, String vsName) {
        return executeReadOnly(session ->
                session.createQuery(
                        "SELECT m.masterId FROM MasterCatalog m WHERE m.port = :p AND m.vsName = :n",
                        Long.class)
                        .setParameter("p", (long) port)
                        .setParameter("n", vsName)
                        .setMaxResults(1)
                        .uniqueResultOptional()
        );
    }

    public void delete(MasterCatalog entity) {
        executeInSession(session -> {
            session.remove(entity);
            return null;
        });
    }

    public boolean existsByVsName(String vsName) {
        Long count = executeReadOnly(session ->
                session.createQuery(
                        "SELECT COUNT(c) FROM MasterCatalog c WHERE c.vsName = :n",
                        Long.class)
                        .setParameter("n", vsName)
                        .uniqueResult()
        );
        return count != null && count > 0;
    }

    public boolean existsByPort(int port) {
        Long count = executeReadOnly(session ->
                session.createQuery(
                        "SELECT COUNT(c) FROM MasterCatalog c WHERE c.port = :p",
                        Long.class)
                        .setParameter("p", (long) port)
                        .uniqueResult()
        );
        return count != null && count > 0;
    }
}
