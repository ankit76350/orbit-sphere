package com.orbitastra.backend.models.common.enums;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The IETF language tags a school may be set to.
 *
 * <p><b>CURATED, NOT GENERATED — and that is the difference from the other two enums.</b>
 * {@link SchoolTimeZone} and {@link CountryCode} are exhaustive because a school really can be in
 * any zone or any country, and refusing a real one would be a bug. A locale is different: it is a
 * promise that the product has strings in that language. Listing all 800-odd tags the JDK knows
 * would advertise Icelandic support this product does not have.
 *
 * <p>So the set is a product decision: <b>India's 22 scheduled languages plus Sanskrit</b>, and
 * <b>English for the countries a multi-country tenant plausibly sits in</b>. Every tag was checked
 * against {@code Locale.forLanguageTag} before being written down, so none of them is a
 * misremembered code.
 *
 * <p><b>Adding one is a one-line change</b>, and it should be made when the translations exist
 * rather than in advance. That is the whole reason this list is short.
 *
 * <p><b>THE WIRE VALUE IS THE TAG, NOT THE CONSTANT NAME.</b> {@code EN_IN} is how Java has to
 * spell it — a constant cannot contain a hyphen — and every request, response and stored document
 * says {@code "en-IN"}. Same mechanism, and the same reason, as {@link SchoolTimeZone}: existing
 * documents hold the hyphenated form. See {@code config/EnumCodeConfig} for the Mongo half.
 */
public enum SchoolLocale {

    /** English (India) */
    EN_IN("en-IN"),
    /** Hindi (India) */
    HI_IN("hi-IN"),
    /** Bangla (India) */
    BN_IN("bn-IN"),
    /** Marathi (India) */
    MR_IN("mr-IN"),
    /** Telugu (India) */
    TE_IN("te-IN"),
    /** Tamil (India) */
    TA_IN("ta-IN"),
    /** Gujarati (India) */
    GU_IN("gu-IN"),
    /** Urdu (India) */
    UR_IN("ur-IN"),
    /** Kannada (India) */
    KN_IN("kn-IN"),
    /** Odia (India) */
    OR_IN("or-IN"),
    /** Malayalam (India) */
    ML_IN("ml-IN"),
    /** Punjabi (India) */
    PA_IN("pa-IN"),
    /** Assamese (India) */
    AS_IN("as-IN"),
    /** Maithili (India) */
    MAI_IN("mai-IN"),
    /** Santali (India) */
    SAT_IN("sat-IN"),
    /** Kashmiri (India) */
    KS_IN("ks-IN"),
    /** Nepali (India) */
    NE_IN("ne-IN"),
    /** Sindhi (India) */
    SD_IN("sd-IN"),
    /** Dogri (India) */
    DOI_IN("doi-IN"),
    /** Konkani (India) */
    KOK_IN("kok-IN"),
    /** Manipuri (India) */
    MNI_IN("mni-IN"),
    /** Bodo (India) */
    BRX_IN("brx-IN"),
    /** Sanskrit (India) */
    SA_IN("sa-IN"),
    /** English (United States) */
    EN_US("en-US"),
    /** English (United Kingdom) */
    EN_GB("en-GB"),
    /** English (Australia) */
    EN_AU("en-AU"),
    /** English (Canada) */
    EN_CA("en-CA"),
    /** English (Singapore) */
    EN_SG("en-SG"),
    /** English (United Arab Emirates) */
    EN_AE("en-AE"),
    /** English (South Africa) */
    EN_ZA("en-ZA");

    /** How many locales this enum covers. */
    public static final int TOTAL = 30;

    private static final Map<String, SchoolLocale> BY_TAG = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(one -> one.tag.toLowerCase(Locale.ENGLISH),
                    Function.identity()));

    private final String tag;

    SchoolLocale(String tag) {
        this.tag = tag;
    }

    /** The IETF tag, which is what goes on the wire and into the database. */
    @JsonValue
    public String getTag() {
        return tag;
    }

    /**
     * The locale with this tag.
     *
     * <p>Case-insensitive, because an IETF tag's casing is a convention rather than part of its
     * identity — {@code en-in} and {@code en-IN} are the same tag, and RFC 5646 says so. The
     * canonical form is what gets stored.
     *
     * @throws IllegalArgumentException naming the offending value and listing what is supported —
     *                                 short enough to list, unlike the other two enums
     */
    @JsonCreator
    public static SchoolLocale fromTag(String tag) {
        SchoolLocale found = tag == null ? null
                : BY_TAG.get(tag.trim().toLowerCase(Locale.ENGLISH));
        if (found == null) {
            throw new IllegalArgumentException(
                    "'" + tag + "' is not a locale this product supports. Supported: "
                            + Stream.of(values()).map(SchoolLocale::getTag)
                                    .collect(Collectors.joining(", "))
                            + ".");
        }
        return found;
    }

    /** The JDK locale, for formatting. */
    public Locale toLocale() {
        return Locale.forLanguageTag(tag);
    }

    @Override
    public String toString() {
        return tag;
    }
}
