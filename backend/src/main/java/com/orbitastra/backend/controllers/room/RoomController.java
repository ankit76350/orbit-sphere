package com.orbitastra.backend.controllers.room;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.access.ActionGate;
import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.room.room.request.RoomCreateRequest;
import com.orbitastra.backend.dto.room.room.request.RoomSearchRequest;
import com.orbitastra.backend.dto.room.room.response.RoomResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.room.RoomService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The school's rooms. Endpoints #1–#3 of {@code controllers/room/README.md}, all built.
 *
 * <p>A room belongs to the school, not to a year, so the paths carry no {@code {year}}.
 */
@RestController
@RequestMapping("/schools/current/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final CurrentSchoolResolver currentSchool;
    private final ActionGate gate;

    /**
     * Endpoint #1 — <b>add a room</b>. 201, and it starts as {@code IN_USE}.
     *
     * <pre>
     * 409 ROOM_NAME_TAKEN             the school has a room of that name, any case, closed too
     * 400 INVALID_VALUE               a resourceType that does not exist — lists the accepted ones
     * 400 VALIDATION_FAILED           no name or type, or capacity below 1
     * 409 SCHOOL_NOT_EDITABLE         the school is suspended or closed
     * 400 TENANT_NOT_RESOLVED         no idtoken cookie
     * </pre>
     */
    @PostMapping
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody RoomCreateRequest request) {

        //! Gate 1 — is the school itself live ---------------------------------------------
        //! Gate 2 — is the school paying --------------------------------------------------
        //! No gate 4: a room belongs to the school, so there is no year in this path.
        School school = currentSchool.require();
        gate.requireActiveSchool(school);
        gate.requireUsableSubscription(school);

        RoomResponse response = roomService.createRoom(request);

        return ResponseEntity
                .created(URI.create("/schools/current/rooms/" + response.roomDocsId()))
                .body(response);
    }

    /**
     * Endpoint #2 — <b>the school's rooms</b>, filtered and paged.
     *
     * <p>Filters, all optional: {@code resourceType}, {@code status}, {@code search},
     * {@code minCapacity}. Paging: {@code page}, {@code size}, {@code sort} (name, capacity,
     * createdAt, updatedAt, each with {@code ,desc}).
     *
     * <pre>
     * 400 VALIDATION_FAILED           a resourceType or status that does not exist
     * 400 INVALID_PAGE · INVALID_PAGE_SIZE · INVALID_SORT_FIELD
     * 400 TENANT_NOT_RESOLVED         no idtoken cookie
     * </pre>
     *
     * <p><b>No gates.</b> A read.
     */
    @GetMapping
    public ResponseEntity<PageResponse<RoomResponse>> list(RoomSearchRequest request) {
        return ResponseEntity.ok(roomService.listRooms(request));
    }

    /**
     * Endpoint #3 — <b>one room</b>, by its id.
     *
     * <pre>
     * 404 ROOM_NOT_FOUND              no room of that id in this school
     * 400 TENANT_NOT_RESOLVED         no idtoken cookie
     * </pre>
     *
     * <p><b>No gates.</b> A read.
     */
    @GetMapping("/{roomDocsId}")
    public ResponseEntity<RoomResponse> get(@PathVariable String roomDocsId) {
        return ResponseEntity.ok(roomService.getRoom(roomDocsId));
    }
}
