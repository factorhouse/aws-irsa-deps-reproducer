(defproject aws-irsa-deps-reproducer "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [software.amazon.awssdk/licensemanager "2.18.20"]
                 [software.amazon.msk/aws-msk-iam-auth "1.1.5"]
                 [software.amazon.glue/schema-registry-serde "1.1.14"]]
  :repl-options {:init-ns irsa-error-repro}
  :main irsa-error-repro)
