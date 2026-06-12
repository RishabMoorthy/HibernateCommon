package org.common.db.repository;

import org.common.db.entity.AssignedService;
import org.hibernate.SessionFactory;

import java.util.ArrayList;
import java.util.List;

public class AssignedServiceRepository extends AbstractRepository {

    public AssignedServiceRepository(SessionFactory sessionFactory) { super(sessionFactory); }

    public List<AssignedService> findByIdUsername(String username) {
        return executeReadOnly(session ->
                session.createQuery(
                        "FROM AssignedService a WHERE a.id.username = :u",
                        AssignedService.class)
                        .setParameter("u", username)
                        .getResultList()
        );
    }

    public void deleteByIdUsername(String username) {
        executeInSession(session -> {
            session.createMutationQuery(
                            "DELETE FROM AssignedService a WHERE a.id.username = :u")
                    .setParameter("u", username)
                    .executeUpdate();
            return null;
        });
    }

    public AssignedService save(AssignedService entity) {
        return executeInSession(session -> (AssignedService) session.merge(entity));
    }

    public List<AssignedService> saveAll(List<AssignedService> entities) {
        return executeInSession(session -> {
            List<AssignedService> saved = new ArrayList<>();
            for (AssignedService e : entities) {
                saved.add((AssignedService) session.merge(e));
            }
            return saved;
        });
    }
}
