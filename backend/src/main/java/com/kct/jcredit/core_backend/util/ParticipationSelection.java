package com.kct.jcredit.core_backend.util;

import com.kct.jcredit.core_backend.entity.ParticipatingProject;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A company joins a project once per equipment, so a (company, project) pair can own
 * several participation rows. These helpers collapse those rows into the single answer
 * the company level screens ask for.
 */
public final class ParticipationSelection {

    private static final List<String> STATUS_PRIORITY = List.of("APPROVED", "OPEN", "REJECTED");

    private ParticipationSelection() {
    }

    /**
     * The row that decides what the company may do: APPROVED wins over OPEN, OPEN over
     * REJECTED. Ties keep the oldest row, so the caller must query in id order.
     */
    public static Optional<ParticipatingProject> preferred(List<ParticipatingProject> participations) {
        return participations.stream().min(Comparator.comparingInt(ParticipationSelection::rank));
    }

    /**
     * READY as soon as any of the company's participations carries its membership PDF,
     * null when the company has not joined at all.
     */
    public static String pdfStatus(List<ParticipatingProject> participations) {
        if (participations.isEmpty()) {
            return null;
        }
        return participations.stream().anyMatch(p -> p.getS3KeyPdfMembership() != null)
                ? "READY"
                : "NOT_READY";
    }

    private static int rank(ParticipatingProject participation) {
        int index = STATUS_PRIORITY.indexOf(participation.getStatus());
        return index < 0 ? STATUS_PRIORITY.size() : index;
    }
}
