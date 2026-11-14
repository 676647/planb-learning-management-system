// The four question types, with labels and the API path used to create them.
export const QUESTION_TYPES = {
  MULTIPLE_CHOICE: { label: 'Multiple choice', kind: 'multiple-choice' },
  ORDERING: { label: 'Ordering', kind: 'ordering' },
  MATCHING: { label: 'Matching', kind: 'matching' },
  FILL_IN_BLANKS: { label: 'Fill in the blanks', kind: 'fill-in-blanks' },
}

/** An empty answer for a question as students see it. */
export function emptyAnswer(question) {
  switch (question.type) {
    case 'ORDERING': return [...question.items]            // starts in the (shuffled) order shown
    case 'MATCHING': return question.leftItems.map(() => '')
    case 'FILL_IN_BLANKS': return Array(question.blankCount).fill('')
    default: return []
  }
}
