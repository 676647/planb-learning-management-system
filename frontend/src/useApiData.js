import { useCallback, useEffect, useState } from 'react'

/**
 * Loads data from the API when a component appears (and when `fetcher` changes).
 *
 *   const fetchCourses = useCallback(() => api.myEnrollments(), [])
 *   const { data, error, loading, reload } = useApiData(fetchCourses)
 *
 * Call reload() after changing something to fetch fresh data. If the component goes away
 * (or a newer request starts) before an answer arrives, the old answer is ignored.
 */
export function useApiData(fetcher) {
    const [state, setState] = useState({ data: null, error: '', loading: true })
    const [version, setVersion] = useState(0)

    useEffect(() => {
        let stale = false
        fetcher()
            .then(data => { if (!stale) setState({ data, error: '', loading: false }) })
            .catch(err => { if (!stale) setState(prev => ({ ...prev, error: err.message, loading: false })) })
        return () => { stale = true }
    }, [fetcher, version])

    const reload = useCallback(() => setVersion(v => v + 1), [])

    return { ...state, reload }
}
