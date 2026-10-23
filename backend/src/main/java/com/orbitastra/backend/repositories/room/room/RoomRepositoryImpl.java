package com.orbitastra.backend.repositories.room.room;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.dto.room.room.request.RoomSearchRequest;
import com.orbitastra.backend.models.room.Room;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RoomRepositoryImpl implements RoomRepositoryCustom {

    private final MongoTemplate mongo;

    /**
     * One page of the school's rooms. The school always applies; the type, status, search text and
     * smallest capacity only when sent.
     */
    @Override
    public Page<Room> search(String schoolId, RoomSearchRequest request, Pageable pageable) {

        //! step 1 - the school, then whichever filters were sent
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));

        if (request.resourceType() != null) {
            filters.add(Criteria.where("resourceType").is(request.resourceType()));
        }
        if (request.status() != null) {
            filters.add(Criteria.where("status").is(request.status()));
        }
        if (request.minCapacity() != null) {
            filters.add(Criteria.where("capacity").gte(request.minCapacity()));
        }
        //! The search text is quoted, so "(" is looked for as a bracket rather than breaking the query.
        if (request.search() != null && !request.search().isBlank()) {
            String needle = Pattern.quote(request.search().trim());
            filters.add(new Criteria().orOperator(
                    Criteria.where("name").regex(needle, "i"),
                    Criteria.where("location").regex(needle, "i")));
        }
        Criteria criteria = new Criteria().andOperator(filters.toArray(new Criteria[0]));

        //! step 2 - how many match, for totalElements
        // TODO: count rooms
        long total = mongo.count(new Query(criteria), Room.class);

        //! step 3 - one page of them, in the order asked for
        // TODO: search rooms
        List<Room> rows = mongo.find(new Query(criteria).with(pageable), Room.class);

        return new PageImpl<>(rows, pageable, total);
    }
}
