package com.orbitastra.backend.repositories.room.room;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.room.Room;

/**
 * The school's rooms, halls, labs and other places — a paper's exam room is one of them.
 *
 * <p>Every query carries {@code schoolId}, so another school's room is never found.
 */
public interface RoomRepository extends MongoRepository<Room, String> {

    /** One room of this school, by its id. */
    Optional<Room> findByIdAndSchoolId(String id, String schoolId);
}
