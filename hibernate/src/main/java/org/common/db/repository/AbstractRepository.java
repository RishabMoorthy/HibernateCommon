package org.common.db.repository;

import org.common.db.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.function.Function;

/**
 * - Base repository — single SessionFactory resolved from config at startup.
 * - No DB type arg needed; whichever DB is in database.url is used.
 * - Two constructors: no-arg for StubServerCore (uses HibernateUtil),
 *   SessionFactory arg for Spring Boot (uses injected factory).
 */
public abstract class AbstractRepository {

    private final SessionFactory sessionFactory;
    private final String dbTag;

    /** StubServerCore path — SessionFactory from HibernateUtil config */
    protected AbstractRepository() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
        this.dbTag = HibernateUtil.getDatabaseType().name();
    }

    /** Spring Boot path — SessionFactory injected by RepositoryConfig */
    protected AbstractRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.dbTag = "SPRING";
    }

    protected SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /** Transactional write — auto rollback on failure */
    protected <T> T executeInSession(Function<Session, T> action) {
        Transaction tx = null;

        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();

            T result = action.apply(session);

            tx.commit();
            return result;

        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception rollbackEx) {
                    System.err.println("[AbstractRepository] Rollback failed: "
                            + rollbackEx.getMessage());
                }
            }
            throw new RuntimeException("DB write failed [" + dbTag + "]", e);
        }
    }

    /** Read-only — no transaction overhead */
    protected <T> T executeReadOnly(Function<Session, T> work) {
        try (Session session = sessionFactory.openSession()) {
            session.setDefaultReadOnly(true);
            return work.apply(session);
        } catch (Exception e) {
            throw new RuntimeException("DB read failed [" + dbTag + "]", e);
        }
    }
}
