package com.orbitastra.backend.repositories.room.room;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.room.room.request.RoomSearchRequest;
import com.orbitastra.backend.models.room.Room;

/** The room reads that need a query built by hand. */
public interface RoomRepositoryCustom {

    /** One page of the school's rooms, with whichever filters were sent. */
    Page<Room> search(String schoolId, RoomSearchRequest request, Pageable pageable);
}
