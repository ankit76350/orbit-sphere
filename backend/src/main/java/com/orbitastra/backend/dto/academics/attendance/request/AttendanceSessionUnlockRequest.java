package com.orbitastra.backend.dto.academics.attendance.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What attendance #4 needs to open a locked register again.
 *
 * @param reason  <b>required</b> — why a final register is being re-opened. Saved on the register,
 *                in its unlock history. A blank reason is refused.
 * @param version the register's version, as last read.
 */
public record AttendanceSessionUnlockRequest(
        @Size(max = 500) String reason,
        @NotNull Long version) {
}
