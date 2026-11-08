/** Shows an answer (the student's, or the correct one) in a readable way for the question's type. */
export default function AnswerView({ question, answer }) {
    if (!answer || answer.length === 0 || answer.every(a => a === '')) {
        return <span className="hint">(no answer)</span>
    }
    switch (question.type) {
        case 'MULTIPLE_CHOICE':
            return <span>{answer.map(i => question.options[Number(i)] ?? `#${i}`).join(', ')}</span>
        case 'ORDERING':
            return <span>{answer.join(' → ')}</span>
        case 'MATCHING':
            return (
                <span>{question.leftItems.map((left, i) => `${left} → ${answer[i] || '?'}`).join(' · ')}</span>
            )
        case 'FILL_IN_BLANKS':
            return (
                <span>
                    {question.textParts.map((part, i) => (
                        <span key={i}>{part}{i < question.blankCount && <strong>[{answer[i] || '?'}]</strong>}</span>
                    ))}
                </span>
            )
        default:
            return <span>{answer.join(', ')}</span>
    }
}
