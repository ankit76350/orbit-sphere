package com.orbitastra.backend.dto.academics.attendance.request;

import jakarta.validation.constraints.NotNull;

/**
 * What attendance #3 needs to lock a register.
 *
 * @param version <b>the register's version</b>, as last read. A register changed since — by
 *        another lock, an unlock or a correction to it — is refused, so nobody locks a register
 *        they have not seen.
 */
public record AttendanceSessionLockRequest(@NotNull Long version) {
}
