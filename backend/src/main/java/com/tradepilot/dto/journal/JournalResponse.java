package com.tradepilot.dto.journal;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record JournalResponse(Long id, Long accountId, LocalDate journalDate, String mood, String setupQuality, String mistakes, String lessonLearned, String notes, LocalDateTime createdAt, LocalDateTime updatedAt) {

        @java.lang.SuppressWarnings("all")
        public static class JournalResponseBuilder {
                @java.lang.SuppressWarnings("all")
                private Long id;
                @java.lang.SuppressWarnings("all")
                private Long accountId;
                @java.lang.SuppressWarnings("all")
                private LocalDate journalDate;
                @java.lang.SuppressWarnings("all")
                private String mood;
                @java.lang.SuppressWarnings("all")
                private String setupQuality;
                @java.lang.SuppressWarnings("all")
                private String mistakes;
                @java.lang.SuppressWarnings("all")
                private String lessonLearned;
                @java.lang.SuppressWarnings("all")
                private String notes;
                @java.lang.SuppressWarnings("all")
                private LocalDateTime createdAt;
                @java.lang.SuppressWarnings("all")
                private LocalDateTime updatedAt;

                @java.lang.SuppressWarnings("all")
                JournalResponseBuilder() {
                }

                /**
                 * @return {@code this}.
                 */
                @java.lang.SuppressWarnings("all")
                public JournalResponse.JournalResponseBuilder id(final Long id) {
                        this.id = id;
                        return this;
                }

                /**
                 * @return {@code this}.
                 */
                @java.lang.SuppressWarnings("all")
                public JournalResponse.JournalResponseBuilder accountId(final Long accountId) {
                        this.accountId = accountId;
                        return this;
                }

                /**
                 * @return {@code this}.
                 */
                @java.lang.SuppressWarnings("all")
                public JournalResponse.JournalResponseBuilder journalDate(final LocalDate journalDate) {
                        this.journalDate = journalDate;
                        return this;
                }

                /**
                 * @return {@code this}.
                 */
                @java.lang.SuppressWarnings("all")
                public JournalResponse.JournalResponseBuilder mood(final String mood) {
                        this.mood = mood;
                        return this;
                }

                /**
                 * @return {@code this}.
                 */
                @java.lang.SuppressWarnings("all")
                public JournalResponse.JournalResponseBuilder setupQuality(final String setupQuality) {
                        this.setupQuality = setupQuality;
                        return this;
                }

                /**
                 * @return {@code this}.
                 */
                @java.lang.SuppressWarnings("all")
                public JournalResponse.JournalResponseBuilder mistakes(final String mistakes) {
                        this.mistakes = mistakes;
                        return this;
                }

                /**
                 * @return {@code this}.
                 */
                @java.lang.SuppressWarnings("all")
                public JournalResponse.JournalResponseBuilder lessonLearned(final String lessonLearned) {
                        this.lessonLearned = lessonLearned;
                        return this;
                }

                /**
                 * @return {@code this}.
                 */
                @java.lang.SuppressWarnings("all")
                public JournalResponse.JournalResponseBuilder notes(final String notes) {
                        this.notes = notes;
                        return this;
                }

                /**
                 * @return {@code this}.
                 */
                @java.lang.SuppressWarnings("all")
                public JournalResponse.JournalResponseBuilder createdAt(final LocalDateTime createdAt) {
                        this.createdAt = createdAt;
                        return this;
                }

                /**
                 * @return {@code this}.
                 */
                @java.lang.SuppressWarnings("all")
                public JournalResponse.JournalResponseBuilder updatedAt(final LocalDateTime updatedAt) {
                        this.updatedAt = updatedAt;
                        return this;
                }

                @java.lang.SuppressWarnings("all")
                public JournalResponse build() {
                        return new JournalResponse(this.id, this.accountId, this.journalDate, this.mood, this.setupQuality, this.mistakes, this.lessonLearned, this.notes, this.createdAt, this.updatedAt);
                }

                @java.lang.Override
                @java.lang.SuppressWarnings("all")
                public java.lang.String toString() {
                        return "JournalResponse.JournalResponseBuilder(id=" + this.id + ", accountId=" + this.accountId + ", journalDate=" + this.journalDate + ", mood=" + this.mood + ", setupQuality=" + this.setupQuality + ", mistakes=" + this.mistakes + ", lessonLearned=" + this.lessonLearned + ", notes=" + this.notes + ", createdAt=" + this.createdAt + ", updatedAt=" + this.updatedAt + ")";
                }
        }

        @java.lang.SuppressWarnings("all")
        public static JournalResponse.JournalResponseBuilder builder() {
                return new JournalResponse.JournalResponseBuilder();
        }
}
