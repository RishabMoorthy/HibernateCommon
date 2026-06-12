package org.common.db.repository;

import org.common.db.entity.VsLiveUrl;

import java.util.Optional;

public class VsLiveUrlRepository extends AbstractRepository {

    public Optional<VsLiveUrl> findActiveByVsId(Long vsId) {
        return executeInSession(session ->
                session.createQuery(
                        "FROM VsLiveUrl v WHERE v.vsid = :vsId AND v.isActive = 'Y'",
                        VsLiveUrl.class)
                        .setParameter("vsId", vsId)
                        .uniqueResultOptional()
        );
    }
}
