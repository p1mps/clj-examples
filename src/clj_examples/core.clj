(ns clj-examples.core
  (:require [clojure.core.async :refer :all])
  (:gen-class))


;; Data is transmitted on queue-like channels. By default channels
;; are unbuffered (0-length) - they require producer and consumer to
;; rendezvous for the transfer of a value through the channel.

;; Use `chan` to make an unbuffered channel:
(chan)

;; Pass a number to create a channel with a fixed buffer:
(chan 10)

;; `close!` a channel to stop accepting puts. Remaining values are still
;; available to take. Drained channels return nil on take. Nils may
;; not be sent over a channel explicitly!

(let [c (chan)]
  (close! c))

;;;; ORDINARY THREADS

;; In ordinary threads, we use `>!!` (blocking put) and `<!!`
;; (blocking take) to communicate via channels.

(let [c (chan 10)]
  (>!! c "hello")
  (assert (= "hello" (<!! c)))
  (close! c))

;; Because these are blocking calls, if we try to put on an
;; unbuffered channel, we will block the main thread. We can use
;; `thread` (like `future`) to execute a body in a pool thread and
;; return a channel with the result. Here we launch a background task
;; to put "hello" on a channel, then read that value in the current thread.

(let [c (chan)]
  (thread (>!! c "hello"))
  (assert (= "hello" (<!! c)))
  (close! c))

;;;; GO BLOCKS AND IOC THREADS

;; The `go` macro asynchronously executes its body in a special pool
;; of threads. Channel operations that would block will pause
;; execution instead, blocking no threads. This mechanism encapsulates
;; the inversion of control that is external in event/callback
;; systems. Inside `go` blocks, we use `>!` (put) and `<!` (take).

;; Here we convert our prior channel example to use go blocks:
(let [c (chan)]
  (go (>! c "hello"))
  (assert (= "hello" (<!! (go (<! c)))))
  (close! c))

;; Instead of the explicit thread and blocking call, we use a go block
;; for the producer. The consumer uses a go block to take, then
;; returns a result channel, from which we do a blocking take.
