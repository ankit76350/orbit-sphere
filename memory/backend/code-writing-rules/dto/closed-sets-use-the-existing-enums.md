# Closed Sets Use the Existing Enums, Never a String

**Project-wide convention (set 2026-09-15).** A field whose legal values are a known, finite list
is typed as the enum this project already has. Never as a `String` with a `@Size` on it.

## The enums that already exist

| Field | Enum | Stored as |
|---|---|---|
| country, nationality, address country | [`CountryCode`](../../../../backend/src/main/java/com/orbitastra/backend/models/common/enums/CountryCode.java) | `name()` — `"IN"` |
| language, locale | [`SchoolLocale`](../../../../backend/src/main/java/com/orbitastra/backend/models/common/enums/SchoolLocale.java) | the tag — `"en-IN"` |
| time zone | `SchoolTimeZone` | the IANA id — `"Asia/Kolkata"` |
| gender | `Gender` | `name()` |

`SchoolLocale` and `SchoolTimeZone` need a Mongo read/write converter pair because their constant
name is not their stored value; both already live in
[`EnumCodeConfig`](../../../../backend/src/main/java/com/orbitastra/backend/config/EnumCodeConfig.java).
`CountryCode` and `Gender` need none — the constant name *is* the value.

## Type the model, not just the DTO

`School` stores `CountryCode countryCode` and `SchoolLocale defaultLocale`. Anything else storing
the same thing does too. A `String` on the model with an enum on the DTO puts the constraint in
one place and the data in another, and the database is the one that outlives the code.

## Both boundaries have to agree

Jackson reads an enum from a **body** through its `@JsonCreator`, so `"in"` resolves. Spring's
default converter for a **query parameter** matches the constant name exactly, so
`?nationalityCode=in` was refused while the same value in a body was accepted.

That is fixed once, for everybody, in
[`EnumQueryParamConfig`](../../../../backend/src/main/java/com/orbitastra/backend/config/EnumQueryParamConfig.java) —
a `Converter<String, X>` bean per enum, delegating to the enum's own factory so the refusal
message is identical on both paths. **Add one there when a new enum becomes a query parameter.**

## An empty string is not a value

`"countryCode": ""` is a `400`, not "no country". Omit the key. This differs from the `""`-clears
rule on PATCH endpoints, which applies to free-text fields only.

## Why

Free strings accepted `"12"` as a nationality and `"adads"` as a language, and shipped:
**four stored rows had to be corrected** before the fields could be typed, because a document
holding `"12"` fails to deserialise the moment the field becomes an enum. A closed set that is
only closed in a comment is not closed.

The cost of catching it at the boundary is one line; the cost of catching it later is a
migration plus a `400` nobody can explain.

Related: [[../model/mongodb-document-id-naming]] — the same "type it properly at the edge" rule.
