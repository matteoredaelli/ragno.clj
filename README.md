# ragno

A crawler for extracting info from web domains

## Usage

Copy and edit the configuration file

```bash
cp ragno-sample.edn ragno.edn
```

Invoke a library API function from the command-line:

    $clojure -X net.clojars.matteoredaelli.ragno/cli :urlfile \urls.csv\"  :config-file \"ragno.edn\"

Using Redis or Apache kvrocks or Snap KeyDB

    $clojure -X net.clojars.matteoredaelli.redis/cli :config-file \"ragno.edn\"

    $redis-cli publish ragno "https://www.redaelli.org/"
    

Run the project's tests (they'll fail until you edit them):

    $ clojure -T:build test

Run the project's CI pipeline and build a JAR (this will fail until you edit the tests to pass):

    $ clojure -T:build ci

This will produce an updated `pom.xml` file with synchronized dependencies inside the `META-INF`
directory inside `target/classes` and the JAR in `target`. You can update the version (and SCM tag)
information in generated `pom.xml` by updating `build.clj`.

Install it locally (requires the `ci` task be run first):

    $ clojure -T:build install

Deploy it to Clojars -- needs `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment
variables (requires the `ci` task be run first):

    $ clojure -T:build deploy

Your library will be deployed to net.clojars.matteoredaelli/ragno on clojars.org by default.

##

cat out.jl |jq -r '[.url, .status, (.title? // [""] | join(". ")), (.description? // [""] | join(". ")), (."social-tags" // [""] | join(","))] |@tsv'

A
A

## ROADMAP

- Evaluating parallel batch processing

  [ ] with https://github.com/nilenso/goose
  [ ] with slurm
  
  
## License

Copyright © 2023 Matteo Redaelli

Distributed under the Eclipse Public License version 2.0.
