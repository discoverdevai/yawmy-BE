package com.discoverdev.nms.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserMuteRepository extends JpaRepository<UserMute, UUID> {

    Optional<UserMute> findByMuterIdAndMutedId(UUID muterId, UUID mutedId);

    boolean existsByMuterIdAndMutedId(UUID muterId, UUID mutedId);

    List<UserMute> findByMuterIdOrderByCreatedAtDesc(UUID muterId);

    /**
     * Of these recipients, who has muted the sender. Asked once when fanning out a
     * message instead of once per recipient — a room group can have thirty.
     */
    @Query("""
        select m.muter.id from UserMute m
        where m.muted.id = :senderId and m.muter.id in :recipientIds
        """)
    List<UUID> findMutersOf(
        @Param("senderId") UUID senderId,
        @Param("recipientIds") java.util.Collection<UUID> recipientIds);
}
