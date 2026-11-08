package fr.skyframe.lms.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The grading rule of each question type. Plain unit tests: no Spring, no database. */
class QuestionGradingTests {

    @Nested
    class MultipleChoice {
        MultipleChoiceQuestion question() {
            MultipleChoiceQuestion q = new MultipleChoiceQuestion();
            q.setOptions(List.of("PostgreSQL", "MongoDB", "RabbitMQ", "H2"));
            q.setCorrectOptions(Set.of(0, 3));   // the relational databases
            return q;
        }

        @Test
        void exactlyTheCorrectOptionsScoreFull() {
            assertThat(question().score(List.of("3", "0"))).isEqualTo(1.0);
        }

        @Test
        void missingOrExtraOptionsScoreNothing() {
            assertThat(question().score(List.of("0"))).isZero();
            assertThat(question().score(List.of("0", "3", "1"))).isZero();
            assertThat(question().score(List.of())).isZero();
            assertThat(question().score(null)).isZero();
        }

        @Test
        void severalCorrectOptionsMeanSeveralMayBeTicked() {
            assertThat(question().multipleAnswers()).isTrue();
        }

        @Test
        void needsTwoOptionsAndACorrectOne() {
            MultipleChoiceQuestion q = new MultipleChoiceQuestion();
            q.setOptions(List.of("Only one"));
            q.setCorrectOptions(Set.of(0));
            assertThatThrownBy(q::validate).isInstanceOf(IllegalArgumentException.class);

            MultipleChoiceQuestion noneCorrect = question();
            noneCorrect.setCorrectOptions(Set.of());
            assertThatThrownBy(noneCorrect::validate).isInstanceOf(IllegalArgumentException.class);

            MultipleChoiceQuestion outOfRange = question();
            outOfRange.setCorrectOptions(Set.of(7));
            assertThatThrownBy(outOfRange::validate).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Ordering {
        OrderingQuestion question() {
            OrderingQuestion q = new OrderingQuestion();
            q.setItems(List.of("Compile", "Test", "Package", "Deploy"));
            return q;
        }

        @Test
        void rightOrderScoresFull() {
            assertThat(question().score(List.of("Compile", "Test", "Package", "Deploy"))).isEqualTo(1.0);
        }

        @Test
        void partlyRightOrderScoresTheShareInPlace() {
            // Compile and Deploy are in place; Test and Package are swapped
            assertThat(question().score(List.of("Compile", "Package", "Test", "Deploy"))).isEqualTo(0.5);
        }

        @Test
        void itemsMustBeDifferent() {
            OrderingQuestion q = new OrderingQuestion();
            q.setItems(List.of("A", "A"));
            assertThatThrownBy(q::validate).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Matching {
        MatchingQuestion question() {
            MatchingQuestion q = new MatchingQuestion();
            q.setPairs(List.of(new MatchPair("JPA", "Persistence"), new MatchPair("JWT", "Security"),
                    new MatchPair("AMQP", "Messaging")));
            return q;
        }

        @Test
        void allPairsRightScoresFull() {
            assertThat(question().score(List.of("Persistence", "Security", "Messaging"))).isEqualTo(1.0);
        }

        @Test
        void partlyRightScoresTheShareOfCorrectPairs() {
            assertThat(question().score(List.of("Persistence", "Messaging", "Security"))).isCloseTo(1.0 / 3, org.assertj.core.data.Offset.offset(1e-9));
        }

        @Test
        void correctAnswerIsTheRightSideInOrder() {
            assertThat(question().correctAnswer()).containsExactly("Persistence", "Security", "Messaging");
        }
    }

    @Nested
    class FillInBlanks {
        FillInBlanksQuestion question() {
            FillInBlanksQuestion q = new FillInBlanksQuestion();
            q.setText("Spring Boot runs on [[Java|the JVM]] and listens on port [[8080]] by default.");
            return q;
        }

        @Test
        void textIsSplitAroundTheBlanksWithoutRevealingThem() {
            assertThat(question().textParts())
                    .containsExactly("Spring Boot runs on ", " and listens on port ", " by default.");
        }

        @Test
        void alternativesAndUpperOrLowerCaseAreAccepted() {
            assertThat(question().score(List.of("the jvm", " 8080 "))).isEqualTo(1.0);
            assertThat(question().score(List.of("JAVA", "8080"))).isEqualTo(1.0);
        }

        @Test
        void eachBlankCountsSeparately() {
            assertThat(question().score(List.of("Python", "8080"))).isEqualTo(0.5);
        }

        @Test
        void needsAtLeastOneBlank() {
            FillInBlanksQuestion q = new FillInBlanksQuestion();
            q.setText("No blanks here.");
            assertThatThrownBy(q::validate).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void correctAnswerUsesTheFirstAlternative() {
            assertThat(question().correctAnswer()).containsExactly("Java", "8080");
        }
    }
}
