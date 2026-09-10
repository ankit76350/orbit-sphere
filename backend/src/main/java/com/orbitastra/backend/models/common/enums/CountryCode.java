package com.orbitastra.backend.models.common.enums;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Every ISO 3166-1 alpha-2 country code, as a closed set.
 *
 * <p><b>The constant name IS the code</b>, which is the one thing that makes this enum simpler
 * than {@link SchoolTimeZone}: "IN" is a legal Java identifier, so {@code name()} already matches
 * what the wire and the database hold. No {@code @JsonValue} and no Mongo converter are needed —
 * Spring Data and Jackson both write {@code name()} by default, and that is already right.
 *
 * <p><b>{@link #fromCode} exists anyway</b>, for the message. Jackson's own refusal for an unknown
 * enum lists every accepted value, and with 249 of them that is a wall of text nobody reads. One
 * sentence naming the bad value and pointing at the standard is more use.
 *
 * <p><b>Generated from {@code Locale.getISOCountries()}</b>, so the list is the JDK's and the
 * javadoc names come from {@code getDisplayCountry}. Nothing here was typed by hand, which is the
 * only way a 249-constant enum stays free of typos.
 *
 * <p>The names are the JDK's English display names at the time of generation. They are
 * documentation, not data — nothing reads them, and a country renaming itself does not change its
 * code.
 */
public enum CountryCode {

    /** Andorra. */
    AD,
    /** United Arab Emirates. */
    AE,
    /** Afghanistan. */
    AF,
    /** Antigua & Barbuda. */
    AG,
    /** Anguilla. */
    AI,
    /** Albania. */
    AL,
    /** Armenia. */
    AM,
    /** Angola. */
    AO,
    /** Antarctica. */
    AQ,
    /** Argentina. */
    AR,
    /** American Samoa. */
    AS,
    /** Austria. */
    AT,
    /** Australia. */
    AU,
    /** Aruba. */
    AW,
    /** Åland Islands. */
    AX,
    /** Azerbaijan. */
    AZ,
    /** Bosnia & Herzegovina. */
    BA,
    /** Barbados. */
    BB,
    /** Bangladesh. */
    BD,
    /** Belgium. */
    BE,
    /** Burkina Faso. */
    BF,
    /** Bulgaria. */
    BG,
    /** Bahrain. */
    BH,
    /** Burundi. */
    BI,
    /** Benin. */
    BJ,
    /** St. Barthélemy. */
    BL,
    /** Bermuda. */
    BM,
    /** Brunei. */
    BN,
    /** Bolivia. */
    BO,
    /** Caribbean Netherlands. */
    BQ,
    /** Brazil. */
    BR,
    /** Bahamas. */
    BS,
    /** Bhutan. */
    BT,
    /** Bouvet Island. */
    BV,
    /** Botswana. */
    BW,
    /** Belarus. */
    BY,
    /** Belize. */
    BZ,
    /** Canada. */
    CA,
    /** Cocos (Keeling) Islands. */
    CC,
    /** Congo - Kinshasa. */
    CD,
    /** Central African Republic. */
    CF,
    /** Congo - Brazzaville. */
    CG,
    /** Switzerland. */
    CH,
    /** Côte d’Ivoire. */
    CI,
    /** Cook Islands. */
    CK,
    /** Chile. */
    CL,
    /** Cameroon. */
    CM,
    /** China. */
    CN,
    /** Colombia. */
    CO,
    /** Costa Rica. */
    CR,
    /** Cuba. */
    CU,
    /** Cape Verde. */
    CV,
    /** Curaçao. */
    CW,
    /** Christmas Island. */
    CX,
    /** Cyprus. */
    CY,
    /** Czechia. */
    CZ,
    /** Germany. */
    DE,
    /** Djibouti. */
    DJ,
    /** Denmark. */
    DK,
    /** Dominica. */
    DM,
    /** Dominican Republic. */
    DO,
    /** Algeria. */
    DZ,
    /** Ecuador. */
    EC,
    /** Estonia. */
    EE,
    /** Egypt. */
    EG,
    /** Western Sahara. */
    EH,
    /** Eritrea. */
    ER,
    /** Spain. */
    ES,
    /** Ethiopia. */
    ET,
    /** Finland. */
    FI,
    /** Fiji. */
    FJ,
    /** Falkland Islands. */
    FK,
    /** Micronesia. */
    FM,
    /** Faroe Islands. */
    FO,
    /** France. */
    FR,
    /** Gabon. */
    GA,
    /** United Kingdom. */
    GB,
    /** Grenada. */
    GD,
    /** Georgia. */
    GE,
    /** French Guiana. */
    GF,
    /** Guernsey. */
    GG,
    /** Ghana. */
    GH,
    /** Gibraltar. */
    GI,
    /** Greenland. */
    GL,
    /** Gambia. */
    GM,
    /** Guinea. */
    GN,
    /** Guadeloupe. */
    GP,
    /** Equatorial Guinea. */
    GQ,
    /** Greece. */
    GR,
    /** South Georgia & South Sandwich Islands. */
    GS,
    /** Guatemala. */
    GT,
    /** Guam. */
    GU,
    /** Guinea-Bissau. */
    GW,
    /** Guyana. */
    GY,
    /** Hong Kong SAR China. */
    HK,
    /** Heard & McDonald Islands. */
    HM,
    /** Honduras. */
    HN,
    /** Croatia. */
    HR,
    /** Haiti. */
    HT,
    /** Hungary. */
    HU,
    /** Indonesia. */
    ID,
    /** Ireland. */
    IE,
    /** Israel. */
    IL,
    /** Isle of Man. */
    IM,
    /** India. */
    IN,
    /** British Indian Ocean Territory. */
    IO,
    /** Iraq. */
    IQ,
    /** Iran. */
    IR,
    /** Iceland. */
    IS,
    /** Italy. */
    IT,
    /** Jersey. */
    JE,
    /** Jamaica. */
    JM,
    /** Jordan. */
    JO,
    /** Japan. */
    JP,
    /** Kenya. */
    KE,
    /** Kyrgyzstan. */
    KG,
    /** Cambodia. */
    KH,
    /** Kiribati. */
    KI,
    /** Comoros. */
    KM,
    /** St. Kitts & Nevis. */
    KN,
    /** North Korea. */
    KP,
    /** South Korea. */
    KR,
    /** Kuwait. */
    KW,
    /** Cayman Islands. */
    KY,
    /** Kazakhstan. */
    KZ,
    /** Laos. */
    LA,
    /** Lebanon. */
    LB,
    /** St. Lucia. */
    LC,
    /** Liechtenstein. */
    LI,
    /** Sri Lanka. */
    LK,
    /** Liberia. */
    LR,
    /** Lesotho. */
    LS,
    /** Lithuania. */
    LT,
    /** Luxembourg. */
    LU,
    /** Latvia. */
    LV,
    /** Libya. */
    LY,
    /** Morocco. */
    MA,
    /** Monaco. */
    MC,
    /** Moldova. */
    MD,
    /** Montenegro. */
    ME,
    /** St. Martin. */
    MF,
    /** Madagascar. */
    MG,
    /** Marshall Islands. */
    MH,
    /** North Macedonia. */
    MK,
    /** Mali. */
    ML,
    /** Myanmar (Burma). */
    MM,
    /** Mongolia. */
    MN,
    /** Macao SAR China. */
    MO,
    /** Northern Mariana Islands. */
    MP,
    /** Martinique. */
    MQ,
    /** Mauritania. */
    MR,
    /** Montserrat. */
    MS,
    /** Malta. */
    MT,
    /** Mauritius. */
    MU,
    /** Maldives. */
    MV,
    /** Malawi. */
    MW,
    /** Mexico. */
    MX,
    /** Malaysia. */
    MY,
    /** Mozambique. */
    MZ,
    /** Namibia. */
    NA,
    /** New Caledonia. */
    NC,
    /** Niger. */
    NE,
    /** Norfolk Island. */
    NF,
    /** Nigeria. */
    NG,
    /** Nicaragua. */
    NI,
    /** Netherlands. */
    NL,
    /** Norway. */
    NO,
    /** Nepal. */
    NP,
    /** Nauru. */
    NR,
    /** Niue. */
    NU,
    /** New Zealand. */
    NZ,
    /** Oman. */
    OM,
    /** Panama. */
    PA,
    /** Peru. */
    PE,
    /** French Polynesia. */
    PF,
    /** Papua New Guinea. */
    PG,
    /** Philippines. */
    PH,
    /** Pakistan. */
    PK,
    /** Poland. */
    PL,
    /** St. Pierre & Miquelon. */
    PM,
    /** Pitcairn Islands. */
    PN,
    /** Puerto Rico. */
    PR,
    /** Palestinian Territories. */
    PS,
    /** Portugal. */
    PT,
    /** Palau. */
    PW,
    /** Paraguay. */
    PY,
    /** Qatar. */
    QA,
    /** Réunion. */
    RE,
    /** Romania. */
    RO,
    /** Serbia. */
    RS,
    /** Russia. */
    RU,
    /** Rwanda. */
    RW,
    /** Saudi Arabia. */
    SA,
    /** Solomon Islands. */
    SB,
    /** Seychelles. */
    SC,
    /** Sudan. */
    SD,
    /** Sweden. */
    SE,
    /** Singapore. */
    SG,
    /** St. Helena. */
    SH,
    /** Slovenia. */
    SI,
    /** Svalbard & Jan Mayen. */
    SJ,
    /** Slovakia. */
    SK,
    /** Sierra Leone. */
    SL,
    /** San Marino. */
    SM,
    /** Senegal. */
    SN,
    /** Somalia. */
    SO,
    /** Suriname. */
    SR,
    /** South Sudan. */
    SS,
    /** São Tomé & Príncipe. */
    ST,
    /** El Salvador. */
    SV,
    /** Sint Maarten. */
    SX,
    /** Syria. */
    SY,
    /** Eswatini. */
    SZ,
    /** Turks & Caicos Islands. */
    TC,
    /** Chad. */
    TD,
    /** French Southern Territories. */
    TF,
    /** Togo. */
    TG,
    /** Thailand. */
    TH,
    /** Tajikistan. */
    TJ,
    /** Tokelau. */
    TK,
    /** Timor-Leste. */
    TL,
    /** Turkmenistan. */
    TM,
    /** Tunisia. */
    TN,
    /** Tonga. */
    TO,
    /** Turkey. */
    TR,
    /** Trinidad & Tobago. */
    TT,
    /** Tuvalu. */
    TV,
    /** Taiwan. */
    TW,
    /** Tanzania. */
    TZ,
    /** Ukraine. */
    UA,
    /** Uganda. */
    UG,
    /** U.S. Outlying Islands. */
    UM,
    /** United States. */
    US,
    /** Uruguay. */
    UY,
    /** Uzbekistan. */
    UZ,
    /** Vatican City. */
    VA,
    /** St. Vincent & Grenadines. */
    VC,
    /** Venezuela. */
    VE,
    /** British Virgin Islands. */
    VG,
    /** U.S. Virgin Islands. */
    VI,
    /** Vietnam. */
    VN,
    /** Vanuatu. */
    VU,
    /** Wallis & Futuna. */
    WF,
    /** Samoa. */
    WS,
    /** Yemen. */
    YE,
    /** Mayotte. */
    YT,
    /** South Africa. */
    ZA,
    /** Zambia. */
    ZM,
    /** Zimbabwe. */
    ZW;

    /** How many codes this enum covers. */
    public static final int TOTAL = 249;

    /**
     * The country with this code.
     *
     * <p>Case-insensitive and trimmed, unlike {@link SchoolTimeZone#fromId}: an alpha-2 code has
     * one canonical spelling and it is upper case, so "in" is an unambiguous way of writing "IN"
     * rather than a different value. A zone id has no such property, which is why that one is
     * strict and this one is not.
     *
     * @throws IllegalArgumentException naming the offending value, which the request handler turns
     *                                 into a 400 naming the field
     */
    @JsonCreator
    public static CountryCode fromCode(String code) {
        String candidate = code == null ? "" : code.trim().toUpperCase(Locale.ENGLISH);
        try {
            return valueOf(candidate);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "'" + code + "' is not an ISO 3166-1 alpha-2 country code. Use a two-letter "
                            + "code such as IN.");
        }
    }

    /** The code, which is the constant's own name. Here so callers need not say {@code name()}. */
    public String getCode() {
        return name();
    }
}
