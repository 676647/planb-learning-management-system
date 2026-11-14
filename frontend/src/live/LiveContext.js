import { createContext, useContext } from 'react'

/** openLive(gameId): switches the app to the live quiz screen. Provided by Home. */
export const LiveContext = createContext({ openLive: () => {} })

export function useOpenLive() {
    return useContext(LiveContext).openLive
}
