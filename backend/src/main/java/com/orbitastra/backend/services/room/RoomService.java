package com.orbitastra.backend.services.room;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.room.room.request.RoomCreateRequest;
import com.orbitastra.backend.dto.room.room.request.RoomSearchRequest;
import com.orbitastra.backend.dto.room.room.response.RoomResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.room.Room;
import com.orbitastra.backend.models.room.enums.RoomStatus;
import com.orbitastra.backend.repositories.room.room.RoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The school's rooms — classrooms, labs, the hall. Endpoints #1–#3 of
 * {@code controllers/room/README.md}: add a room, list them, read one.
 *
 * <p>A room belongs to the school, not to a year, so there is no year in any path and no gate 4.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final CurrentSchoolResolver currentSchool;
    private final RoomRepository rooms;

    /**
     * What {@code ?sort=} accepts, keyed by the lower-cased name a caller types. Only these, so a
     * caller cannot sort on a field the database would have to read every row to order by.
     */
    private static final Map<String, String> SORTABLE_ROOM_FIELDS = new LinkedHashMap<>();

    static {
        SORTABLE_ROOM_FIELDS.put("name", "name");
        SORTABLE_ROOM_FIELDS.put("capacity", "capacity");
        SORTABLE_ROOM_FIELDS.put("createdat", "createdAt");
        SORTABLE_ROOM_FIELDS.put("updatedat", "updatedAt");
    }

    /** The same set as a sentence, for the refusal to list. */
    private static final String SORTABLE_ROOM_FIELD_NAMES =
            String.join(", ", SORTABLE_ROOM_FIELDS.values());

    /**
     * The default order: by name. The name is unique in the school, so no two rows tie and paging
     * never shows a room twice.
     */
    private static final Sort ROOM_ORDER = Sort.by(Sort.Order.asc("name"));

    /**
     * Endpoint #1 — <b>add a room</b>. It starts as {@code IN_USE}.
     *
     * <p>The name must be free in the school, ignoring case — closed rooms keep theirs. Gates 1
     * and 2 run in the controller.
     */
    public RoomResponse createRoom(RoomCreateRequest request) {

        //! Step 1 - Get the current school. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String name = request.name().trim();
        log.info("[createRoom] Step 1: Adding room '{}' to school {}", name, school.getId());

        //! Step 2 - The name must be free in the school, ignoring upper and lower case.
        // TODO: read room (same name in this school)
        rooms.findFirstBySchoolIdAndNameIgnoreCase(school.getId(), name).ifPresent(other -> {
            throw ApiException.conflict("ROOM_NAME_TAKEN",
                    "This school already has a room called '" + other.getName() + "'"
                            + (other.getStatus() == RoomStatus.CLOSED ? " (closed)" : "")
                            + ". Choose another name.");
        });

        //! Step 3 - Build the room. It always starts in use.
        Room room = Room.builder()
                .schoolId(school.getId())
                .name(name)
                .resourceType(request.resourceType())
                .location(TextHelper.blankToNull(request.location()))
                .capacity(request.capacity())
                .remarks(TextHelper.blankToNull(request.remarks()))
                .status(RoomStatus.IN_USE)
                .build();

        //! Step 4 - Save it.
        // TODO: insert room
        Room saved = rooms.save(room);
        log.info("[createRoom] Step 2: Saved room '{}' (id {})", saved.getName(), saved.getId());

        return RoomResponse.of(saved);
    }

    /**
     * Endpoint #2 — <b>the school's rooms</b>, filtered and paged.
     *
     * <p>Every filter is optional: {@code resourceType}, {@code status}, {@code search} (part of
     * the name or the location, ignoring case) and {@code minCapacity}. Ordered by name unless
     * {@code ?sort=} says otherwise. <b>No gates.</b> A read.
     */
    public PageResponse<RoomResponse> listRooms(RoomSearchRequest request) {

        //! Step 1 - Check the paging and the order before anything is read.
        Pageable pageable = PageResponse.pageableOf(request.page(), request.size(), request.sort(),
                SORTABLE_ROOM_FIELDS, SORTABLE_ROOM_FIELD_NAMES, ROOM_ORDER);

        //! Step 2 - Get the current school. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        log.info("[listRooms] Step 1: Listing the rooms of school {}", school.getId());

        //! Step 3 - One page of them.
        // TODO: search rooms
        Page<Room> page = rooms.search(school.getId(), request, pageable);
        log.info("[listRooms] Step 2: Found {} room(s) in all; returning {}",
                page.getTotalElements(), page.getNumberOfElements());

        return PageResponse.from(page, RoomResponse::of);
    }

    /**
     * Endpoint #3 — <b>one room</b>, by its id. Another school's room is a 404. <b>No gates.</b>
     */
    public RoomResponse getRoom(String roomDocsId) {

        //! Step 1 - Get the current school. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        String roomId = roomDocsId == null ? "" : roomDocsId.trim();
        log.info("[getRoom] Step 1: Reading room {} of school {}", roomId, school.getId());

        //! Step 2 - The room, scoped to this school.
        // TODO: read room
        Room room = rooms.findByIdAndSchoolId(roomId, school.getId())
                .orElseThrow(() -> ApiException.notFound("ROOM_NOT_FOUND",
                        "No room with id '" + roomId + "' in this school."));

        return RoomResponse.of(room);
    }
}
