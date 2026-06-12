package org.common.db.repository;

import org.common.db.entity.VsLiveUrl;
import org.hibernate.SessionFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LiveUrlRepository extends AbstractRepository {

    public LiveUrlRepository(SessionFactory sessionFactory) { super(sessionFactory); }

    public List<VsLiveUrl> findByVsid(Long vsid) {
        return executeReadOnly(session ->
                session.createQuery(
                        "FROM VsLiveUrl v WHERE v.vsid = :id",
                        VsLiveUrl.class)
                        .setParameter("id", vsid)
                        .getResultList()
        );
    }

    public boolean existsByVsidAndHost(Long vsid, String host) {
        Long count = executeReadOnly(session ->
                session.createQuery(
                        "SELECT COUNT(v) FROM VsLiveUrl v WHERE v.vsid = :id AND v.host = :h",
                        Long.class)
                        .setParameter("id", vsid)
                        .setParameter("h", host)
                        .uniqueResult()
        );
        return count != null && count > 0;
    }

    public Optional<VsLiveUrl> findTopByVsUrlIdDesc() {
        return executeReadOnly(session ->
                session.createQuery(
                        "FROM VsLiveUrl ORDER BY vsUrlId DESC",
                        VsLiveUrl.class)
                        .setMaxResults(1)
                        .uniqueResultOptional()
        );
    }

    public Optional<VsLiveUrl> findById(Long id) {
        return executeReadOnly(session ->
                Optional.ofNullable(session.get(VsLiveUrl.class, id))
        );
    }

    public VsLiveUrl save(VsLiveUrl entity) {
        return executeInSession(session -> (VsLiveUrl) session.merge(entity));
    }

    public List<VsLiveUrl> saveAll(List<VsLiveUrl> entities) {
        return executeInSession(session -> {
            List<VsLiveUrl> saved = new ArrayList<>();
            for (VsLiveUrl e : entities) {
                saved.add((VsLiveUrl) session.merge(e));
            }
            return saved;
        });
    }

    public void deleteById(Long id) {
        executeInSession(session -> {
            VsLiveUrl e = session.get(VsLiveUrl.class, id);
            if (e != null) session.remove(e);
            return null;
        });
    }
}
