/**
 * Lets a student answer one question. The answer is always a list of strings:
 * MULTIPLE_CHOICE: chosen option indexes · ORDERING: the items in the chosen order
 * MATCHING: one chosen right-hand text per left item · FILL_IN_BLANKS: one text per blank
 */
export default function QuestionInput({ question, answer, onChange }) {
    switch (question.type) {
        case 'MULTIPLE_CHOICE': {
            const toggle = (index) => {
                const value = String(index)
                if (!question.multipleAnswers) return onChange([value])
                onChange(answer.includes(value) ? answer.filter(a => a !== value) : [...answer, value])
            }
            return (
                <div className="choice-list">
                    {question.multipleAnswers && <p className="hint">Several answers can be correct.</p>}
                    {question.options.map((option, i) => (
                        <label key={i} className="choice">
                            <input type={question.multipleAnswers ? 'checkbox' : 'radio'}
                                   name={`q${question.id}`} checked={answer.includes(String(i))}
                                   onChange={() => toggle(i)} />
                            {option}
                        </label>
                    ))}
                </div>
            )
        }
        case 'ORDERING': {
            const move = (from, to) => {
                const next = [...answer]
                const [item] = next.splice(from, 1)
                next.splice(to, 0, item)
                onChange(next)
            }
            return (
                <ol className="ordering-list">
                    {answer.map((item, i) => (
                        <li key={item}>
                            <span>{item}</span>
                            <span className="order-buttons">
                                <button type="button" className="btn-icon-text" disabled={i === 0}
                                        aria-label={`Move ${item} up`} onClick={() => move(i, i - 1)}>▲</button>
                                <button type="button" className="btn-icon-text" disabled={i === answer.length - 1}
                                        aria-label={`Move ${item} down`} onClick={() => move(i, i + 1)}>▼</button>
                            </span>
                        </li>
                    ))}
                </ol>
            )
        }
        case 'MATCHING':
            return (
                <div className="matching-list">
                    {question.leftItems.map((left, i) => (
                        <label key={left} className="matching-row">
                            <span className="matching-left">{left}</span>
                            <span className="matching-arrow">→</span>
                            <select className="small-select" value={answer[i]}
                                    onChange={e => onChange(answer.map((a, j) => (j === i ? e.target.value : a)))}>
                                <option value="">Choose…</option>
                                {question.rightOptions.map(right => <option key={right} value={right}>{right}</option>)}
                            </select>
                        </label>
                    ))}
                </div>
            )
        case 'FILL_IN_BLANKS':
            return (
                <p className="blanks-text">
                    {question.textParts.map((part, i) => (
                        <span key={i}>
                            {part}
                            {i < question.blankCount && (
                                <input className="blank-input" value={answer[i]} aria-label={`Blank ${i + 1}`}
                                       onChange={e => onChange(answer.map((a, j) => (j === i ? e.target.value : a)))} />
                            )}
                        </span>
                    ))}
                </p>
            )
        default:
            return null
    }
}
