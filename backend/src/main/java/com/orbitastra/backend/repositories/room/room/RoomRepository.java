package com.orbitastra.backend.repositories.room.room;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.room.Room;

/**
 * The school's rooms, halls, labs and other places — a paper's exam room is one of them.
 *
 * <p>Every query carries {@code schoolId}, so another school's room is never found.
 */
public interface RoomRepository extends MongoRepository<Room, String>, RoomRepositoryCustom {

    /** One room of this school, by its id. */
    Optional<Room> findByIdAndSchoolId(String id, String schoolId);

    /**
     * A room of this school with the same name, ignoring upper and lower case — so "Room 12" and
     * "room 12" count as the same name. Closed rooms are included: they keep their name.
     */
    Optional<Room> findFirstBySchoolIdAndNameIgnoreCase(String schoolId, String name);

    /** Several rooms of this school at once, by id. */
    List<Room> findBySchoolIdAndIdIn(String schoolId, Collection<String> ids);
}
