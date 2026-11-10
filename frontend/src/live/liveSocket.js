// The WebSocket connection for live quizzes, shared by every part of the page that listens.
//
// It speaks STOMP (a small messaging protocol on top of WebSockets): we SUBSCRIBE to
// destinations and the server pushes JSON messages to them. The connection is opened when
// the first subscription starts and closed when the last one ends. After a lost connection
// the client reconnects by itself, subscribes again and tells listeners, so they can fetch
// what they missed. If the WebSocket can't connect at all, the live screens fall back to
// asking the REST API every few seconds (see useLiveGame.js), and the console says why.
import { Client } from '@stomp/stompjs'
import { getToken } from '../api.js'

let client = null
let nextId = 1
const subscriptions = new Map() // id -> { destination, onMessage, handle }
const connectListeners = new Set()
const stateListeners = new Set()
let connected = false

function setConnected(value) {
  if (connected === value) return
  connected = value
  stateListeners.forEach(listener => listener(value))
}

function socketUrl() {
  // Same host as the page: nginx (Docker) and the Vite dev server both forward /api/ws
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${protocol}://${window.location.host}/api/ws`
}

function subscribeNow(entry) {
  entry.handle = client.subscribe(entry.destination, (frame) => {
    try {
      entry.onMessage(JSON.parse(frame.body))
    } catch (err) {
      console.warn('Bad live message', err)
    }
  })
}

function ensureClient() {
  if (client) return
  client = new Client({
    brokerURL: socketUrl(),
    // A fresh token on every (re)connect: the server checks it on CONNECT
    beforeConnect: (c) => { c.connectHeaders = { Authorization: `Bearer ${getToken()}` } },
    reconnectDelay: 2000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  })
  client.onConnect = () => {
    console.info('Live connection open')
    setConnected(true)
    subscriptions.forEach(subscribeNow)
    connectListeners.forEach(listener => listener())
  }
  client.onStompError = (frame) => console.warn('Live connection refused by the server:', frame.headers.message)
  client.onWebSocketClose = (event) => {
    setConnected(false)
    console.warn(`Live connection closed (code ${event.code}${event.reason ? `: ${event.reason}` : ''}); retrying`)
  }
  client.activate()
}

/** Listens to a destination; returns a function that stops listening. */
export function subscribe(destination, onMessage) {
  const id = nextId++
  const entry = { destination, onMessage, handle: null }
  subscriptions.set(id, entry)
  ensureClient()
  if (client.connected) subscribeNow(entry)
  return () => {
    subscriptions.delete(id)
    if (entry.handle && client?.connected) entry.handle.unsubscribe()
    if (subscriptions.size === 0 && client) {
      client.deactivate()
      client = null
      setConnected(false)
    }
  }
}

/** Whether the WebSocket is connected right now. */
export function isConnected() {
  return connected
}

/** Called with true/false when the connection opens or closes; returns a function that stops it. */
export function onConnectionChange(listener) {
  stateListeners.add(listener)
  return () => stateListeners.delete(listener)
}

/** Called after every (re)connect; returns a function that stops it. */
export function onConnected(listener) {
  connectListeners.add(listener)
  return () => connectListeners.delete(listener)
}
