package com.orbitastra.backend.services.crm.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;

import lombok.RequiredArgsConstructor;

/**
 * The read {@link com.orbitastra.backend.services.crm.AdmissionCycleService} makes more than once.
 *
 * <p>Per the service folder rules: a main service has its own {@code utils}, and <b>a method here
 * never calls another method here</b>. Only the service calls these.
 *
 * <p><b>One method, and that is the file being honest rather than thin.</b> Two things in that
 * service look extractable and are not: {@code datesRunForwards} and {@code nextStepFor} each have
 * a single caller, so they stay inline under their {@code //! step N} where they can be read in the
 * order they happen. Moving them would buy a longer import list and a jump to nowhere.
 */
@Component
@RequiredArgsConstructor
public class AdmissionCycleServiceUtils {

    private final SchoolClassRepository schoolClasses;

    /**
     * The names of the classes a seat table points at, keyed by id.
     *
     * <p><b>ONE QUERY FOR EVERY ROW, not one per row.</b> A cycle can hold twenty classes, and
     * reading them one at a time is the N+1 this project keeps naming.
     *
     * <p><b>Nothing to look up is not a query.</b> Every cycle is created with an empty seat table,
     * so an empty list is the common case rather than an edge one.
     *
     * <p><b>The year is the CYCLE'S, not the school's current one.</b> A cycle admits into one year
     * and seats against another year's class are seats nobody could fill — which is why the year is
     * a parameter and not something this reads for itself.
     *
     * <p><b>A class that is gone is simply absent from the map.</b> That is what lets #6 render a
     * seat row with no name, and what lets #4 tell the caller which of the ids it sent do not
     * exist: the keys are the ones that were found.
     *
     * <p>A merge function is needed even though ids are unique: {@code toMap} throws on a duplicate
     * key rather than keeping either.
     *
     * Used by:
     * - getCycle()
     * - setCapacities()
     */
    public Map<String, String> classNamesFor(School school, String academicYear,
            Collection<String> classDocsIds) {

        if (classDocsIds == null || classDocsIds.isEmpty()) {
            return Map.of();
        }

        // TODO: read school classes
        List<SchoolClass> found = schoolClasses.findBySchoolIdAndAcademicYearAndIdIn(
                school.getId(), academicYear, List.copyOf(classDocsIds));

        return found.stream().collect(Collectors.toMap(
                SchoolClass::getId, SchoolClass::getName, (first, second) -> first));
    }
}
