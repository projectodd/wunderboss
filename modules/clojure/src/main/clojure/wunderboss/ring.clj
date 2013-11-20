(ns wunderboss.ring
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import io.undertow.server.HttpServerExchange
           [io.undertow.util HeaderMap Headers HttpString]
           [java.io File InputStream OutputStream]
           java.nio.channels.FileChannel
           clojure.lang.ISeq))

(defn- headers->map [^HeaderMap headers]
  (reduce
    (fn [accum header-name]
      (assoc accum
        (-> header-name .toString .toLowerCase)
        (->> header-name
          (.get headers)
          (str/join ","))))
    {}
    (.getHeaderNames headers)))

(defn- ring-request [^HttpServerExchange exchange]
  (let [headers (.getRequestHeaders exchange)
        content-type (.getFirst headers Headers/CONTENT_TYPE)]
    ;; TODO: context, path-info ?
    {:server-port (-> exchange .getDestinationAddress .getPort)
     :server-name (.getHostName exchange)
     :remote-addr (-> exchange .getSourceAddress .getAddress .getHostAddress)
     :uri (.getRequestURI exchange)
     :query-string (.getQueryString exchange)
     :scheme (-> exchange .getRequestScheme keyword)
     :request-method (-> exchange .getRequestMethod .toString .toLowerCase keyword)
     :content-type content-type
     :content-length (.getRequestContentLength exchange)
     :character-encoding (if content-type
                           (Headers/extractTokenFromHeader content-type "charset"))
     ;; TODO: :ssl-client-cert
     :headers (headers->map headers)
     :body (.getInputStream exchange)}))

(defn- merge-headers [^HeaderMap to-headers from-headers]
  (doseq [[k v] from-headers]
    (let [k (HttpString. k)]
      (if (coll? v)
        (.addAll to-headers k v)
        (.add to-headers k v)))))

(defprotocol BodyCoercion
  (coerce-body [body exchange]))

(extend-protocol BodyCoercion

  Object
  (coerce-body [body _]
    (throw (IllegalStateException. (str "Can't coerce body of type " (class body)))))

  nil
  (coerce-body [_ _]
    (throw (IllegalStateException. "Can't coerce nil body")))

  String
  (coerce-body [body ^OutputStream os]
    (.write os (.getBytes body)))

  ISeq
  (coerce-body [body ^OutputStream os]
    (doseq [fragment body]
      (coerce-body fragment os)))

  File
  (coerce-body [body ^OutputStream os]
    (io/copy body os))

  InputStream
  (coerce-body [body ^OutputStream os]
    (with-open [body body]
      (io/copy body os))))

(defn- populate-response [^HttpServerExchange exchange {:keys [status headers body]}]
  (when status
    (.setResponseCode exchange status))
  (merge-headers (.getResponseHeaders exchange) headers)
  (coerce-body body (.getOutputStream exchange)))

(defn handle-request [f ^HttpServerExchange exchange]
  (.startBlocking exchange)
  (with-open [output-stream (.getOutputStream exchange)]
    (if-let [response (f (ring-request exchange))]
      (populate-response exchange response)
      (throw (NullPointerException. "Ring handler returned nil")))))
