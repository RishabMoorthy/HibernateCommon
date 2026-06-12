package org.common.db.repository;

import org.common.db.entity.User;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class UserRepository extends AbstractRepository {

    public UserRepository(SessionFactory sessionFactory) { super(sessionFactory); }

    public Optional<User> findByUsername(String username) {
        return executeReadOnly(session ->
                Optional.ofNullable(session.get(User.class, username))
        );
    }

    public boolean existsByUsernameOrEmail(String username, String email) {
        Long count = executeReadOnly(session ->
                session.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.username = :u OR u.email = :e",
                        Long.class)
                        .setParameter("u", username)
                        .setParameter("e", email)
                        .uniqueResult()
        );
        return count != null && count > 0;
    }

    public boolean existsById(String username) {
        return executeReadOnly(session ->
                session.get(User.class, username) != null
        );
    }

    public List<User> findAll() {
        return executeReadOnly(session ->
                session.createQuery("FROM User", User.class).list()
        );
    }

    public User save(User entity) {
        return executeInSession(session -> (User) session.merge(entity));
    }

    public void deleteById(String username) {
        executeInSession(session -> {
            User u = session.get(User.class, username);
            if (u != null) session.remove(u);
            return null;
        });
    }
}
