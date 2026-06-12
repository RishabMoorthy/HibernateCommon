package org.common.db.repository;

import org.common.db.entity.RefreshToken;
import org.hibernate.SessionFactory;

import java.util.Optional;

public class RefreshTokenRepository extends AbstractRepository {

    public RefreshTokenRepository(SessionFactory sessionFactory) { super(sessionFactory); }

    public Optional<RefreshToken> findByJti(String jti) {
        return executeReadOnly(session ->
                Optional.ofNullable(session.get(RefreshToken.class, jti))
        );
    }

    public void deleteAllByUsername(String username) {
        executeInSession(session -> {
            session.createMutationQuery(
                            "DELETE FROM RefreshToken r WHERE r.username = :u")
                    .setParameter("u", username)
                    .executeUpdate();
            return null;
        });
    }

    public RefreshToken save(RefreshToken entity) {
        return executeInSession(session -> (RefreshToken) session.merge(entity));
    }

    public void deleteById(String jti) {
        executeInSession(session -> {
            RefreshToken rt = session.get(RefreshToken.class, jti);
            if (rt != null) session.remove(rt);
            return null;
        });
    }
}
