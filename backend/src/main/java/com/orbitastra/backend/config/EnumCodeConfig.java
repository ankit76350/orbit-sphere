package com.orbitastra.backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import com.orbitastra.backend.models.common.enums.SchoolLocale;
import com.orbitastra.backend.models.common.enums.SchoolTimeZone;

/**
 * Teaches Mongo to store the enums whose wire value is not their constant name.
 *
 * <p><b>Why this has to exist.</b> Spring Data writes an enum as {@code name()}. For
 * {@link SchoolTimeZone} that would store {@code "ASIA_KOLKATA"} where every one of the
 * thousands of existing documents holds {@code "Asia/Kolkata"} — so the moment the field became
 * an enum, every school in the database would have failed to read, and every school written
 * afterwards would have been unreadable by anything expecting the IANA form.
 *
 * <p><b>The rule these converters keep: the stored value is byte-identical to what was stored
 * before the field was an enum.</b> No migration, no dual-read, nothing to undo. The enum
 * constrains what can get <i>in</i>; it does not change what is on disk.
 *
 * <p>{@code @JsonValue} does the same job for HTTP and is declared on the enum itself. The two
 * are separate mechanisms for separate boundaries, and both are needed — Jackson does not know
 * about Mongo and Mongo does not read Jackson's annotations.
 *
 * <p><b>The reading converter is deliberately strict.</b> An unrecognised string throws rather
 * than resolving to null, because a document holding a zone this build does not know about is a
 * fact somebody needs to see. Silently nulling it would move that school's calendar to UTC and
 * look like it had always been that way.
 */
@Configuration
public class EnumCodeConfig {

    @Bean
    MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                new SchoolTimeZoneWriter(),
                new SchoolTimeZoneReader(),
                new SchoolLocaleWriter(),
                new SchoolLocaleReader()));
    }

    /** Enum -> the IANA id, which is what the collection already holds. */
    @WritingConverter
    static class SchoolTimeZoneWriter implements Converter<SchoolTimeZone, String> {
        @Override
        public String convert(SchoolTimeZone source) {
            return source.getId();
        }
    }

    /** The IANA id -> enum. Throws on anything unknown; see the note on the class. */
    @ReadingConverter
    static class SchoolTimeZoneReader implements Converter<String, SchoolTimeZone> {
        @Override
        public SchoolTimeZone convert(String source) {
            return SchoolTimeZone.fromId(source);
        }
    }

    /**
     * Enum -> the IETF tag, for the same reason as the zone: {@code EN_IN} is the constant and
     * {@code "en-IN"} is what every document holds.
     *
     * <p><b>{@code CountryCode} needs no pair of these</b>, and that is worth saying out loud so
     * nobody adds one for symmetry: "IN" is a legal constant name, so Spring Data's default
     * {@code name()} handling already writes and reads exactly what is stored.
     */
    @WritingConverter
    static class SchoolLocaleWriter implements Converter<SchoolLocale, String> {
        @Override
        public String convert(SchoolLocale source) {
            return source.getTag();
        }
    }

    /** The IETF tag -> enum. Strict, like the zone's. */
    @ReadingConverter
    static class SchoolLocaleReader implements Converter<String, SchoolLocale> {
        @Override
        public SchoolLocale convert(String source) {
            return SchoolLocale.fromTag(source);
        }
    }
}
